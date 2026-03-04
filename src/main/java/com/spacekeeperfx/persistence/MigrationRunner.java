package com.spacekeeperfx.persistence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MigrationRunner {

    private final Database db;

    public MigrationRunner(Database db) { this.db = db; }

    public void migrate() {
        List<Migration> plan = migrations();
        db.inTransaction(c -> {
            int ver = readUserVersion(c);
            for (Migration m : plan) {
                if (ver < m.toVersion) {
                    m.apply(c);
                    setUserVersion(c, m.toVersion);
                    ver = m.toVersion;
                }
            }
            return null;
        });
    }

    private int readUserVersion(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version;")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void setUserVersion(Connection c, int v) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA user_version=" + v);
        }
    }

    /** Build migration plan with statements in dependency-correct order. */
    private List<Migration> migrations() {
        List<Migration> list = new ArrayList<>();

        // --- V1: initial schema for a single-vault DB file ---
        List<String> v1 = List.of(
                // meta
                """
                CREATE TABLE IF NOT EXISTS meta (
                  key   TEXT PRIMARY KEY,
                  value TEXT NOT NULL
                )
                """,
                // vault (one row per file)
                """
                CREATE TABLE IF NOT EXISTS vault (
                  id         TEXT PRIMARY KEY,
                  name       TEXT NOT NULL,
                  created_at INTEGER NOT NULL,
                  updated_at INTEGER NOT NULL
                )
                """,
                // subspace
                """
                CREATE TABLE IF NOT EXISTS subspace (
                  id         TEXT PRIMARY KEY,
                  vault_id   TEXT NOT NULL REFERENCES vault(id) ON DELETE CASCADE,
                  name       TEXT NOT NULL,
                  created_at INTEGER NOT NULL,
                  updated_at INTEGER NOT NULL
                )
                """,
                // column_def (CREATE TABLE must come BEFORE its index)
                """
                CREATE TABLE IF NOT EXISTS column_def (
                  id            TEXT PRIMARY KEY,
                  subspace_id   TEXT NOT NULL REFERENCES subspace(id) ON DELETE CASCADE,
                  name          TEXT NOT NULL,
                  type          TEXT NOT NULL,          -- TEXT | NUMBER | PHOTO
                  enabled       INTEGER NOT NULL,       -- 0/1
                  required      INTEGER NOT NULL,       -- 0/1
                  is_unique     INTEGER NOT NULL,       -- 0/1
                  display_order INTEGER NOT NULL
                )
                """,
                // index on column_def AFTER table exists
                "CREATE INDEX IF NOT EXISTS idx_column_def_subspace ON column_def(subspace_id)",

                // record
                """
                CREATE TABLE IF NOT EXISTS record (
                  id         TEXT PRIMARY KEY,
                  subspace_id TEXT NOT NULL REFERENCES subspace(id) ON DELETE CASCADE,
                  created_at INTEGER NOT NULL,
                  updated_at INTEGER NOT NULL
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_record_subspace ON record(subspace_id)",

                // record_value
                """
                CREATE TABLE IF NOT EXISTS record_value (
                  record_id    TEXT NOT NULL REFERENCES record(id) ON DELETE CASCADE,
                  column_id    TEXT NOT NULL REFERENCES column_def(id) ON DELETE CASCADE,
                  text_value   TEXT,
                  number_value REAL,
                  PRIMARY KEY (record_id, column_id)
                )
                """,

                // trigger (single statement)
                """
                CREATE TRIGGER IF NOT EXISTS trg_unique_text_value
                BEFORE INSERT ON record_value
                WHEN NEW.text_value IS NOT NULL
                BEGIN
                  SELECT CASE
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
                """
        );

        list.add(new Migration(1, v1));
        return list;
    }

    private static final class Migration {
        final int toVersion;
        final List<String> statements;
        Migration(int toVersion, List<String> statements) {
            this.toVersion = toVersion;
            this.statements = statements;
        }
        void apply(Connection c) throws SQLException {
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
                for (String sql : statements) {
                    String s = sql.trim();
                    if (!s.isEmpty()) st.execute(s);
                }
            }
        }
    }
}
