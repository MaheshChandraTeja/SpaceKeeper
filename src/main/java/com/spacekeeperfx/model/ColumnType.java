package com.spacekeeperfx.model;

/**
 * Supported column types for a Subspace table.
 * TEXT  - free-form text
 * NUMBER - numeric (BigDecimal-friendly; stored as Double for simplicity here)
 * PHOTO - file path to an image on disk (DB stores relative path)
 */
public enum ColumnType {
    TEXT,
    NUMBER,
    PHOTO;

    public boolean isPhoto() {
        return this == PHOTO;
    }

    public boolean isNumeric() {
        return this == NUMBER;
    }

    public static ColumnType fromString(String value) {
        if (value == null) return TEXT;
        return ColumnType.valueOf(value.trim().toUpperCase());
    }
}
