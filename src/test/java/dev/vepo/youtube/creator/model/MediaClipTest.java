package dev.vepo.youtube.creator.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MediaClipTest {

    @Test
    void effectiveDurationAccountsForSpeed() {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(10);
        clip.setSpeed(2.0);

        assertEquals(5.0, clip.getEffectiveDuration(), 0.001);
    }

    @Test
    void validClipPassesValidation() {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");

        assertTrue(clip.isValid());
    }
}
