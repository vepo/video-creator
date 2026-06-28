package dev.vepo.youtube.creator.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.AppConfig;
import dev.vepo.youtube.creator.model.VideoEditRequest;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.model.TimelineProject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VideoProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);
    
    @Inject
    AppConfig appConfig;
    
    @Inject
    MLTXmlGenerator xmlGenerator;
    
    @Inject
    MediaService fileStorageService;
    
    public String processVideo(VideoEditRequest editRequest) throws IOException, InterruptedException {
        // Generate MLT XML
        String xmlPath = xmlGenerator.generateMLTXml(
            editRequest.getInputFile(),
            editRequest.getTrimOperations(),
            editRequest.getVideoSettings(),
            editRequest.getOutputFile()
        );
        
        // Build melt command
        var command = buildMeltCommand(xmlPath, editRequest.getOutputFile(), editRequest.getVideoSettings());
        
        // Log the command for debugging
        logger.info("Executing melt command: {}", command);
        
        // Execute melt command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.info("melt: {}", line); // Log melt output
            }
        }
        
        int exitCode = process.waitFor();
        
        // Cleanup XML file
        fileStorageService.cleanupFile(xmlPath);
        
        if (exitCode != 0) {
            throw new RuntimeException("melt command failed with exit code " + exitCode + "\nOutput: " + output);
        }
        
        return editRequest.getOutputFile();
    }
    
    private List<String> buildMeltCommand(String xmlPath, String outputPath, VideoSettings settings) {
        List<String> command = new ArrayList<>();
        command.add(appConfig.meltCommand());
        command.add(xmlPath);
        command.add("-consumer");
        
        // Build consumer with proper parameter separation
        String absoluteOutputPath = java.nio.file.Paths.get(outputPath).toAbsolutePath().toString();
        command.add("avformat:" + absoluteOutputPath);
        command.add("vcodec=" + settings.getVideoCodec());
        command.add("acodec=" + settings.getAudioCodec());
        command.add("crf=" + settings.getCrf());
        command.add("preset=" + settings.getPreset());
        
        if (settings.getAudioBitrate() != null) {
            command.add("ab=" + settings.getAudioBitrate() + "k");
        }
        
        if (settings.getWidth() != null && settings.getHeight() != null) {
            command.add("width=" + settings.getWidth());
            command.add("height=" + settings.getHeight());
        }
        
        // Add proper file format extension handling
        if (!outputPath.endsWith(".mp4") && !outputPath.endsWith(".mkv") && !outputPath.endsWith(".avi")) {
            command.add("f=mp4");
        }
        
        return command;
    }
    
    public boolean isMeltAvailable() {
        try {
            Process process = new ProcessBuilder(appConfig.meltCommand(), "--version").start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    public String processTimelineProject(TimelineProject project, String outputPath) throws IOException, InterruptedException {
        // Generate MLT XML for timeline project
        String xmlPath = xmlGenerator.generateTimelineMLTXml(project);
        
        // Build melt command
        List<String> command = buildMeltCommand(xmlPath, outputPath, project.getVideoSettings());
        
        // Log the command for debugging
        logger.info("Executing timeline melt command: {}", command);
        
        // Execute melt command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.info("melt: {}", line); // Log melt output
            }
        }
        
        int exitCode = process.waitFor();
        
        // Cleanup XML file
        fileStorageService.cleanupFile(xmlPath);
        
        if (exitCode != 0) {
            throw new RuntimeException("melt command failed with exit code " + exitCode + "\nOutput: " + output);
        }
        
        return outputPath;
    }
    
    public String generatePreview(TimelineProject project) throws IOException, InterruptedException {
        return generatePreview(project, 0, 0);
    }

    public String generatePreview(TimelineProject project, double startTimeSeconds, double durationSeconds)
            throws IOException, InterruptedException {
        String previewFilename = "preview_%d.mp4".formatted(System.currentTimeMillis());
        String previewPath = fileStorageService.getOutputPath(previewFilename).toString();

        String xmlPath = xmlGenerator.generateTimelineMLTXml(project);

        VideoSettings settings = project.getVideoSettings();
        var command = new ArrayList<String>();
        command.add(appConfig.meltCommand());
        command.add(xmlPath);
        command.add("-consumer");
        command.add("avformat:%s".formatted(Paths.get(previewPath).toAbsolutePath().toString()));
        command.add("vcodec=libx264");
        command.add("acodec=aac");
        command.add("crf=28");
        command.add("preset=ultrafast");
        command.add("format=mp4");
        command.add("movflags=faststart");

        if (settings != null) {
            if (settings.getWidth() != null && settings.getHeight() != null) {
                command.add("width=" + settings.getWidth());
                command.add("height=" + settings.getHeight());
            }
            if (settings.getAudioBitrate() != null) {
                command.add("ab=" + settings.getAudioBitrate() + "k");
            }
        }

        // Optional trim — frame numbers at 60 fps (matches MLT XML generation)
        if (startTimeSeconds > 0) {
            command.add("in=" + (int) (startTimeSeconds * 60));
        }
        if (durationSeconds > 0) {
            double endSeconds = startTimeSeconds + durationSeconds;
            command.add("out=" + (int) (endSeconds * 60));
        }

        logger.info("Executing preview melt command: {}", command);
        
        // Execute melt command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        // Cleanup XML file
        fileStorageService.cleanupFile(xmlPath);
        
        if (exitCode != 0) {
            throw new RuntimeException("Preview generation failed with exit code " + exitCode + "\nOutput: " + output);
        }
        
        return previewPath;
    }

    public java.nio.file.Path generateHlsPreview(TimelineProject project, java.nio.file.Path outputDir)
            throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        java.nio.file.Path manifest = outputDir.resolve("index.m3u8");
        String xmlPath = xmlGenerator.generateTimelineMLTXml(project);
        VideoSettings settings = project.getVideoSettings();

        var command = new ArrayList<String>();
        command.add(appConfig.meltCommand());
        command.add(xmlPath);
        command.add("-consumer");
        command.add("avformat:" + manifest.toAbsolutePath());
        command.add("vcodec=libx264");
        command.add("acodec=aac");
        command.add("crf=28");
        command.add("preset=ultrafast");
        command.add("f=hls");
        command.add("hls_time=2");
        command.add("hls_list_size=0");
        command.add("hls_flags=delete_segments+append_list");
        if (settings != null && settings.getWidth() != null && settings.getHeight() != null) {
            command.add("width=" + settings.getWidth());
            command.add("height=" + settings.getHeight());
        }

        logger.info("Executing HLS preview command: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("melt-hls: {}", line);
            }
        }
        int exitCode = process.waitFor();
        fileStorageService.cleanupFile(xmlPath);
        if (exitCode != 0) {
            throw new RuntimeException("HLS preview generation failed with exit code " + exitCode);
        }
        return manifest;
    }
}