package com.spacekeeperfx.persistence.dao;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Record;
import com.spacekeeperfx.model.IdGenerator;
import com.spacekeeperfx.persistence.Database;
import com.spacekeeperfx.persistence.mapper.RowMappers;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class RecordDao {

    private final Database db;

    public RecordDao(Database db) {
        this.db = db;
    }

    /** Create a blank record in a subspace. */
    public Record insertBlank(String subspaceId) {
        String id = IdGenerator.newIdWithPrefix("rec_");
        long now = System.currentTimeMillis();
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO record(id,subspace_id,created_at,updated_at) VALUES(?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, subspaceId);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            return null;
        });
        return new Record(id, subspaceId, new LinkedHashMap<>());
    }

    /** Upsert a single cell value respecting column type. */
    public void upsertValue(String recordId, String columnId, ColumnType type, Object value) {
        db.inTransaction(c -> {
            // normalize to columns
            String sql = """
                INSERT INTO record_value(record_id,column_id,text_value,number_value)
                VALUES(?,?,?,?)
                ON CONFLICT(record_id,column_id) DO UPDATE SET
                  text_value=excluded.text_value,
                  number_value=excluded.number_value
                """;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, recordId);
                ps.setString(2, columnId);
                switch (type) {
                    case TEXT, PHOTO -> {
                        ps.setString(3, value == null ? null : String.valueOf(value));
                        ps.setNull(4, Types.DOUBLE);
                    }
                    case NUMBER -> {
                        if (value == null) {
                            ps.setNull(3, Types.VARCHAR);
                            ps.setNull(4, Types.DOUBLE);
                        } else if (value instanceof BigDecimal bd) {
                            ps.setNull(3, Types.VARCHAR);
                            ps.setDouble(4, bd.doubleValue());
                        } else if (value instanceof Number n) {
                            ps.setNull(3, Types.VARCHAR);
                            ps.setDouble(4, n.doubleValue());
                        } else {
                            // best effort parse
                            try {
                                double d = Double.parseDouble(String.valueOf(value));
                                ps.setNull(3, Types.VARCHAR);
                                ps.setDouble(4, d);
                            } catch (NumberFormatException ex) {
                                throw new IllegalArgumentException("Invalid numeric value for column: " + columnId);
                            }
                        }
                    }
                }
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE record SET updated_at=? WHERE id=?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, recordId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public boolean deleteByIds(String subspaceId, Collection<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) return true;
        int updated = db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM record WHERE id=? AND subspace_id=?")) {
                int count = 0;
                for (String id : recordIds) {
                    ps.setString(1, id);
                    ps.setString(2, subspaceId);
                    count += ps.executeUpdate();
                }
                return count;
            }
        });
        return updated > 0;
    }

    public List<Record> listBySubspace(String subspaceId) {
        try (Connection c = db.getConnection()) {
            List<Record> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM record WHERE subspace_id=? ORDER BY created_at DESC")) {
                ps.setString(1, subspaceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) rows.add(RowMappers.mapRecordSkeleton(rs));
                }
            }
            // Load values for each record (N+1; fine for local; optimize later)
            for (int i = 0; i < rows.size(); i++) {
                rows.set(i, RowMappers.loadRecordValues(c, rows.get(i)));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Record> findById(String recordId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM record WHERE id=?")) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Record r = RowMappers.mapRecordSkeleton(rs);
                return Optional.of(RowMappers.loadRecordValues(c, r));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertBlank(Connection c, String id, String subspaceId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO record (id, subspace_id) VALUES (?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, subspaceId);
            ps.executeUpdate();
        }
    }

    /** Set a TEXT value for (recordId, columnId). If value is null, the cell is cleared. */
    public void setText(String recordId, String columnId, String value) {
        db.inTransaction(c -> {
            if (value == null) {
                deleteValue(c, recordId, columnId);
            } else {
                upsertValue(c, recordId, columnId, value, null);
            }
            return null;
        });
    }

    /** Set a NUMBER value for (recordId, columnId). If value is null, the cell is cleared. */
    public void setNumber(String recordId, String columnId, BigDecimal value) {
        db.inTransaction(c -> {
            if (value == null) {
                deleteValue(c, recordId, columnId);
            } else {
                // store as DOUBLE; text column is cleared to avoid JDBC type issues
                upsertValue(c, recordId, columnId, null, value.doubleValue());
            }
            return null;
        });
    }

    /** Set a PHOTO path (stored as text) for (recordId, columnId). If path is null, the cell is cleared. */
    public void setPhotoPath(String recordId, String columnId, String path) {
        db.inTransaction(c -> {
            if (path == null || path.isBlank()) {
                deleteValue(c, recordId, columnId);
            } else {
                upsertValue(c, recordId, columnId, path, null);
            }
            return null;
        });
    }

    /* ------------------- helpers ------------------- */

    /** Delete any value for (recordId, columnId). */
    private void deleteValue(Connection c, String recordId, String columnId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM record_value WHERE record_id=? AND column_id=?")) {
            ps.setString(1, recordId);
            ps.setString(2, columnId);
            ps.executeUpdate();
        }
    }

    public Record createBlank(String subspaceId) {
        final String id = IdGenerator.newIdWithPrefix("rec_");
        final long now = java.time.Instant.now().getEpochSecond();

        db.inTransaction(conn -> {
            insertBlank(conn, id, subspaceId, now);
            return null;
        });

        // return an empty in-memory record
        return new Record(id, subspaceId, new java.util.LinkedHashMap<>());
    }

    private void insertBlank(java.sql.Connection conn, String id, String subspaceId, long now) throws java.sql.SQLException {
        try (var ps = conn.prepareStatement("""
        INSERT INTO record (id, subspace_id, created_at, updated_at)
        VALUES (?, ?, ?, ?)
    """)) {
            ps.setString(1, id);
            ps.setString(2, subspaceId);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    /**
     * Insert-or-update a value. Exactly one of (textValue, numberValue) should be non-null.
     * Clears the other column so the JDBC mapper can read cleanly.
     *
     * Requires a UNIQUE constraint on (record_id, column_id) in record_value.
     * If you don't have it, either add:
     *   CREATE UNIQUE INDEX IF NOT EXISTS ux_record_value ON record_value(record_id, column_id);
     * or replace the UPSERT with delete+insert.
     */
    private void upsertValue(Connection c, String recordId, String columnId,
                             String textValue, Double numberValue) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
            INSERT INTO record_value (record_id, column_id, text_value, number_value)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(record_id, column_id) DO UPDATE SET
                text_value   = excluded.text_value,
                number_value = excluded.number_value
            """)) {
            ps.setString(1, recordId);
            ps.setString(2, columnId);
            if (textValue != null) {
                ps.setString(3, textValue);
                ps.setObject(4, null);
            } else {
                ps.setObject(3, null);
                ps.setObject(4, numberValue); // Double will map cleanly for JDBC
            }
            ps.executeUpdate();
        }
    }
}
