package com.spacekeeperfx.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class Database {

    private final Path dbFile;
    private final String jdbcUrl;

    public Database() {
        this(defaultDbPath());
    }

    public Database(Path dbFile) {
        this.dbFile = dbFile;
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    public java.nio.file.Path getDbFile() {
        return this.dbFile;
    }

    private static Path defaultDbPath() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".spacekeeper", "spacekeeper.db");
    }

    /** Ensure directory exists, open a connection, turn on WAL + FKs, then run migrations. */
    public void init() {
        try {
            Files.createDirectories(dbFile.getParent());
        } catch (Exception e) {
            throw new RuntimeException("Cannot create DB directory: " + dbFile.getParent(), e);
        }

        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("PRAGMA foreign_keys=ON;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite pragmas.", e);
        }

        new MigrationRunner(this).migrate();
    }

    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    /** Utility to run a UNIT OF WORK inside a transaction. */
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = getConnection()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                c.setAutoCommit(old);
                return result;
            } catch (SQLException e) {
                c.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    // Optional: expose the db file path (handy for AppConfig.photosDirFor(db))
    public Path getDbPath() { return dbFile; }

    /**
     * Close underlying resources.
     * If you open a new Connection per operation (typical for SQLite), this is a no-op.
     * Safe to call multiple times.
     */
    public void close() { /* nothing to close */ }

    // inside com.spacekeeperfx.persistence.Database

    /**
     * Open (or create) a SQLite database at the given path and run migrations.
     */
    public static Database open(java.nio.file.Path dbPath) {
        java.nio.file.Path p = dbPath.toAbsolutePath();
        try {
            java.nio.file.Path parent = p.getParent();
            if (parent != null) java.nio.file.Files.createDirectories(parent);
        } catch (Exception ignored) {}

        Database db;
        // Prefer a constructor that already exists in your class.
        // If you have Database(Path) use that; otherwise fall back to a JDBC-URL ctor.
        try {
            var ctor = Database.class.getDeclaredConstructor(java.nio.file.Path.class);
            ctor.setAccessible(true);
            db = ctor.newInstance(p);
        } catch (NoSuchMethodException e) {
            // If you DON'T have a (Path) ctor, but do have a (String url) ctor:
            db = new Database(Path.of("jdbc:sqlite:" + p.toString()));
            // Stash the path if your class has a dbPath field (used by AppConfig.photosDirFor(db))
            try {
                var f = Database.class.getDeclaredField("dbPath");
                f.setAccessible(true);
                f.set(db, p);
            } catch (Exception ignored) {}
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to construct Database for: " + p, e);
        }

        db.init(); // your existing migration bootstrap (MigrationRunner.migrate(this))
        return db;
    }

}
