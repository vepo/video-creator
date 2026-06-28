package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MLTXmlGeneratorTest {

    private MLTXmlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MLTXmlGenerator();
    }

    @Test
    void generatesMltXmlWithCustomDimensionsAndNoTrims() throws Exception {
        var settings = new VideoSettings();
        settings.setWidth(1280);
        settings.setHeight(720);
        prepareInputVideo("target/test-input/sample.mp4");

        var xmlPath = generator.generateMLTXml(
                "target/test-input/sample.mp4",
                null,
                settings,
                "target/test-output/out.mp4");
        var xml = Files.readString(Path.of(xmlPath));

        assertTrue(xml.contains("width=\"1280\""));
        assertTrue(xml.contains("<entry producer=\"producer0\"/>"));
    }

    @Test
    void generatesMltXmlWithTrimOperations() throws Exception {
        var settings = new VideoSettings();
        settings.setWidth(1920);
        settings.setHeight(1080);
        prepareInputVideo("target/test-input/sample.mp4");

        var xmlPath = generator.generateMLTXml(
                "target/test-input/sample.mp4",
                List.of(new TrimOperation(1.0, 5.0)),
                settings,
                "target/test-output/out.mp4");
        var xml = Files.readString(Path.of(xmlPath));

        assertTrue(xml.contains("in=\"30\" out=\"150\""));
    }

    @Test
    void generatesMltXmlForTimelineWithClips() throws Exception {
        prepareInputVideo("target/test-input/sample.mp4");
        var project = timelineWithVideoClip();

        var xmlPath = generator.generateTimelineMLTXml(project);
        var xml = Files.readString(Path.of(xmlPath));

        assertTrue(xml.contains("<producer id=\"producer"));
        assertTrue(xml.contains("<property name=\"resource\">"));
        assertTrue(xml.contains("<entry producer=\"producer"));
    }

    @Test
    void timelineXmlUsesProjectFrameRateAndEffectFilter() throws Exception {
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

        var xmlPath = generator.generateTimelineMLTXml(project);
        var xml = Files.readString(Path.of(xmlPath));

        assertTrue(xml.contains("frame_rate_num=\"24\""));
        assertTrue(xml.contains("frame_rate_den=\"1\""));
        assertTrue(xml.contains("greyscale"));
    }

    private static TimelineProject timelineWithVideoClip() {
        var project = new TimelineProject();
        var clip = new MediaClip("clip-1", "target/test-input/sample.mp4", "sample.mp4", "video");
        clip.setStartTime(0);
        clip.setEndTime(5);
        clip.setTimelinePosition(0);
        project.getVideoTrack().addClip(clip);
        return project;
    }

    private static void prepareInputVideo(String path) throws Exception {
        var videoPath = Path.of(path);
        Files.createDirectories(videoPath.getParent());
        if (!Files.exists(videoPath)) {
            Files.writeString(videoPath, "placeholder");
        }
    }
}
