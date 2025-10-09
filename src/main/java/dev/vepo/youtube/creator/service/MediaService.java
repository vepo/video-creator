package dev.vepo.youtube.creator.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import dev.vepo.youtube.creator.AppConfig;
import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MediaService {
    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    private static long retrieveDuration(Path content, String mimeType) {
        return switch (MediaType.load(mimeType)) {
            case AUDIO -> retrieveAudioDuration(content);
            case VIDEO -> retrieveVideoDuration(content);
            default -> -1l;
        };
    }

    private static long retrieveAudioDuration(Path audioPath) {
        try {
            String audioAbsolutePath = audioPath.toAbsolutePath().toString();

            // Build the soxi command
            String[] command = { "soxi", "-D", audioAbsolutePath };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = readProcessOutput(process);
            logger.info("Audio duration extraction: {}", output);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Could not read audio duration! {}", readProcessError(process));
                return -1;
            }

            return parseSecondsToMillis(output.trim());
        } catch (IOException e) {
            logger.error("Could not read audio duration!", e);
            return -1l;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1l;
        }
    }

    private static long parseSecondsToMillis(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return -1;
        }

        try {
            // soxi returns duration in seconds as floating point: "0.809002"
            double seconds = Double.parseDouble(durationStr);
            return Math.round(seconds * 1000); // Convert to milliseconds
        } catch (NumberFormatException e) {
            System.err.println("Error parsing soxi duration: " + durationStr);
            return -1;
        }
    }

    private static long retrieveVideoDuration(Path videoPath) {
        try {
            String videoAbsolutePath = videoPath.toAbsolutePath().toString();

            // Build the shell command
            String[] command = {
                    "/bin/sh", "-c",
                    String.format("ffmpeg -i \"%s\" 2>&1 | grep Duration | awk '{print $2}' | tr -d ,",
                            videoAbsolutePath)
            };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = readProcessOutput(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Could not read video duration! {}", readProcessError(process));
                return -1;
            }

            return parseDurationToMillis(output.trim());
        } catch (IOException e) {
            logger.error("Could not read video duration!", e);
            return -1l;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1l;
        }
    }

    private static String readProcessError(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }

    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString();
    }

    private static long parseDurationToMillis(String durationStr) {
        if (durationStr == null || durationStr.isEmpty() || durationStr.equals("N/A")) {
            return -1;
        }

        try {
            // Format: HH:MM:SS.millis or HH:MM:SS
            String[] parts = durationStr.split(":");
            if (parts.length < 3) {
                return -1;
            }

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            // Handle seconds and milliseconds
            String[] secondsParts = parts[2].split("\\.");
            int seconds = Integer.parseInt(secondsParts[0]);
            int millis = 0;

            if (secondsParts.length > 1) {
                // Handle fractional seconds (e.g., 10.03 -> 30ms)
                String fractional = secondsParts[1];
                if (fractional.length() > 3) {
                    fractional = fractional.substring(0, 3);
                } else if (fractional.length() == 2) {
                    fractional += "0"; // 10.03 -> 030ms
                } else if (fractional.length() == 1) {
                    fractional += "00"; // 10.0 -> 000ms
                }
                millis = Integer.parseInt(fractional);
            }

            return TimeUnit.HOURS.toMillis(hours) +
                    TimeUnit.MINUTES.toMillis(minutes) +
                    TimeUnit.SECONDS.toMillis(seconds) +
                    millis;

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String retrieveMimeType(Path resource) throws IOException {
        var process = new ProcessBuilder("file", "-i", resource.toAbsolutePath().toString()).start();
        try {
            int exitCode = process.waitFor();
            logger.info("Exit code: {}", exitCode);
            if (exitCode == 0) {
                var output = readProcessOutput(process);
                var parts = output.split(":");
                if (parts.length == 2) {
                    String mediaInfo = parts[1].trim();
                    String[] mediaParts = mediaInfo.split(";");
                    return mediaParts[0].trim();
                }
            } else {
                logger.error("Could not retrieve mime type: %s", readProcessError(process));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private static String retrieveHash(Path resource) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream is = Files.newInputStream(resource)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = md.digest();
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "mongodb.database")
    String databaseName;

    @ConfigProperty(name = "app.media.chunk-size", defaultValue = "1048576")
    Integer mediaChunkSize;

    @Inject
    AppConfig appConfig;

    public Media store(Path content, String filename) throws IOException {
        var uploadAt = Instant.now();
        String hash = retrieveHash(content);
        var options = new GridFSUploadOptions().chunkSizeBytes(mediaChunkSize)
                .metadata(new Document("filename", filename))
                .metadata(new Document("uploaded-at", uploadAt.toEpochMilli()))
                .metadata(new Document("hash", hash));
        String mimeType = retrieveMimeType(content);
        long duration = retrieveDuration(content, mimeType);
        if (Objects.nonNull(mimeType)) {
            options.metadata(new Document("mime-type", mimeType));
        }
        return new Media(getBucket().uploadFromStream(filename, new FileInputStream(content.toFile()), options),
                filename, hash, mimeType, duration, uploadAt);
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

    private GridFSBucket getBucket() {
        return GridFSBuckets.create(mongoClient.getDatabase(databaseName), "media");
    }
}