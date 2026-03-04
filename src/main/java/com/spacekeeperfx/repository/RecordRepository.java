package com.spacekeeperfx.repository;

import com.spacekeeperfx.model.Record;
import com.spacekeeperfx.persistence.dao.RecordDao;

import java.math.BigDecimal;
import java.util.List;

public class RecordRepository {

    private final RecordDao recordDao;

    public RecordRepository(RecordDao recordDao) {
        this.recordDao = recordDao;
    }

    public List<Record> listBySubspace(String subspaceId) {
        return recordDao.listBySubspace(subspaceId);
    }

    public Record createBlank(String subspaceId) {
        return recordDao.createBlank(subspaceId);
    }

    public boolean deleteByIds(String subspaceId, List<String> ids) {
        return recordDao.deleteByIds(subspaceId, ids);
    }

    public void setText(String recordId, String columnId, String value) {
        recordDao.setText(recordId, columnId, value);
    }

    public void setNumber(String recordId, String columnId, BigDecimal value) {
        recordDao.setNumber(recordId, columnId, value);
    }

    public void setPhotoPath(String recordId, String columnId, String relativeOrAbsolutePath) {
        recordDao.setPhotoPath(recordId, columnId, relativeOrAbsolutePath);
    }
}
