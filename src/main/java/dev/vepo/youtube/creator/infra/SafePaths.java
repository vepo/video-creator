package dev.vepo.youtube.creator.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bson.types.ObjectId;

public final class SafePaths {

    private SafePaths() {
    }

    public static String requireObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new IllegalArgumentException("Invalid project id");
        }
        return id;
    }

    public static String safeBasename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        String base = Path.of(filename).getFileName().toString();
        if (base.isBlank() || base.contains("..") || base.contains("/") || base.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
        return base;
    }

    public static Path resolveWithin(Path baseDir, String filename) {
        Path normalizedBase = baseDir.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(safeBasename(filename)).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new IllegalArgumentException("Path traversal detected");
        }
        return resolved;
    }

    public static Path resolveRelativeWithin(Path baseDir, String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid path");
        }
        Path normalizedBase = baseDir.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new IllegalArgumentException("Path traversal detected");
        }
        return resolved;
    }

    public static Path createTempDirectory(Path tempRoot, String prefix) throws IOException {
        Path root = tempRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        return Files.createTempDirectory(root, prefix);
    }

    public static Path createTempFile(Path tempRoot, String prefix, String suffix) throws IOException {
        Path root = tempRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        return Files.createTempFile(root, prefix, suffix);
    }
}
