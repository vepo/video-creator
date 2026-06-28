package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class ScreenSizeTest {

    @Test
    void defaultScreenSizeIsFullHdLandscape() {
        var screenSize = ScreenSize.getDefault();

        assertEquals("1920x1080", screenSize.getResolution());
        assertTrue(screenSize.isLandscape());
    }

    @Test
    void findScreenSizeByResolutionString() {
        var screenSize = ScreenSize.fromResolution("1280x720");

        assertNotNull(screenSize);
        assertTrue(screenSize.getName().contains("720p"));
    }

    @Test
    void verticalScreenSizesAreTallerThanWide() {
        var screenSize = ScreenSize.VERTICAL_1080p;

        assertTrue(screenSize.isVertical());
    }

    @Test
    void squareScreenSizesHaveEqualWidthAndHeight() {
        var screenSize = ScreenSize.SQUARE_720p;

        assertTrue(screenSize.isSquare());
    }

    @Test
    void unknownResolutionThrowsError() {
        try {
            ScreenSize.fromResolution("9999x9999");
            assertTrue(false, "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }
}
