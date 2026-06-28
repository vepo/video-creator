package dev.vepo.youtube.creator.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.AppConfig;
import dev.vepo.youtube.creator.project.Media;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MediaAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(MediaAnalysisService.class);

    @Inject
    AppConfig appConfig;

    @Inject
    MediaService mediaService;

    public record AnalysisResult(String thumbnailPath, String waveformPath) {
    }

    public AnalysisResult analyze(Media media) throws IOException {
        Path source = mediaService.materializeMedia(media);
        Path thumbDir = mediaService.getOutputPath("analysis").getParent().resolve("analysis");
        Files.createDirectories(thumbDir);
        String baseName = media.getHash();
        Path thumbnail = thumbDir.resolve(baseName + "_thumb.jpg");
        Path waveform = thumbDir.resolve(baseName + "_waveform.png");
        generateThumbnail(source, thumbnail);
        generateWaveform(source, waveform);
        return new AnalysisResult(
                thumbnail.toString(),
                waveform.toString());
    }

    public Path getThumbnailPath(String mediaHash) {
        Path thumbDir = mediaService.getOutputPath("analysis").getParent().resolve("analysis");
        return thumbDir.resolve(mediaHash + "_thumb.jpg");
    }

    public Path getWaveformPath(String mediaHash) {
        Path thumbDir = mediaService.getOutputPath("analysis").getParent().resolve("analysis");
        return thumbDir.resolve(mediaHash + "_waveform.png");
    }

    public void generateThumbnail(Path source, Path output) throws IOException {
        if (Files.exists(output)) {
            return;
        }
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(source.toAbsolutePath().toString());
        command.add("-ss");
        command.add("1");
        command.add("-vframes");
        command.add("1");
        command.add("-vf");
        command.add("scale=160:-1");
        command.add(output.toAbsolutePath().toString());
        runProcess(command, "thumbnail");
    }

    public void generateWaveform(Path source, Path output) throws IOException {
        if (Files.exists(output)) {
            return;
        }
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(source.toAbsolutePath().toString());
        command.add("-filter_complex");
        command.add("showwavespic=s=640x80:colors=0x3daee9");
        command.add("-frames:v");
        command.add("1");
        command.add(output.toAbsolutePath().toString());
        runProcess(command, "waveform");
    }

    private void runProcess(List<String> command, String label) throws IOException {
        logger.info("Running {} analysis: {}", label, command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("ffmpeg-{}: {}", label, line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("ffmpeg " + label + " failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ffmpeg " + label + " interrupted", e);
        }
    }
}
