package com.spacekeeperfx.persistence.dao;

import com.spacekeeperfx.persistence.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class MetaDao {
    private final Database db;
    public MetaDao(Database db) { this.db = db; }

    public Optional<String> get(String key) {
        return db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT value FROM meta WHERE key=?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
                }
            }
        });
    }

    public void set(String key, String value) {
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO meta(key,value) VALUES(?,?) " +
                            "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public boolean hasPassword() {
        return get("password_hash").filter(s -> !s.isBlank()).isPresent();
    }
}
