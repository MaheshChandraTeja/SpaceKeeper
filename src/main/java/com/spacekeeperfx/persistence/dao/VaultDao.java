package com.spacekeeperfx.persistence.dao;

import com.spacekeeperfx.model.Vault;
import com.spacekeeperfx.model.IdGenerator;
import com.spacekeeperfx.persistence.Database;
import com.spacekeeperfx.persistence.mapper.RowMappers;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VaultDao {

    private final Database db;

    public VaultDao(Database db) {
        this.db = db;
    }

    public Vault insert(String name) {
        String id = IdGenerator.newIdWithPrefix("vault_");
        long now = System.currentTimeMillis();

        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO vault(id,name,created_at,updated_at) VALUES(?,?,?,?)")) {
                ps.setString(1, id);
                ps.setString(2, name.trim());
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            }
            return null;
        });
        return new Vault(id, name);
    }

    public boolean rename(String vaultId, String newName) {
        int updated = db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE vault SET name=?, updated_at=? WHERE id=?")) {
                ps.setString(1, newName.trim());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, vaultId);
                return ps.executeUpdate();
            }
        });
        return updated > 0;
    }

    public boolean delete(String vaultId) {
        int updated = db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM vault WHERE id=?")) {
                ps.setString(1, vaultId);
                return ps.executeUpdate();
            }
        });
        return updated > 0;
    }

    public Optional<Vault> findById(String vaultId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM vault WHERE id=?")) {
            ps.setString(1, vaultId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(RowMappers.mapVault(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Vault> listAll() {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM vault ORDER BY created_at ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Vault> list = new ArrayList<>();
                while (rs.next()) list.add(RowMappers.mapVault(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
