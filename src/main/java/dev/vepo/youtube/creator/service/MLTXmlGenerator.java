package dev.vepo.youtube.creator.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.vepo.youtube.creator.model.TrimOperation;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.MediaTrack;
import dev.vepo.youtube.creator.model.MediaClip;
import dev.vepo.youtube.creator.service.render.MltClipFilters;
import dev.vepo.youtube.creator.service.render.MltFrameRate;
import dev.vepo.youtube.creator.service.render.MltFrameRate.Rational;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MLTXmlGenerator {

    public String generateMLTXml(String inputVideoPath, List<TrimOperation> trimOperations,
                               VideoSettings settings, String outputPath) throws IOException {
        Path xmlPath = Files.createTempFile("mlt_project_", ".xml");
        Rational fps = profileRational(settings);

        try (BufferedWriter writer = Files.newBufferedWriter(xmlPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<mlt>\n");
            writeProfile(writer, settings, fps);

            writer.write("  <producer id=\"producer0\">\n");
            writer.write(String.format("    <property name=\"resource\">%s</property>\n",
                    java.nio.file.Paths.get(inputVideoPath).toAbsolutePath()));
            writer.write("  </producer>\n");

            writer.write("  <playlist id=\"playlist0\">\n");
            if (trimOperations != null && !trimOperations.isEmpty()) {
                for (TrimOperation trim : trimOperations) {
                    int startFrame = MltFrameRate.toFrameNumber(trim.getStartTime(), fps);
                    int endFrame = MltFrameRate.toFrameNumber(trim.getEndTime(), fps);
                    writer.write(String.format("    <entry producer=\"producer0\" in=\"%d\" out=\"%d\"/>\n",
                            startFrame, endFrame));
                }
            } else {
                writer.write("    <entry producer=\"producer0\"/>\n");
            }
            writer.write("  </playlist>\n");

            writer.write("  <tractor id=\"tractor0\">\n");
            writer.write("    <track producer=\"playlist0\"/>\n");
            writer.write("  </tractor>\n");
            writer.write("</mlt>");
        }

        return xmlPath.toString();
    }

    public String generateTimelineMLTXml(TimelineProject project) throws IOException {
        Path xmlPath = Files.createTempFile("timeline_project_", ".xml");
        VideoSettings settings = project.getVideoSettings();
        Rational fps = profileRational(settings);

        try (BufferedWriter writer = Files.newBufferedWriter(xmlPath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<mlt>\n");
            writeProfile(writer, settings, fps);

            int producerId = 0;
            int filterId = 0;

            for (MediaTrack track : project.getTracks()) {
                for (MediaClip clip : track.getClips()) {
                    writer.write(String.format("  <producer id=\"producer%d\">\n", producerId));
                    writer.write(String.format("    <property name=\"resource\">%s</property>\n",
                            java.nio.file.Paths.get(clip.getFilePath()).toAbsolutePath()));
                    writer.write("  </producer>\n");
                    MltClipFilters.writeFilters(writer, clip, filterId);
                    filterId += 10;
                    clip.setId("producer" + producerId);
                    producerId++;
                }
            }

            int playlistId = 0;
            for (MediaTrack track : project.getTracks()) {
                if (!track.hasClips()) {
                    continue;
                }
                writer.write(String.format("  <playlist id=\"playlist%d\">\n", playlistId));
                List<MediaClip> sortedClips = track.getClips().stream()
                        .sorted((a, b) -> Double.compare(a.getTimelinePosition(), b.getTimelinePosition()))
                        .toList();

                for (MediaClip clip : sortedClips) {
                    double speed = clip.getSpeed() > 0 ? clip.getSpeed() : 1.0;
                    int startFrame = MltFrameRate.toFrameNumber(clip.getStartTime(), fps);
                    int endFrame = MltFrameRate.toFrameNumber(clip.getEndTime(), fps);
                    int timelineFrame = MltFrameRate.toFrameNumber(clip.getTimelinePosition(), fps);

                    writer.write(String.format("    <entry producer=\"%s\" in=\"%d\" out=\"%d\"/>\n",
                            clip.getId(), startFrame, endFrame));
                    if (speed != 1.0) {
                        writer.write(String.format("    <property name=\"speed\">%.2f</property>\n", speed));
                    }
                    if (timelineFrame > 0) {
                        writer.write(String.format("    <property name=\"length\">%d</property>\n",
                                timelineFrame));
                    }
                }
                writer.write("  </playlist>\n");
                playlistId++;
            }

            writer.write("  <tractor id=\"tractor0\">\n");
            playlistId = 0;
            for (MediaTrack track : project.getTracks()) {
                if (track.hasClips()) {
                    writer.write(String.format("    <track producer=\"playlist%d\"/>\n", playlistId));
                    playlistId++;
                }
            }
            writer.write("  </tractor>\n");
            writer.write("</mlt>");
        }

        return xmlPath.toString();
    }

    private static Rational profileRational(VideoSettings settings) {
        if (settings != null && settings.getFrameRateNum() != null && settings.getFrameRateDen() != null) {
            return new Rational(settings.getFrameRateNum(), settings.getFrameRateDen());
        }
        return new Rational(30, 1);
    }

    private static void writeProfile(BufferedWriter writer, VideoSettings settings, Rational fps) throws IOException {
        int width = settings != null && settings.getWidth() != null && settings.getWidth() > 0
                ? settings.getWidth() : 1920;
        int height = settings != null && settings.getHeight() != null && settings.getHeight() > 0
                ? settings.getHeight() : 1080;
        writer.write(String.format(
                "  <profile width=\"%d\" height=\"%d\" frame_rate_num=\"%d\" frame_rate_den=\"%d\" progressive=\"1\"/>\n",
                width, height, fps.numerator(), fps.denominator()));
    }
}
