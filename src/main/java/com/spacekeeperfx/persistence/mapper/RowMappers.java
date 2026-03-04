package com.spacekeeperfx.persistence.mapper;


import com.spacekeeperfx.model.Vault;
import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Record;

import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helpers to map JDBC rows to model objects.
 * Note: createdAt/updatedAt in models are not injected from DB (they default to 'now').
 * If you need exact timestamps in the model, we can extend the models later.
 */
public final class RowMappers {

    private RowMappers() {}

    public static Vault mapVault(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        return new Vault(id, name);
    }

    public static Subspace mapSubspace(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String vaultId = rs.getString("vault_id");
        String name = rs.getString("name");
        // columns are attached separately by DAO
        return new Subspace(id, vaultId, name, java.util.List.of());
    }

    public static ColumnDef mapColumnDef(ResultSet rs) throws SQLException {
        String id         = rs.getString("id");
        String subspaceId = rs.getString("subspace_id");
        String name       = rs.getString("name");
        ColumnType type   = ColumnType.fromString(rs.getString("type"));
        boolean enabled   = rs.getInt("enabled") == 1;
        boolean required  = rs.getInt("required") == 1;
        boolean unique    = rs.getInt("is_unique") == 1;
        int order         = rs.getInt("display_order");

        // Use the full factory (or new ColumnDef(...)) so IDs/names are preserved from DB
        return ColumnDef.of(id, subspaceId, name, type, enabled, required, unique, order);
        // alternatively:
        // return new ColumnDef(id, subspaceId, name, type, enabled, required, unique, order);
    }


    /** Create a Record with ID/Subspace but without values; values are loaded separately. */
    public static Record mapRecordSkeleton(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String subspaceId = rs.getString("subspace_id");
        return new Record(id, subspaceId, new LinkedHashMap<>());
    }

    /** Load all EAV values for the given record from DB and attach to the model. */
    public static Record loadRecordValues(Connection c, Record r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
        SELECT column_id, text_value, number_value
        FROM record_value
        WHERE record_id=?
    """)) {
            ps.setString(1, r.getId());
            Map<String, Object> vals = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String col = rs.getString("column_id");
                    String t   = rs.getString("text_value");

                    // Be defensive: number_value may be NULL or even stored as text.
                    Object raw = rs.getObject("number_value");  // no target class
                    BigDecimal num = null;
                    if (raw instanceof Number n) {
                        num = BigDecimal.valueOf(n.doubleValue());
                    } else if (raw instanceof String s) {
                        try {
                            String trimmed = s.trim();
                            if (!trimmed.isEmpty()) {
                                num = new BigDecimal(trimmed);
                            }
                        } catch (NumberFormatException ignore) {
                            // not a valid number; treat as null and prefer text_value
                        }
                    }

                    if (t != null) {
                        vals.put(col, t);
                    } else if (num != null) {
                        vals.put(col, num);
                    } else {
                        vals.put(col, null);
                    }
                }
            }
            // attach to record
            for (var e : vals.entrySet()) {
                r.put(e.getKey(), e.getValue());
            }
            return r;
        }
    }
}
