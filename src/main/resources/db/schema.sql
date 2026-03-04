-- SpaceKeeperFX — SQLite schema
-- Baseline tables for Vaults, Subspaces, Column Definitions, Records, and EAV Record Values.
-- Initial three logical columns per subspace are intended:
--   1) Name / ID (TEXT, required, unique)
--   2) Photograph (PHOTO, optional; stores relative file path)
--   3) Description (TEXT)
-- These are usually seeded by the application when a subspace is created.

PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS vault (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS subspace (
  id TEXT PRIMARY KEY,
  vault_id TEXT NOT NULL REFERENCES vault(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS column_def (
  id TEXT PRIMARY KEY,
  subspace_id TEXT NOT NULL REFERENCES subspace(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  type TEXT NOT NULL,           -- TEXT | NUMBER | PHOTO
  enabled INTEGER NOT NULL,     -- 0/1
  required INTEGER NOT NULL,    -- 0/1
  is_unique INTEGER NOT NULL,   -- 0/1
  display_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_column_def_subspace ON column_def(subspace_id);

CREATE TABLE IF NOT EXISTS record (
  id TEXT PRIMARY KEY,
  subspace_id TEXT NOT NULL REFERENCES subspace(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_record_subspace ON record(subspace_id);

-- EAV for cell values. Store either text_value or number_value for a (record_id, column_id).
CREATE TABLE IF NOT EXISTS record_value (
  record_id TEXT NOT NULL REFERENCES record(id) ON DELETE CASCADE,
  column_id TEXT NOT NULL REFERENCES column_def(id) ON DELETE CASCADE,
  text_value   TEXT,
  number_value REAL,
  PRIMARY KEY (record_id, column_id)
);

-- Optional uniqueness enforcement for TEXT/PHOTO columns at subspace scope
-- (applies when column_def.is_unique = 1).
CREATE TRIGGER IF NOT EXISTS trg_unique_text_value
BEFORE INSERT ON record_value
WHEN NEW.text_value IS NOT NULL
BEGIN
  SELECT
    CASE
      WHEN EXISTS (
        SELECT 1
        FROM record_value rv
        JOIN record r ON r.id = rv.record_id
        JOIN column_def c ON c.id = rv.column_id
        WHERE rv.text_value = NEW.text_value
          AND c.is_unique = 1
          AND rv.column_id = NEW.column_id
          AND r.subspace_id = (SELECT subspace_id FROM record WHERE id = NEW.record_id)
      )
      THEN RAISE(ABORT, 'Unique text value already exists for this column in the subspace.')
    END;
END;
