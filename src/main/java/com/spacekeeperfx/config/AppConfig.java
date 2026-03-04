package com.spacekeeperfx.config;

import javafx.scene.Scene;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConfig {

    private final Path dbPath;
    private final Path photosDir;
    private final boolean darkTheme;

    private AppConfig(Path dbPath, Path photosDir, boolean darkTheme) {
        this.dbPath = dbPath;
        this.photosDir = photosDir;
        this.darkTheme = darkTheme;
    }

    public static AppConfig load() {
        Path home   = appHome();
        Path vaults = vaultsDir();
        Path photos = defaultPhotosRoot();
        Path db     = home.resolve("spacekeeper.db");

        try {
            Files.createDirectories(home);
            Files.createDirectories(vaults);
            Files.createDirectories(photos);
        } catch (Exception ignored) {}
        boolean dark = true;
        return new AppConfig(db, photos, dark);
    }

    public Path getDbPath()    { return dbPath; }
    public Path getPhotosDir() { return photosDir; }
    public boolean isDarkTheme() { return darkTheme; }

    public void applyTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(resource("/css/base.css"));
        if (darkTheme) {
            scene.getStylesheets().add(resource("/css/dark.css"));
        }
    }

    private static String resource(String p) {
        var url = AppConfig.class.getResource(p);
        return url == null ? "" : url.toExternalForm();
    }

     public static Path appHome() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".spacekeeper");
    }

    public static Path vaultsDir() {
        return appHome().resolve("vaults");
    }

    public static Path defaultPhotosRoot() {
        return appHome().resolve("photos");
    }

    public static Path photosDirFor(com.spacekeeperfx.persistence.Database db) {
        Path dbFile = db.getDbFile();
        Path folder = dbFile == null ? vaultsDir().resolve("photos") :
                dbFile.getParent().resolve(dbFile.getFileName().toString().replaceFirst("\\.db$", "") + "_photos");
        try { Files.createDirectories(folder); } catch (Exception ignored) {}
        return folder;
    }
}
