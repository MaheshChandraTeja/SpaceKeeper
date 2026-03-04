package com.spacekeeperfx.repository;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.persistence.dao.SubspaceDao;

import java.util.List;

public class SubspaceRepository {

    private final SubspaceDao subspaceDao;

    public SubspaceRepository(SubspaceDao subspaceDao) {
        this.subspaceDao = subspaceDao;
    }

    public List<Subspace> listByVault(String vaultId) {
        return subspaceDao.listByVault(vaultId);
    }

    public Subspace getById(String subspaceId) {
        return subspaceDao.findById(subspaceId);
    }

    public Subspace create(String vaultId, String name, boolean enablePhotograph) {
        return subspaceDao.insert(vaultId, name, enablePhotograph);
    }

    public boolean rename(String subspaceId, String newName) {
        return subspaceDao.rename(subspaceId, newName);
    }

    public boolean delete(String subspaceId) {
        return subspaceDao.delete(subspaceId);
    }

    /** Replace all column defs for a subspace (add/update/remove). */
    public void replaceColumns(String subspaceId, List<ColumnDef> defs) {
        subspaceDao.replaceColumns(subspaceId, defs);
    }
}
