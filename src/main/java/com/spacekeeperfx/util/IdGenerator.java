package com.spacekeeperfx.util;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Simple unique ID generator for rows/columns/subspaces/etc.
 * Format: <prefix>_<hexTimestamp>_<uuidHex>
 *
 * Examples:
 *   IdGenerator.newId("col")  -> col_18f2a0f6c5_7b2d4c8e1f0a4a0b8b3b1a2c3d4e5f6
 *   IdGenerator.newId()       -> id_18f2a0f6c5_...
 *
 * - Uses epoch millis (sortable-ish) + UUID v4 for uniqueness.
 * - Output is ASCII [a-zA-Z0-9_], safe for SQLite TEXT PKs and file names.
 */
public final class IdGenerator {

    private IdGenerator() {}

    /** Generate an ID with a custom prefix (letters/digits only; others are stripped). */
    public static String newId(String prefix) {
        String p = sanitizePrefix(prefix);
        String tsHex = Long.toHexString(Instant.now().toEpochMilli());
        String uuidHex = UUID.randomUUID().toString().replace("-", "");
        return p + "_" + tsHex + "_" + uuidHex;
    }

    /** Generate an ID with the default "id" prefix. */
    public static String newId() {
        return newId("id");
    }

    private static String sanitizePrefix(String prefix) {
        String p = Objects.toString(prefix, "id").trim();
        p = p.replaceAll("[^A-Za-z0-9]", "");
        return p.isEmpty() ? "id" : p;
    }

    public static String newIdWithPrefix(String rawPrefix) {
        String p = Objects.toString(rawPrefix, "id").trim();
        // keep underscores if present, but strip other non-alphanumerics
        p = p.replaceAll("[^A-Za-z0-9_]", "");
        if (p.isEmpty()) p = "id";
        if (!p.endsWith("_")) p = p + "_";

        String tsHex = Long.toHexString(Instant.now().toEpochMilli());
        String uuidHex = UUID.randomUUID().toString().replace("-", "");
        return p + tsHex + "_" + uuidHex;
    }
}
