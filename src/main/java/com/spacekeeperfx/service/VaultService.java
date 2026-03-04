package com.spacekeeperfx.service;

import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.model.Vault;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages top-level Vaults (workspaces).
 * In-memory implementation for now; swap to DAO-backed storage later.
 */
public class VaultService {

    private final Map<String, Vault> vaults = new ConcurrentHashMap<>();

    // Optional: cascade deletes into subspaces/records if wired
    private SubspaceService subspaceService;

    public VaultService() {}

    public VaultService(SubspaceService subspaceService) {
        this.subspaceService = subspaceService;
    }

    public void setSubspaceService(SubspaceService subspaceService) {
        this.subspaceService = subspaceService;
    }

    /** Create a new Vault. */
    public Vault createVault(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("Vault name cannot be blank.");
        Vault v = new Vault(trimmed);
        vaults.put(v.getId(), v);
        return v;
    }

    /** Rename an existing Vault. */
    public boolean renameVault(String vaultId, String newName) {
        Vault v = vaults.get(vaultId);
        if (v == null) return false;
        String trimmed = Objects.requireNonNull(newName, "newName").trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("New name cannot be blank.");
        v.rename(trimmed);
        return true;
    }

    /** Delete a Vault and (optionally) cascade delete its Subspaces & Records. */
    public boolean deleteVault(String vaultId, boolean cascade) {
        Vault removed = vaults.remove(vaultId);
        if (removed == null) return false;

        if (cascade && subspaceService != null) {
            var subs = subspaceService.listSubspaces(vaultId);
            for (Subspace s : subs) {
                subspaceService.deleteSubspace(s.getId(), true);
            }
        }
        return true;
    }

    public Optional<Vault> getVault(String vaultId) {
        return Optional.ofNullable(vaults.get(vaultId));
    }

    public List<Vault> listVaults() {
        return vaults.values().stream()
                .sorted(Comparator.comparing(Vault::getCreatedAt))
                .toList();
    }

    /** Track subspace ID membership inside a Vault (purely advisory in-memory). */
    public void addSubspaceToVault(String vaultId, String subspaceId) {
        Vault v = vaults.get(vaultId);
        if (v != null) v.addSubspaceId(subspaceId);
    }

    public void removeSubspaceFromVault(String vaultId, String subspaceId) {
        Vault v = vaults.get(vaultId);
        if (v != null) v.removeSubspaceId(subspaceId);
    }
}
