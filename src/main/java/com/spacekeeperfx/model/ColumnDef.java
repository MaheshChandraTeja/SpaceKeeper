package com.spacekeeperfx.model;

import com.spacekeeperfx.util.IdGenerator;

import java.util.Objects;

public class ColumnDef {
    private String id;
    private String subspaceId;
    private String name;
    private ColumnType type;
    private boolean enabled;
    private boolean required;
    private boolean unique;
    private int displayOrder;

    public ColumnDef() {}

    public ColumnDef(String id,
                     String subspaceId,
                     String name,
                     ColumnType type,
                     boolean enabled,
                     boolean required,
                     boolean unique,
                     int displayOrder) {
        this.id = id;
        this.subspaceId = subspaceId;
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.required = required;
        this.unique = unique;
        this.displayOrder = displayOrder;
    }

    public ColumnDef(String id,
                     String subspaceId,
                     ColumnType type,
                     boolean enabled,
                     boolean required,
                     boolean unique,
                     int displayOrder) {
        this(id, subspaceId, "", type, enabled, required, unique, displayOrder);
    }

    public static ColumnDef of(String id,
                               ColumnType type,
                               boolean enabled,
                               boolean required,
                               boolean unique,
                               int displayOrder) {
        return new ColumnDef(id, null, "", type, enabled, required, unique, displayOrder);
    }

    public static ColumnDef of(String id,
                               String subspaceId,
                               ColumnType type,
                               boolean enabled,
                               boolean required,
                               boolean unique,
                               int displayOrder) {
        return new ColumnDef(id, subspaceId, "", type, enabled, required, unique, displayOrder);
    }

    public static ColumnDef of(String id,
                               String subspaceId,
                               String name,
                               ColumnType type,
                               boolean enabled,
                               boolean required,
                               boolean unique,
                               int displayOrder) {
        return new ColumnDef(id, subspaceId, name, type, enabled, required, unique, displayOrder);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubspaceId() { return subspaceId; }
    public void setSubspaceId(String subspaceId) { this.subspaceId = subspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ColumnType getType() { return type; }
    public void setType(ColumnType type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public boolean isUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnDef that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ColumnDef{" +
                "id='" + id + '\'' +
                ", subspaceId='" + subspaceId + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", enabled=" + enabled +
                ", required=" + required +
                ", unique=" + unique +
                ", displayOrder=" + displayOrder +
                '}';
    }

    public static ColumnDef standardNameId() {
        return new ColumnDef(
                com.spacekeeperfx.util.IdGenerator.newId("col"),
                null,
                "Name / ID",
                ColumnType.TEXT,
                true,
                true,
                true,
                0
        );
    }

    public static ColumnDef standardPhotograph(boolean enabled) {
        return new ColumnDef(
                com.spacekeeperfx.util.IdGenerator.newId("col"),
                null,
                "Photograph",
                ColumnType.PHOTO,
                enabled,
                false,
                false,
                1
        );
    }

    public static ColumnDef standardDescription() {
        return new ColumnDef(
                IdGenerator.newId("col"),
                null,
                "Description",
                ColumnType.TEXT,
                true,
                false,
                false,
                2
        );
    }

    public ColumnDef rename(String newName) {
        this.name = newName == null ? "" : newName.trim();
        return this;
    }

    public ColumnDef withSubspace(String subspaceId) {
        this.subspaceId = subspaceId;
        return this;
    }

    public ColumnDef withDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
        return this;
    }
}
