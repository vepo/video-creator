package dev.vepo.youtube.creator.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import dev.vepo.youtube.creator.AppConfig;
import dev.vepo.youtube.creator.model.VideoEditRequest;
import dev.vepo.youtube.creator.model.VideoSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VideoProcessingService {
    
    @Inject
    AppConfig appConfig;
    
    @Inject
    MLTXmlGenerator xmlGenerator;
    
    @Inject
    FileStorageService fileStorageService;
    
    public String processVideo(VideoEditRequest editRequest) throws IOException, InterruptedException {
        // Generate MLT XML
        String xmlPath = xmlGenerator.generateMLTXml(
            editRequest.getInputFile(),
            editRequest.getTrimOperations(),
            editRequest.getVideoSettings(),
            editRequest.getOutputFile()
        );
        
        // Build melt command
        List<String> command = buildMeltCommand(xmlPath, editRequest.getOutputFile(), editRequest.getVideoSettings());
        
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
            throw new RuntimeException("melt command failed with exit code " + exitCode + "\nOutput: " + output);
        }
        
        return editRequest.getOutputFile();
    }
    
    private List<String> buildMeltCommand(String xmlPath, String outputPath, VideoSettings settings) {
        List<String> command = new ArrayList<>();
        command.add(appConfig.meltCommand());
        command.add(xmlPath);
        command.add("-consumer");
        
        StringBuilder consumer = new StringBuilder("avformat:");
        consumer.append(outputPath);
        consumer.append(" vcodec=").append(settings.getVideoCodec());
        consumer.append(" acodec=").append(settings.getAudioCodec());
        consumer.append(" crf=").append(settings.getCrf());
        consumer.append(" preset=").append(settings.getPreset());
        
        if (settings.getAudioBitrate() != null) {
            consumer.append(" ab=").append(settings.getAudioBitrate()).append("k");
        }
        
        if (settings.getWidth() != null && settings.getHeight() != null) {
            consumer.append(" width=").append(settings.getWidth());
            consumer.append(" height=").append(settings.getHeight());
        }
        
        command.add(consumer.toString());
        
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
}