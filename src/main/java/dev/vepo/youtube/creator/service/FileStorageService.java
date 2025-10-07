package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import dev.vepo.youtube.creator.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FileStorageService {
    
    @Inject
    AppConfig appConfig;
    
    public String storeUploadedFile(byte[] content, String originalFilename) throws IOException {
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
        Path uploadPath = Paths.get(appConfig.uploadDir());
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.write(filePath, content);
        
        return filePath.toString();
    }
    
    public Path getOutputPath(String filename) {
        Path outputPath = Paths.get(appConfig.outputDir());
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create output directory", e);
        }
        return outputPath.resolve(filename);
    }
    
    public Path createTempFile(String prefix, String suffix) throws IOException {
        Path tempPath = Paths.get(appConfig.tempDir());
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }
        return Files.createTempFile(tempPath, prefix, suffix);
    }
    
    public void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // Log warning but don't throw
            System.err.println("Warning: Could not delete file: " + filePath);
        }
    }
}