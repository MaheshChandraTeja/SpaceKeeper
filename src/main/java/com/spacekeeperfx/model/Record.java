package com.spacekeeperfx.model;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * A single row in a Subspace table.
 * Values are stored in a columnId -> Object map; UI and persistence layers
 * use ColumnDef.type to coerce values.
 *
 * Conventions:
 * - "col_name_id" (TEXT, required, unique)     -> primary human-friendly key
 * - "col_photo"   (PHOTO, optional)            -> relative file path string
 * - "col_description" (TEXT)                   -> description
 */
public final class Record {
    private final String id;            // stable generated ID (not the human Name/ID)
    private final String subspaceId;    // parent link
    private final Map<String, Object> values; // columnId -> value
    private final Instant createdAt;
    private Instant updatedAt;

    public Record(String subspaceId) {
        this(IdGenerator.newIdWithPrefix("rec_"), subspaceId, new LinkedHashMap<>());
    }

    public Record(String id, String subspaceId, Map<String, Object> initialValues) {
        this.id = Objects.requireNonNull(id, "id");
        this.subspaceId = Objects.requireNonNull(subspaceId, "subspaceId");
        this.values = (initialValues == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(initialValues);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }

    public String getSubspaceId() { return subspaceId; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public Map<String, Object> getValues() { return Collections.unmodifiableMap(values); }

    public void put(String columnId, Object value) {
        Objects.requireNonNull(columnId, "columnId");
        values.put(columnId, value);
        this.updatedAt = Instant.now();
    }

    public Object get(String columnId) {
        return values.get(columnId);
    }

    // --- Convenience helpers for common baseline columns ---
    public void setNameId(String nameId) { put("col_name_id", nameId); }

    public Optional<String> getNameId() {
        Object v = get("col_name_id");
        return Optional.ofNullable(v == null ? null : String.valueOf(v));
    }

    public void setDescription(String description) { put("col_description", description); }

    public Optional<String> getDescription() {
        Object v = get("col_description");
        return Optional.ofNullable(v == null ? null : String.valueOf(v));
    }

    /** Store relative path string; image content is managed by ImageService */
    public void setPhotograph(Path relativePath) {
        put("col_photo", (relativePath == null) ? null : relativePath.toString());
    }

    public Optional<String> getPhotographPath() {
        Object v = get("col_photo");
        return Optional.ofNullable(v == null ? null : String.valueOf(v));
    }

    // --- Typed helpers for dynamic numeric/text/photo columns ---
    public void setText(String columnId, String value) { put(columnId, value); }

    public Optional<String> getText(String columnId) {
        Object v = get(columnId);
        return Optional.ofNullable(v == null ? null : String.valueOf(v));
    }

    public void setNumber(String columnId, BigDecimal value) {
        put(columnId, value == null ? null : value);
    }

    public Optional<BigDecimal> getNumber(String columnId) {
        Object v = get(columnId);
        if (v == null) return Optional.empty();
        if (v instanceof BigDecimal bd) return Optional.of(bd);
        if (v instanceof Number n) return Optional.of(BigDecimal.valueOf(n.doubleValue()));
        try {
            return Optional.of(new BigDecimal(String.valueOf(v)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public void setPhotoPath(String columnId, Path relativePath) {
        put(columnId, (relativePath == null) ? null : relativePath.toString());
    }

    public Optional<String> getPhotoPath(String columnId) {
        Object v = get(columnId);
        return Optional.ofNullable(v == null ? null : String.valueOf(v));
    }

    @Override
    public String toString() {
        return "Record{" +
                "id='" + id + '\'' +
                ", subspaceId='" + subspaceId + '\'' +
                ", values=" + values +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
