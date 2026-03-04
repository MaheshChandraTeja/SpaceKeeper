package com.spacekeeperfx.persistence.dao;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.persistence.Database;
import com.spacekeeperfx.util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SubspaceDao {

    private final Database db;

    public SubspaceDao(Database db) {
        this.db = db;
    }

    /* ------------------- Queries ------------------- */


    public Subspace findById(String subspaceId) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, vault_id, name FROM subspace WHERE id=?")) {
                ps.setString(1, subspaceId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    String id = rs.getString("id");
                    String vId = rs.getString("vault_id");
                    String name = rs.getString("name");
                    List<ColumnDef> cols = loadColumns(c, id);
                    return new Subspace(id, vId, name, cols);
                }
            }
        });
    }

    /* ------------------- Mutations ------------------- */

    public Subspace insert(String vaultId, String name, boolean enablePhotograph) {
        final String subspaceId = IdGenerator.newIdWithPrefix("sub_");
        final long now = java.time.Instant.now().getEpochSecond();
        final java.util.List<ColumnDef> cols = new java.util.ArrayList<>(3);

        db.inTransaction(conn -> {
            // INSERT into subspace WITH created_at/updated_at
            try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO subspace (id, vault_id, name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """)) {
                ps.setString(1, subspaceId);
                ps.setString(2, vaultId);
                ps.setString(3, name);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.executeUpdate();
            }

            // Baseline columns -> NO timestamps here
            ColumnDef nameCol = ColumnDef.standardNameId();
            nameCol.setSubspaceId(subspaceId);
            insertColumn(conn, nameCol);
            cols.add(nameCol);

            ColumnDef photoCol = ColumnDef.standardPhotograph(enablePhotograph);
            photoCol.setSubspaceId(subspaceId);
            insertColumn(conn, photoCol);
            cols.add(photoCol);

            ColumnDef descCol = ColumnDef.standardDescription();
            descCol.setSubspaceId(subspaceId);
            insertColumn(conn, descCol);
            cols.add(descCol);

            return null;
        });

        Subspace s = new Subspace(subspaceId, vaultId, name, cols);
        s.sortByDisplayOrder();
        return s;
    }

    // IMPORTANT: match your actual column_def schema:
// id, subspace_id, name, type, enabled, required, is_unique, display_order
    private void insertColumn(Connection conn, ColumnDef def) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO column_def
          (id, subspace_id, name, type, enabled, required, is_unique, display_order)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """)) {
            ps.setString(1, def.getId());
            ps.setString(2, def.getSubspaceId());
            ps.setString(3, def.getName());
            ps.setString(4, def.getType().name());
            ps.setInt(5, def.isEnabled() ? 1 : 0);
            ps.setInt(6, def.isRequired() ? 1 : 0);
            ps.setInt(7, def.isUnique() ? 1 : 0);
            ps.setInt(8, def.getDisplayOrder());
            ps.executeUpdate();
        }
    }

    public boolean rename(String subspaceId, String newName) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE subspace SET name=? WHERE id=?")) {
                ps.setString(1, newName);
                ps.setString(2, subspaceId);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public boolean delete(String subspaceId) {
        return db.inTransaction(c -> {
            // If FKs have ON DELETE CASCADE, these manual deletes can be removed.
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM record_value WHERE record_id IN (SELECT id FROM record WHERE subspace_id=?)")) {
                ps.setString(1, subspaceId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM record WHERE subspace_id=?")) {
                ps.setString(1, subspaceId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM column_def WHERE subspace_id=?")) {
                ps.setString(1, subspaceId);
                ps.executeUpdate();
            }

            int count;
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM subspace WHERE id=?")) {
                ps.setString(1, subspaceId);
                count = ps.executeUpdate();
            }
            return count > 0;
        });
    }

    /* ------------------- Helpers ------------------- */

    private List<ColumnDef> loadColumns(Connection c, String subspaceId) throws SQLException {
        List<ColumnDef> cols = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT id, subspace_id, name, type, enabled, required, is_unique, display_order
                FROM column_def
                WHERE subspace_id=?
                ORDER BY display_order, name
                """)) {
            ps.setString(1, subspaceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cols.add(new ColumnDef(
                            rs.getString("id"),
                            rs.getString("subspace_id"),
                            rs.getString("name"),
                            ColumnType.fromString(rs.getString("type")),
                            rs.getInt("enabled") == 1,
                            rs.getInt("required") == 1,
                            rs.getInt("is_unique") == 1,
                            rs.getInt("display_order")
                    ));
                }
            }
        }
        return cols;
    }

    public void replaceColumns(String subspaceId, List<ColumnDef> defs) {
        db.inTransaction(c -> {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM column_def WHERE subspace_id=?")) {
                del.setString(1, subspaceId);
                del.executeUpdate();
            }
            // (Re)insert in desired order
            for (ColumnDef d : defs) {
                ColumnDef toSave = new ColumnDef(
                        d.getId(),                         // already unique (IdGenerator)
                        subspaceId,                        // ensure bound to this subspace
                        d.getName(),
                        d.getType(),
                        d.isEnabled(),
                        d.isRequired(),
                        d.isUnique(),
                        d.getDisplayOrder()
                );
                insertColumn(c, toSave);
            }
            return null;
        });
    }

    public Subspace insert(Subspace s) {
        db.inTransaction(conn -> {
            // Insert subspace WITH timestamps (use SQLite CURRENT_TIMESTAMP)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO subspace (id, vault_id, name, created_at, updated_at) " +
                            "VALUES (?,?,?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                ps.setString(1, s.getId());
                ps.setString(2, s.getVaultId());
                ps.setString(3, s.getName());
                ps.executeUpdate();
            }

            // Insert baseline columns (also with timestamps)
            for (var def : s.getColumns()) {
                insertColumn(conn, def);
            }
            return null; // db.inTransaction requires a return (Void)
        });
        return s;
    }

    public java.util.List<Subspace> listByVault(String vaultId) {
        return db.inTransaction(conn -> {
            var list = new java.util.ArrayList<Subspace>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, vault_id, name FROM subspace WHERE vault_id=? ORDER BY name")) {
                ps.setString(1, vaultId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        var s = com.spacekeeperfx.persistence.mapper.RowMappers.mapSubspace(rs);
                        // load columns:
                        s.setColumns(listColumns(conn, s.getId()));
                        list.add(s);
                    }
                }
            }
            return list;
        });
    }

    private java.util.List<com.spacekeeperfx.model.ColumnDef> listColumns(Connection conn, String subspaceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, subspace_id, name, type, enabled, required, is_unique, display_order " +
                        "FROM column_def WHERE subspace_id=? ORDER BY display_order, name")) {
            ps.setString(1, subspaceId);
            var out = new java.util.ArrayList<com.spacekeeperfx.model.ColumnDef>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var def = com.spacekeeperfx.persistence.mapper.RowMappers.mapColumnDef(rs);
                    // ensure subspace id is set (RowMappers.mapColumnDef reads it if selected)
                    if (def.getSubspaceId() == null || def.getSubspaceId().isBlank()) {
                        def.setSubspaceId(subspaceId);
                    }
                    out.add(def);
                }
            }
            return out;
        }
    }
}
