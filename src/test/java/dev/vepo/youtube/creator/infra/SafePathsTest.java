package dev.vepo.youtube.creator.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class SafePathsTest {

    @Test
    void requireObjectIdAcceptsValidId() {
        assertEquals("507f1f77bcf86cd799439011", SafePaths.requireObjectId("507f1f77bcf86cd799439011"));
    }

    @Test
    void requireObjectIdRejectsInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> SafePaths.requireObjectId("../etc/passwd"));
    }

    @Test
    void safeBasenameStripsDirectoryComponents() {
        assertEquals("clip.mp4", SafePaths.safeBasename("/tmp/evil/../clip.mp4"));
    }

    @Test
    void safeBasenameRejectsTraversal() {
        assertThrows(IllegalArgumentException.class, () -> SafePaths.safeBasename(".."));
    }

    @Test
    void resolveWithinKeepsPathInsideBase() {
        Path base = Path.of("target/safe-paths");
        Path resolved = SafePaths.resolveWithin(base, "preview_123.mp4");
        assertTrue(resolved.normalize().startsWith(base.toAbsolutePath().normalize()));
    }

    @Test
    void resolveRelativeWithinRejectsEscapeAttempts() {
        Path base = Path.of("target/safe-paths");
        assertThrows(IllegalArgumentException.class, () -> SafePaths.resolveRelativeWithin(base, "../outside.txt"));
    }
}
