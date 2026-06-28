package dev.vepo.youtube.creator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.project.RenderJob;
import dev.vepo.youtube.creator.project.RenderJob.Status;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RenderJobService {
    private static final Logger logger = LoggerFactory.getLogger(RenderJobService.class);

    private final Map<String, RenderJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "render-job-worker");
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

    public RenderJob enqueue(String projectId, String format, String quality) {
        projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        var job = new RenderJob();
        job.setId(UUID.randomUUID().toString().replace("-", ""));
        job.setProjectId(projectId);
        job.setFormat(format != null ? format : "mp4");
        job.setQuality(quality != null ? quality : "high");
        job.setStatus(Status.QUEUED);
        jobs.put(job.getId(), job);
        executor.submit(() -> runJob(job));
        return job;
    }

    public RenderJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public List<RenderJob> listJobsForProject(String projectId) {
        return jobs.values().stream()
                .filter(j -> projectId.equals(j.getProjectId()))
                .toList();
    }

    public List<RenderJob> listAllJobs() {
        return new ArrayList<>(jobs.values());
    }

    private void runJob(RenderJob job) {
        job.setStatus(Status.RUNNING);
        try {
            Project project = projects.find(job.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            TimelineProject timeline = timelineAssembler.assemble(project);
            applyExportQuality(timeline.getVideoSettings(), job.getQuality());
            applyExportFormat(timeline.getVideoSettings(), job.getFormat());
            String ext = extensionForFormat(job.getFormat());
            String outputFilename = "rendered_" + job.getId() + "." + ext;
            String outputPath = mediaService.getOutputPath(outputFilename).toString();
            videoProcessingService.processTimelineProject(timeline, outputPath);
            job.setOutputFilename(outputFilename);
            job.setDownloadUrl("/download/" + outputFilename);
            job.setStatus(Status.COMPLETED);
            job.setCompletedAt(Instant.now());
        } catch (Exception e) {
            logger.error("Render job {} failed", job.getId(), e);
            job.setStatus(Status.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private void applyExportQuality(VideoSettings settings, String quality) {
        if (settings == null) {
            return;
        }
        switch (quality == null ? "high" : quality.toLowerCase()) {
            case "low" -> {
                settings.setCrf(28);
                settings.setPreset("ultrafast");
            }
            case "medium" -> {
                settings.setCrf(23);
                settings.setPreset("medium");
            }
            default -> {
                settings.setCrf(18);
                settings.setPreset("slow");
            }
        }
    }

    private void applyExportFormat(VideoSettings settings, String format) {
        if (settings == null) {
            return;
        }
        switch (format == null ? "mp4" : format.toLowerCase()) {
            case "webm" -> {
                settings.setVideoCodec("libvpx-vp9");
                settings.setAudioCodec("libopus");
            }
            case "mov" -> {
                settings.setVideoCodec("libx264");
                settings.setAudioCodec("aac");
            }
            default -> {
                settings.setVideoCodec("libx264");
                settings.setAudioCodec("aac");
            }
        }
    }

    private String extensionForFormat(String format) {
        if (format == null) {
            return "mp4";
        }
        return switch (format.toLowerCase()) {
            case "webm" -> "webm";
            case "mov" -> "mov";
            default -> "mp4";
        };
    }
}
