package com.spacekeeperfx.service;

import com.spacekeeperfx.model.IdGenerator;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles saving and resolving photographs on disk.
 * Stores files under a base "photos" directory; DB/UI work with relative paths.
 */
public class ImageService {

    private Path basePhotosDir;

    /**
     * Default base: ~/.spacekeeper/photos
     */
    public ImageService() {
        this(defaultBase());
    }

    public ImageService(Path basePhotosDir) {
        this.basePhotosDir = Objects.requireNonNull(basePhotosDir);
    }

    private static Path defaultBase() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".spacekeeper", "photos");
    }

    /** Ensure base directory exists. */
    public void init() {
        try {
            Files.createDirectories(basePhotosDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create photos directory: " + basePhotosDir, e);
        }
    }

    /**
     * Import (copy) an image file into the managed photos dir.
     * @param source absolute path to an existing image file
     * @return relative path to store in the DB/Record (e.g., "2025/08/img_XXXXXX.jpg")
     */
    public String importPhoto(File file) {
        if (file == null) return null;
        String ext = "";
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) ext = name.substring(dot);
        String target = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.createDirectories(basePhotosDir);
            Files.copy(file.toPath(), basePhotosDir.resolve(target), StandardCopyOption.REPLACE_EXISTING);
            return target; // relative
        } catch (IOException e) {
            throw new RuntimeException("Failed to import photo: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a managed photo by its relative path.
     * @return true if deleted or the file didn't exist; false on failure.
     */
    public boolean deleteManagedPhoto(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return true;
        Path abs = resolve(relativePath);
        try {
            Files.deleteIfExists(abs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Resolve a relative photo path to an absolute path on disk. */
    public Path resolve(String relativePath) {
        if (relativePath == null) return basePhotosDir;
        return basePhotosDir.resolve(relativePath).normalize();
    }

    /** Convert absolute path inside base dir back to a relative path (if applicable). */
    public Optional<String> toRelative(Path absolute) {
        try {
            Path norm = absolute.toRealPath();
            Path base = basePhotosDir.toRealPath();
            if (norm.startsWith(base)) {
                return Optional.of(base.relativize(norm).toString().replace('\\', '/'));
            }
        } catch (IOException ignored) {}
        return Optional.empty();
    }

    public void setBaseDir(Path dir) {
        this.basePhotosDir = dir;
        init();
    }

    private static Optional<String> extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return Optional.empty();
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        // basic sanitization
        if (ext.matches("[a-z0-9]{1,5}")) return Optional.of(ext);
        return Optional.empty();
    }

    public Image load(String relPath, int fitW, int fitH) {
        if (relPath == null || relPath.isBlank()) return null;
        try {
            return new Image(basePhotosDir.resolve(relPath).toUri().toString(), fitW, fitH, true, true);
        } catch (Exception e) {
            return null;
        }
    }
}
