package com.spacekeeperfx.model;

import java.time.Instant;
import java.util.*;

import com.spacekeeperfx.util.IdGenerator;

/**
 * A Subspace is a table inside a Vault.
 * It owns a list of ColumnDefs (including the baseline columns).
 */
public final class Subspace {
    private final String id;
    private final String vaultId;
    private String name;
    private final List<ColumnDef> columns;
    private final Instant createdAt;
    private Instant updatedAt;

    public static Subspace createDefault(String vaultId, String displayName, boolean enablePhotograph) {
        Subspace s = new Subspace(vaultId, displayName);
        // Baseline columns
        s.columns.add(ColumnDef.standardNameId());
        s.columns.add(ColumnDef.standardPhotograph(enablePhotograph)); // optional
        s.columns.add(ColumnDef.standardDescription());
        s.sortByDisplayOrder();
        return s;
    }

    public Subspace(String vaultId, String name) {
        this(IdGenerator.newIdWithPrefix("sub_"), vaultId, name, new ArrayList<>());
    }

    public Subspace(String id, String vaultId, String name, List<ColumnDef> initialColumns) {
        this.id = Objects.requireNonNull(id, "id");
        this.vaultId = Objects.requireNonNull(vaultId, "vaultId");
        this.name = Objects.requireNonNull(name, "name");
        this.columns = (initialColumns == null) ? new ArrayList<>() : new ArrayList<>(initialColumns);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }

    public String getVaultId() { return vaultId; }

    public String getName() { return name; }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "newName").trim();
        touch();
    }

    public List<ColumnDef> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    public Optional<ColumnDef> findColumnById(String columnId) {
        return columns.stream().filter(c -> c.getId().equals(columnId)).findFirst();
    }

    public Optional<ColumnDef> findColumnByName(String name) {
        return columns.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void addColumn(ColumnDef columnDef) {
        Objects.requireNonNull(columnDef, "columnDef");
        // Guard: no duplicate IDs
        if (findColumnById(columnDef.getId()).isPresent()) {
            throw new IllegalArgumentException("Column with id already exists: " + columnDef.getId());
        }
        columns.add(columnDef);
        sortByDisplayOrder();
        touch();
    }

    public boolean removeColumnById(String columnId) {
        boolean removed = columns.removeIf(c -> c.getId().equals(columnId));
        if (removed) touch();
        return removed;
    }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    private void touch() { this.updatedAt = Instant.now(); }

    public void setColumns(List<ColumnDef> cols) {
        this.columns.clear();
        if (cols != null) {
            for (ColumnDef c : cols) {
                // ensure the back-reference is correct
                if (c.getSubspaceId() == null || c.getSubspaceId().isBlank()) {
                    c.setSubspaceId(this.id);
                }
            }
            this.columns.addAll(cols);
        }
        sortByDisplayOrder();
    }

    /** Optional helper if you don’t already have it. */
    public void sortByDisplayOrder() {
        this.columns.sort(
                Comparator.comparingInt(ColumnDef::getDisplayOrder)
                        .thenComparing(ColumnDef::getName, String.CASE_INSENSITIVE_ORDER)
        );
    }

    @Override
    public String toString() {
        return "Subspace{" +
                "id='" + id + '\'' +
                ", vaultId='" + vaultId + '\'' +
                ", name='" + name + '\'' +
                ", columns=" + columns +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
