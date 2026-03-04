package com.spacekeeperfx.util;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight validation helpers.
 */
public final class Validation {

    private Validation() {}

    /** Require non-blank text; returns trimmed string. */
    public static String requireNonBlank(String value, String fieldName) {
        String t = value == null ? "" : value.trim();
        if (t.isBlank()) {
            throw new IllegalArgumentException((fieldName == null ? "Value" : fieldName) + " cannot be blank.");
        }
        return t;
    }

    /** Ensure string length <= max. Returns trimmed string. */
    public static String maxLength(String value, int max, String fieldName) {
        String t = value == null ? "" : value.trim();
        if (t.length() > max) {
            throw new IllegalArgumentException((fieldName == null ? "Value" : fieldName) + " exceeds max length " + max + ".");
        }
        return t;
    }

    /** True if looks like an image filename extension. */
    public static boolean isImageFile(String filename) {
        if (filename == null) return false;
        String f = filename.toLowerCase(Locale.ROOT);
        return f.endsWith(".png") || f.endsWith(".jpg") || f.endsWith(".jpeg")
                || f.endsWith(".gif") || f.endsWith(".bmp") || f.endsWith(".webp");
    }

    /** Try parse BigDecimal from string; throws if invalid and required==true. */
    public static BigDecimal parseBigDecimal(String s, boolean required, String fieldName) {
        String label = fieldName == null ? "number" : fieldName;
        if (s == null || s.trim().isEmpty()) {
            if (required) throw new IllegalArgumentException("Required " + label + " is missing.");
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + " value: " + s);
        }
    }

    /** Enforce uniqueness of a proposed name within a set of existing names. */
    public static void requireUniqueName(Collection<String> existingNames, String proposed, boolean caseInsensitive, String fieldName) {
        if (existingNames == null) return;
        String p = proposed == null ? "" : proposed.trim();
        for (String n : existingNames) {
            if (n == null) continue;
            boolean eq = caseInsensitive ? n.equalsIgnoreCase(p) : n.equals(p);
            if (eq) throw new IllegalArgumentException((fieldName == null ? "Name" : fieldName) + " must be unique.");
        }
    }

    /** Basic safe filename sanitizer; returns a string suitable for filesystem names. */
    public static String sanitizeFilename(String name) {
        String t = requireNonBlank(name, "Filename");
        // Replace forbidden characters on common filesystems
        t = t.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (t.isBlank()) throw new IllegalArgumentException("Filename becomes empty after sanitization.");
        return t;
    }

    /** Check that a path exists and is readable (if provided). */
    public static void requireReadableIfPresent(Path p, String label) {
        if (p == null) return;
        if (!Files.exists(p) || !Files.isReadable(p)) {
            throw new IllegalArgumentException((label == null ? "File" : label) + " is not readable: " + p);
        }
    }
}
