package dev.vepo.youtube.creator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.service.MLTXmlGenerator;

class MLTXmlGeneratorTest {

    @Test
    void timelineXmlUsesProjectFrameRateAndEffectFilter() throws Exception {
        var generator = new MLTXmlGenerator();
        var project = new TimelineProject();
        var settings = new VideoSettings();
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setFrameRateNum(24);
        settings.setFrameRateDen(1);
        project.setVideoSettings(settings);

        var track = new MediaTrack(0, "Video 1", "video");
        var clip = new MediaClip("c1", "/tmp/sample.mp4", "sample.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(2);
        clip.setTimelinePosition(0);
        clip.setEffect("grayscale");
        track.addClip(clip);
        project.getTracks().clear();
        project.addTrack(track);

        String xmlPath = generator.generateTimelineMLTXml(project);
        String xml = java.nio.file.Files.readString(java.nio.file.Path.of(xmlPath));

        assertTrue(xml.contains("frame_rate_num=\"24\""));
        assertTrue(xml.contains("frame_rate_den=\"1\""));
        assertTrue(xml.contains("greyscale"));
    }
}
