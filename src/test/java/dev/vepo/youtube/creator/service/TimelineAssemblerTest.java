package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.shared.ProjectFixtures;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class TimelineAssemblerTest {

    @Inject
    TimelineAssembler timelineAssembler;

    @InjectMock
    MediaService mediaService;

    @BeforeEach
    void stubMaterialize() throws Exception {
        when(mediaService.materializeMedia(any())).thenReturn(Path.of("/tmp/test-video.mp4"));
    }

    @Test
    void assembleEmptyProjectCreatesTracksAndSettings() throws Exception {
        var project = ProjectFixtures.withVideoClip();
        project.getClips().clear();

        var timeline = timelineAssembler.assemble(project);

        assertEquals(2, timeline.getTracks().size());
        assertNotNull(timeline.getVideoSettings());
        assertEquals(1920, timeline.getVideoSettings().getWidth());
        assertFalse(timeline.hasContent());
    }

    @Test
    void assembleMapsClipTimingVolumeAndEffects() throws Exception {
        var timeline = timelineAssembler.assemble(ProjectFixtures.withEnrichedClip());

        MediaClip mediaClip = timeline.getVideoTrack().getClips().getFirst();
        assertEquals("/tmp/test-video.mp4", mediaClip.getFilePath());
        assertEquals(1.0, mediaClip.getTimelinePosition());
        assertEquals(0.5, mediaClip.getStartTime());
        assertEquals(5.5, mediaClip.getEndTime());
        assertEquals(1.25, mediaClip.getSpeed());
        assertEquals(0.8, mediaClip.getVolume(), 0.001);
        assertFalse(mediaClip.getEffects().isEmpty());
        assertNotNull(mediaClip.getClipTransition());
        assertEquals("dissolve", mediaClip.getClipTransition().getType());
        assertEquals(1, mediaClip.getVolumeKeyframes().size());
    }

    @Test
    void assembleSkipsClipWhenMediaMissing() throws Exception {
        var project = ProjectFixtures.withVideoClip();
        project.getClips().add(ProjectFixtures.clipMissingMedia());

        var timeline = timelineAssembler.assemble(project);

        assertEquals(1, timeline.getVideoTrack().getClips().size());
    }

    @Test
    void assembleFallsBackToFirstVideoTrackWhenIndexMissing() throws Exception {
        var project = ProjectFixtures.withVideoClip();
        project.getClips().getFirst().setTrackIndex(99);

        var timeline = timelineAssembler.assemble(project);

        assertEquals(1, timeline.getVideoTrack().getClips().size());
        assertTrue(timeline.getDuration() > 0);
    }
}
