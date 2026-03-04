package com.spacekeeperfx.service;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Record;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages Records (rows) within a Subspace.
 * In-memory store; replace with DAO-backed implementation later.
 */
public class RecordService {

    // subspaceId -> ordered list of Records
    private final Map<String, List<Record>> bySubspace = new ConcurrentHashMap<>();

    // Optional: for validation against column definitions
    private SubspaceService subspaceService;

    public RecordService() {}

    public RecordService(SubspaceService subspaceService) {
        this.subspaceService = subspaceService;
    }

    public void setSubspaceService(SubspaceService subspaceService) {
        this.subspaceService = subspaceService;
    }

    /** Create and add a blank Record for a subspace. */
    public Record createBlank(String subspaceId) {
        var r = new Record(subspaceId);
        addRecord(subspaceId, r);
        return r;
    }

    /** Add an existing Record; validates required/unique constraints if subspaceService is provided. */
    public void addRecord(String subspaceId, Record record) {
        Objects.requireNonNull(subspaceId, "subspaceId");
        Objects.requireNonNull(record, "record");
        record = ensureSubspaceId(record, subspaceId);

        if (subspaceService != null) {
            var cols = subspaceService.getSubspace(subspaceId)
                    .map(s -> s.getColumns())
                    .orElse(List.of());
            validate(record, cols, true);
        }

        bySubspace.computeIfAbsent(subspaceId, k ->
                Collections.synchronizedList(new ArrayList<>())
        ).add(0, record); // newest first
    }

    /** Update/replace a Record's values after inline edits. */
    public boolean updateRecord(String subspaceId, Record updated) {
        List<Record> list = bySubspace.get(subspaceId);
        if (list == null) return false;

        if (subspaceService != null) {
            var cols = subspaceService.getSubspace(subspaceId)
                    .map(s -> s.getColumns())
                    .orElse(List.of());
            validate(updated, cols, true);
        }

        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), updated.getId())) {
                list.set(i, updated);
                return true;
            }
        }
        return false;
    }

    /** Delete records by ID. */
    public int deleteRecords(String subspaceId, Collection<String> recordIds) {
        List<Record> list = bySubspace.get(subspaceId);
        if (list == null || recordIds == null || recordIds.isEmpty()) return 0;
        int before = list.size();
        list.removeIf(r -> recordIds.contains(r.getId()));
        return before - list.size();
    }

    /** Remove all rows in a subspace (cascade). */
    public void deleteAllInSubspace(String subspaceId) {
        bySubspace.remove(subspaceId);
    }

    public List<Record> listRecords(String subspaceId) {
        List<Record> list = bySubspace.getOrDefault(subspaceId, List.of());
        return Collections.unmodifiableList(list);
    }

    public Optional<Record> getRecord(String subspaceId, String recordId) {
        return bySubspace.getOrDefault(subspaceId, List.of()).stream()
                .filter(r -> Objects.equals(r.getId(), recordId))
                .findFirst();
    }

    // --- Validation helpers ---

    /**
     * Ensures required columns are present and 'unique' constraints are upheld within the subspace.
     * Currently checks:
     *  - required TEXT (e.g., "col_name_id") non-blank
     *  - unique columns do not collide (case-insensitive for TEXT)
     */
    public void validate(Record record, List<ColumnDef> columns, boolean checkUniqueness) {
        Map<String, Object> vals = record.getValues();

        for (ColumnDef c : columns) {
            if (!c.isEnabled()) continue;

            Object v = vals.get(c.getId());

            if (c.isRequired()) {
                switch (c.getType()) {
                    case TEXT -> {
                        String s = v == null ? "" : String.valueOf(v).trim();
                        if (s.isBlank()) {
                            throw new IllegalArgumentException("Required field '" + c.getName() + "' cannot be blank.");
                        }
                    }
                    case NUMBER -> {
                        if (v == null) {
                            throw new IllegalArgumentException("Required field '" + c.getName() + "' must be a number.");
                        }
                        if (!(v instanceof BigDecimal) && !(v instanceof Number)) {
                            try {
                                new BigDecimal(String.valueOf(v));
                            } catch (Exception ex) {
                                throw new IllegalArgumentException("Required field '" + c.getName() + "' must be numeric.");
                            }
                        }
                    }
                    case PHOTO -> {
                        // Optional interpretation: PHOTO required => non-null path string
                        String s = v == null ? "" : String.valueOf(v).trim();
                        if (s.isBlank()) {
                            throw new IllegalArgumentException("Required photo for '" + c.getName() + "'.");
                        }
                    }
                }
            }
        }

        if (checkUniqueness) {
            List<ColumnDef> uniqueCols = columns.stream().filter(ColumnDef::isUnique).toList();
            if (!uniqueCols.isEmpty()) {
                // Find the subspace list to check against
                List<Record> siblings = bySubspace.getOrDefault(record.getSubspaceId(), List.of());
                for (ColumnDef c : uniqueCols) {
                    Object newVal = vals.get(c.getId());
                    for (Record sib : siblings) {
                        if (sib.getId().equals(record.getId())) continue; // ignore self
                        Object otherVal = sib.getValues().get(c.getId());
                        if (equalsForType(c.getType(), newVal, otherVal)) {
                            throw new IllegalArgumentException("Value for unique field '" + c.getName() + "' already exists.");
                        }
                    }
                }
            }
        }
    }

    private boolean equalsForType(ColumnType type, Object a, Object b) {
        if (a == null && b == null) return false; // treat as no collision
        if (a == null || b == null) return false;
        return switch (type) {
            case TEXT, PHOTO -> String.valueOf(a).trim().equalsIgnoreCase(String.valueOf(b).trim());
            case NUMBER -> {
                try {
                    BigDecimal A = (a instanceof BigDecimal bd) ? bd : new BigDecimal(String.valueOf(a));
                    BigDecimal B = (b instanceof BigDecimal bd) ? bd : new BigDecimal(String.valueOf(b));
                    yield A.compareTo(B) == 0;
                } catch (Exception e) {
                    yield false;
                }
            }
        };
    }

    private Record ensureSubspaceId(Record r, String subspaceId) {
        if (!Objects.equals(r.getSubspaceId(), subspaceId)) {
            // Recreate preserving values
            return new Record(r.getId(), subspaceId, new LinkedHashMap<>(r.getValues()));
        }
        return r;
    }
}
