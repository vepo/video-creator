package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class FrameRateTest {

    @Test
    void defaultFrameRateIs30FpsWebStreaming() {
        var frameRate = FrameRate.getDefault();

        assertEquals(30.0, frameRate.getValue());
        assertEquals("Web & Streaming", frameRate.getCategory());
    }

    @Test
    void frameRateLookupByValueString() {
        var frameRate = FrameRate.fromValueString("24.0");

        assertTrue(frameRate.isFilmCinema());
    }

    @Test
    void highFrameRateSupportsSlowMotion() {
        var frameRate = FrameRate.FPS_120;

        assertTrue(frameRate.isSlowMotionCapable());
    }
}
