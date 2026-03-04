package com.spacekeeperfx.model;

import java.time.Instant;
import java.util.*;

/**
 * Vault is the top-level workspace/storage.
 * It can contain multiple Subspaces (tables).
 */
public final class Vault {
    private final String id;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    // Optional: cache of subspace IDs for quick lookups (persistence layer authoritative)
    private final Set<String> subspaceIds = new LinkedHashSet<>();

    public Vault(String name) {
        this(IdGenerator.newIdWithPrefix("vault_"), name);
    }

    public Vault(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName, "newName").trim();
        touch();
    }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public Set<String> getSubspaceIds() {
        return Collections.unmodifiableSet(subspaceIds);
    }

    public void addSubspaceId(String subspaceId) {
        subspaceIds.add(Objects.requireNonNull(subspaceId, "subspaceId"));
        touch();
    }

    public boolean removeSubspaceId(String subspaceId) {
        boolean removed = subspaceIds.remove(subspaceId);
        if (removed) touch();
        return removed;
    }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "Vault{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", subspaceIds=" + subspaceIds +
                '}';
    }
}
