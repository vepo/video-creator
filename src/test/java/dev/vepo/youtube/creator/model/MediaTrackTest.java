package dev.vepo.youtube.creator.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MediaTrackTest {

    private MediaTrack track;

    @BeforeEach
    void setUp() {
        track = new MediaTrack(0, "Track 0", "video");
    }

    @Test
    void emptyTrackHasNoClips() {
        assertFalse(track.hasClips());
    }

    @Test
    void addedClipReceivesTrackIndex() {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        track.addClip(clip);

        assertEquals(0, clip.getTrackIndex());
    }

    @Test
    void trackTotalDurationReflectsClipSpan() {
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(5);
        clip.setTimelinePosition(2);
        track.addClip(clip);

        assertEquals(7.0, track.getTotalDuration(), 0.001);
    }
}
