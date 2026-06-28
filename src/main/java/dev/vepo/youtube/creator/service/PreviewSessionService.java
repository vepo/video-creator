package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PreviewSessionService {
    private static final Logger logger = LoggerFactory.getLogger(PreviewSessionService.class);

    public record PreviewSession(String sessionId, String projectId, Path outputDir, Path manifestPath) {
    }

    private final Map<String, PreviewSession> sessions = new ConcurrentHashMap<>();

    @Inject
    Projects projects;

    @Inject
    TimelineAssembler timelineAssembler;

    @Inject
    VideoProcessingService videoProcessingService;

    @Inject
    MediaService mediaService;

    public PreviewSession startSession(String projectId) throws IOException, InterruptedException {
        stopSessionsForProject(projectId);
        var project = projects.find(projectId)
                              .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Path outputDir = mediaService.getOutputPath("preview_session_" + sessionId).getParent()
                .resolve("preview_session_" + sessionId);
        TimelineProject timeline = timelineAssembler.assemble(project);
        applyPreviewQuality(timeline);
        Path manifest = videoProcessingService.generateHlsPreview(timeline, outputDir);
        var session = new PreviewSession(sessionId, projectId, outputDir, manifest);
        sessions.put(sessionId, session);
        logger.info("Started preview session {} for project {}", sessionId, projectId);
        return session;
    }

    public PreviewSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void stopSession(String sessionId) {
        PreviewSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        deleteDir(session.outputDir());
    }

    public PreviewSession refreshSession(String sessionId) throws IOException, InterruptedException {
        PreviewSession existing = sessions.get(sessionId);
        if (existing == null) {
            throw new IllegalArgumentException("Session not found");
        }
        var project = projects.find(existing.projectId())
                              .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        deleteDir(existing.outputDir());
        TimelineProject timeline = timelineAssembler.assemble(project);
        applyPreviewQuality(timeline);
        Path manifest = videoProcessingService.generateHlsPreview(timeline, existing.outputDir());
        var refreshed = new PreviewSession(existing.sessionId(), existing.projectId(), existing.outputDir(), manifest);
        sessions.put(sessionId, refreshed);
        return refreshed;
    }

    private void stopSessionsForProject(String projectId) {
        sessions.values().stream()
                .filter(s -> projectId.equals(s.projectId()))
                .map(PreviewSession::sessionId)
                .toList()
                .forEach(this::stopSession);
    }

    private void deleteDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                     .sorted(java.util.Comparator.reverseOrder())
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             logger.warn("Could not delete {}", path);
                         }
                     });
            }
        } catch (IOException e) {
            logger.warn("Could not clean preview session dir {}", dir);
        }
    }

    private void applyPreviewQuality(TimelineProject timeline) {
        var settings = timeline.getVideoSettings();
        if (settings == null) {
            return;
        }
        settings.setCrf(28);
        settings.setPreset("ultrafast");
        int width = settings.getWidth() != null && settings.getWidth() > 0 ? settings.getWidth() : 1280;
        int height = settings.getHeight() != null && settings.getHeight() > 0 ? settings.getHeight() : 720;
        double scale = Math.min(1.0, Math.min(640.0 / width, 360.0 / height));
        settings.setWidth(Math.max(2, (int) Math.round(width * scale)));
        settings.setHeight(Math.max(2, (int) Math.round(height * scale)));
    }
}
