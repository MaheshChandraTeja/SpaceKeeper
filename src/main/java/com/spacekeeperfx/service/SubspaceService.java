package com.spacekeeperfx.service;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.Subspace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Subspaces (tables) within a Vault.
 * In-memory storage; replace with repository/DAO later.
 */
public class SubspaceService {

    // subspaceId -> Subspace
    private final Map<String, Subspace> subspaces = new ConcurrentHashMap<>();
    // vaultId -> ordered subspaceIds
    private final Map<String, List<String>> byVault = new ConcurrentHashMap<>();

    // Optional cascade
    private RecordService recordService;
    private VaultService vaultService;

    public SubspaceService() {}

    public SubspaceService(RecordService recordService, VaultService vaultService) {
        this.recordService = recordService;
        this.vaultService = vaultService;
    }

    public void setRecordService(RecordService recordService) { this.recordService = recordService; }
    public void setVaultService(VaultService vaultService) { this.vaultService = vaultService; }

    /** Create a Subspace with baseline columns (Name/ID, Photograph opt, Description). */
    public Subspace createSubspace(String vaultId, String name, boolean enablePhotograph) {
        Objects.requireNonNull(vaultId, "vaultId");
        String trimmed = Objects.requireNonNull(name, "name").trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("Subspace name cannot be blank.");

        Subspace s = Subspace.createDefault(vaultId, trimmed, enablePhotograph);
        subspaces.put(s.getId(), s);
        byVault.computeIfAbsent(vaultId, k -> new ArrayList<>()).add(s.getId());

        if (vaultService != null) {
            vaultService.addSubspaceToVault(vaultId, s.getId());
        }
        return s;
    }

    public boolean renameSubspace(String subspaceId, String newName) {
        Subspace s = subspaces.get(subspaceId);
        if (s == null) return false;
        String trimmed = Objects.requireNonNull(newName, "newName").trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("Subspace name cannot be blank.");
        s.rename(trimmed);
        return true;
    }

    /** Replace the column set (rename/enable/disable/order changes already in ColumnDef). */
    public boolean updateColumns(String subspaceId, List<ColumnDef> newDefs) {
        Subspace s = subspaces.get(subspaceId);
        if (s == null) return false;
        Objects.requireNonNull(newDefs, "newDefs");

        // Minimal validation: ensure at least one enabled TEXT column exists (Name/ID)
        boolean hasNameId = newDefs.stream()
                .anyMatch(cd -> cd.isEnabled() && "col_name_id".equals(cd.getId()));
        if (!hasNameId) {
            throw new IllegalArgumentException("The required 'Name / ID' column must remain enabled.");
        }

        // Replace internal list by creating a new Subspace object or mutating via reflection?
        // We'll mutate by reconstructing: keep same id/vaultId/name/timestamps, but replace columns list.
        // Since Subspace exposes only read-only list, we reflect change via private method pattern.
        // Simplest: clear and add using access method we control (not present), so we recreate instead:
        Subspace updated = new Subspace(s.getId(), s.getVaultId(), s.getName(), newDefs);
        subspaces.put(s.getId(), updated);
        return true;
    }

    /** Delete a Subspace; optionally cascade delete its Records. */
    public boolean deleteSubspace(String subspaceId, boolean cascade) {
        Subspace s = subspaces.remove(subspaceId);
        if (s == null) return false;

        List<String> list = byVault.getOrDefault(s.getVaultId(), Collections.emptyList());
        list.remove(subspaceId);

        if (cascade && recordService != null) {
            recordService.deleteAllInSubspace(subspaceId);
        }
        if (vaultService != null) {
            vaultService.removeSubspaceFromVault(s.getVaultId(), s.getId());
        }
        return true;
    }

    /** Remove all subspaces under a Vault (used when deleting a Vault). */
    public void deleteAllByVault(String vaultId, boolean cascade) {
        List<String> ids = new ArrayList<>(byVault.getOrDefault(vaultId, List.of()));
        for (String id : ids) deleteSubspace(id, cascade);
        byVault.remove(vaultId);
    }

    public Optional<Subspace> getSubspace(String subspaceId) {
        return Optional.ofNullable(subspaces.get(subspaceId));
    }

    public List<Subspace> listSubspaces(String vaultId) {
        List<String> ids = byVault.getOrDefault(vaultId, List.of());
        List<Subspace> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Subspace s = subspaces.get(id);
            if (s != null) result.add(s);
        }
        // keep insertion order
        return Collections.unmodifiableList(result);
    }

    /** Utility: ensure a subspaceId belongs to a given vaultId. */
    public boolean belongsToVault(String subspaceId, String vaultId) {
        return byVault.getOrDefault(vaultId, List.of()).contains(subspaceId);
    }
}
