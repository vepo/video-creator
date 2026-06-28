package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PreviewSessionService {
    private static final Logger logger = LoggerFactory.getLogger(PreviewSessionService.class);

    public record PreviewSession(String sessionId, String projectId, Path outputDir, Path manifestPath) {
    }

    public record PreviewSessionStatus(
            String sessionId,
            String projectId,
            String status,
            String manifestUrl,
            int percent,
            Integer etaSeconds,
            String error) {
    }

    private static final class SessionState {
        final String sessionId;
        final String projectId;
        final Path outputDir;
        volatile Path manifestPath;
        volatile String phase = "rendering";
        volatile int percent;
        volatile Integer etaSeconds;
        volatile String errorMessage;
        volatile long renderStartedAt = System.currentTimeMillis();

        SessionState(String sessionId, String projectId, Path outputDir) {
            this.sessionId = sessionId;
            this.projectId = projectId;
            this.outputDir = outputDir;
        }
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "preview-session-worker");
        t.setDaemon(true);
        return t;
    });

    @Inject
    Projects projects;

    @Inject
    TimelineAssembler timelineAssembler;

    @Inject
    VideoProcessingService videoProcessingService;

    @Inject
    MediaService mediaService;

    public PreviewSessionStatus startSession(String projectId) {
        stopSessionsForProject(projectId);
        projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Path outputDir = mediaService.getOutputPath("preview_session_" + sessionId).getParent()
                .resolve("preview_session_" + sessionId);
        var state = new SessionState(sessionId, projectId, outputDir);
        sessions.put(sessionId, state);
        executor.submit(() -> renderSession(state));
        logger.info("Queued preview session {} for project {}", sessionId, projectId);
        return toStatus(state);
    }

    public PreviewSessionStatus getSessionStatus(String sessionId) {
        SessionState state = sessions.get(sessionId);
        return state == null ? null : toStatus(state);
    }

    public PreviewSession getSession(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null || !"ready".equals(state.phase) || state.manifestPath == null) {
            return null;
        }
        return new PreviewSession(state.sessionId, state.projectId, state.outputDir, state.manifestPath);
    }

    public void stopSession(String sessionId) {
        SessionState session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        deleteDir(session.outputDir);
    }

    public PreviewSession refreshSession(String sessionId) throws IOException, InterruptedException {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            throw new IllegalArgumentException("Session not found");
        }
        state.phase = "rendering";
        state.percent = 0;
        state.etaSeconds = null;
        state.errorMessage = null;
        state.renderStartedAt = System.currentTimeMillis();
        renderSession(state);
        if ("failed".equals(state.phase)) {
            throw new RuntimeException(state.errorMessage != null ? state.errorMessage : "Preview refresh failed");
        }
        return getSession(sessionId);
    }

    private void renderSession(SessionState state) {
        state.phase = "rendering";
        state.percent = 0;
        state.etaSeconds = null;
        state.errorMessage = null;
        state.renderStartedAt = System.currentTimeMillis();
        try {
            var project = projects.find(state.projectId)
                                  .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            deleteDir(state.outputDir);
            Files.createDirectories(state.outputDir);
            TimelineProject timeline = timelineAssembler.assemble(project);
            applyPreviewQuality(timeline);
            Path manifest = videoProcessingService.generateHlsPreview(
                    timeline,
                    state.outputDir,
                    percent -> updateProgress(state, percent));
            state.manifestPath = manifest;
            state.percent = 100;
            state.etaSeconds = 0;
            state.phase = "ready";
            logger.info("Preview session {} ready", state.sessionId);
        } catch (Exception e) {
            logger.error("Preview session {} failed", state.sessionId, e);
            state.phase = "failed";
            state.errorMessage = e.getMessage();
        }
    }

    private void updateProgress(SessionState state, int percent) {
        state.percent = Math.min(100, Math.max(0, percent));
        if (percent > 0 && percent < 100) {
            long elapsedMs = System.currentTimeMillis() - state.renderStartedAt;
            state.etaSeconds = (int) Math.round(elapsedMs * (100.0 - percent) / percent / 1000.0);
        } else if (percent >= 100) {
            state.etaSeconds = 0;
        }
    }

    private PreviewSessionStatus toStatus(SessionState state) {
        String manifestUrl = "ready".equals(state.phase) && state.manifestPath != null
                ? "/preview/" + state.sessionId + "/index.m3u8"
                : null;
        return new PreviewSessionStatus(
                state.sessionId,
                state.projectId,
                state.phase,
                manifestUrl,
                state.percent,
                state.etaSeconds,
                state.errorMessage);
    }

    private void stopSessionsForProject(String projectId) {
        sessions.values().stream()
                .filter(s -> projectId.equals(s.projectId))
                .map(s -> s.sessionId)
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

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
