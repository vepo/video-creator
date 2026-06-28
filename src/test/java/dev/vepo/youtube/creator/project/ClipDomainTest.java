package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class ClipDomainTest {

    @Test
    void clipStoresTimelineAndSourceRange() {
        var clip = new Clip();
        clip.setHash("abc");
        clip.setName("Intro");
        clip.setMediaHash("media-1");
        clip.setType(MediaType.VIDEO);
        clip.setStart(2_000);
        clip.setDuration(8_000);
        clip.setSourceIn(500);
        clip.setSourceOut(8_500);
        clip.setSpeed(0.5);
        clip.setVolume(75);
        clip.setTrackIndex(1);
        clip.setSyncGroup("sync-1");

        assertEquals("abc", clip.getHash());
        assertEquals(2_000, clip.getStart());
        assertEquals(8_000, clip.getDuration());
        assertEquals(0.5, clip.getSpeed());
        assertEquals(75, clip.getVolume());
        assertEquals("sync-1", clip.getSyncGroup());
    }

    @Test
    void clipTransitionStoresAlignment() {
        var transition = new ClipTransition("wipe", 300, "start");

        assertEquals("wipe", transition.getType());
        assertEquals(300, transition.getDurationMs());
        assertEquals("start", transition.getAlignment());
    }

    @Test
    void keyframeStoresTimeAndValue() {
        var keyframe = new Keyframe(1_500, 42.5);

        assertEquals(1_500, keyframe.getTimeMs());
        assertEquals(42.5, keyframe.getValue());
    }

    @Test
    void colorGradeDefaultsAreNeutral() {
        var grade = new ColorGrade();

        assertEquals(0, grade.getLift());
        assertEquals(1.0, grade.getGamma());
        assertEquals(1.0, grade.getGain());
        assertEquals(1.0, grade.getSaturation());
        assertNull(grade.getLutPath());
    }
}
