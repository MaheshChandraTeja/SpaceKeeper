package com.spacekeeperfx.repository;

import com.spacekeeperfx.model.Vault;
import com.spacekeeperfx.persistence.dao.VaultDao;

import java.util.List;

public class VaultRepository {
    private final VaultDao vaultDao;

    public VaultRepository(VaultDao vaultDao) {
        this.vaultDao = vaultDao;
    }

    public List<Vault> listAll() {
        return vaultDao.listAll();
    }

    public Vault create(String name) {
        return vaultDao.insert(name);
    }

    public boolean rename(String id, String newName) {
        return vaultDao.rename(id, newName);
    }

    public boolean delete(String id) {
        return vaultDao.delete(id);
    }
}
