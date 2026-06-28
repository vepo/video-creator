package dev.vepo.youtube.creator.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class TimelineProjectTest {

    @Test
    void newTimelineHasVideoAndAudioTracks() {
        var project = new TimelineProject();

        assertEquals(2, project.getTracks().size());
        assertNotNull(project.getVideoTrack());
        assertFalse(project.getAudioTracks().isEmpty());
    }

    @Test
    void emptyTimelineHasNoContent() {
        var project = new TimelineProject();

        assertFalse(project.hasContent());
    }

    @Test
    void durationRecalculatesFromClips() {
        var project = new TimelineProject();
        var clip = new MediaClip("clip-1", "/tmp/video.mp4", "video.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(10);
        clip.setTimelinePosition(0);
        project.getVideoTrack().addClip(clip);

        project.updateDuration();

        assertTrue(project.getDuration() > 0);
    }
}
