package dev.vepo.youtube.creator;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.infra.SafePaths;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.project.RenderJob;
import dev.vepo.youtube.creator.service.EdlExportService;
import dev.vepo.youtube.creator.service.MediaAnalysisService;
import dev.vepo.youtube.creator.service.MediaService;
import dev.vepo.youtube.creator.service.OtioExportService;
import dev.vepo.youtube.creator.service.PluginRegistry;
import dev.vepo.youtube.creator.service.PreviewSessionService;
import dev.vepo.youtube.creator.service.ProjectArchiveService;
import dev.vepo.youtube.creator.service.ProjectTemplateService;
import dev.vepo.youtube.creator.service.RenderJobService;
import dev.vepo.youtube.creator.service.ShareLinkService;
import dev.vepo.youtube.creator.service.TimelineAssembler;
import dev.vepo.youtube.creator.service.VideoProcessingService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class VideoEditorController {
    private static final Logger logger = LoggerFactory.getLogger(VideoEditorController.class);

    @Inject
    @Location("index.html")
    Template index;

    @Inject
    @Location("editor.html")
    Template editor;

    @Inject
    VideoProcessingService videoProcessingService;

    @Inject
    MediaService fileStorageService;

    @Inject
    Projects projects;

    @Inject
    TimelineAssembler timelineAssembler;

    @Inject
    PreviewSessionService previewSessionService;

    @Inject
    RenderJobService renderJobService;

    @Inject
    MediaAnalysisService mediaAnalysisService;

    @Inject
    ProjectTemplateService projectTemplateService;

    @Inject
    ProjectArchiveService projectArchiveService;

    @Inject
    OtioExportService otioExportService;

    @Inject
    EdlExportService edlExportService;

    @Inject
    PluginRegistry pluginRegistry;

    @Inject
    ShareLinkService shareLinkService;

    public record UploadResponse(String fileId, String fileName, String filePath, String message) {}
    public record ErrorResponse(String error) {}
    public record PreviewSessionResponse(String sessionId, String status, String manifestUrl, int percent,
            Integer etaSeconds, String error) {}
    public record RenderResponse(String outputFilename, String downloadUrl, String message) {}
    public record RenderRequest(String format, String quality) {}
    public record RenderQueueRequest(String format, String quality) {}
    public record MediaRenameRequest(String name) {}
    public record ProjectMetadataRequest(String name, String description) {}

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home() {
        boolean dbAvailable = true;
        java.util.List<Project> projectList;
        try {
            projectList = projects.loadAll();
        } catch (Exception e) {
            logger.error("Failed to connect to database", e);
            dbAvailable = false;
            projectList = java.util.Collections.emptyList();
        }
        return index.data("meltAvailable", videoProcessingService.isMeltAvailable(),
                         "dbAvailable", dbAvailable,
                         "appVersion", "1.0.0",
                         "projects", projectList);
    }

    @GET
    @Path("/download/{filename}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM, "video/mp4"})
    public Response downloadFile(@PathParam("filename") String filename) {
        try {
            var filePath = fileStorageService.getOutputPath(filename);
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            var safeFilename = SafePaths.safeBasename(filename);
            var disposition = safeFilename.startsWith("preview_")
                    ? "inline; filename=\"" + safeFilename + "\""
                    : "attachment; filename=\"" + safeFilename + "\"";
            return Response.ok(filePath.toFile())
                    .header("Content-Disposition", disposition)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/editor/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public Response editorPage(@PathParam("projectId") String projectId) {
        if (Objects.isNull(projectId) || projectId.isBlank() || projectId.equalsIgnoreCase("new")) {
            var project = projects.newProject();
            return Response.seeOther(URI.create("/editor/%s".formatted(project.getId().toHexString())))
                           .build();
        }
        var project = projects.find(projectId);
        if (project.isEmpty()) {
            return Response.seeOther(URI.create("/")).build();
        }
        project.get().ensureTracks();
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        return Response.ok()
                       .entity(editor.data("meltAvailable", meltAvailable,
                                           "project", project.get())
                                     .render())
                       .build();
    }

    @PUT
    @Path("/api/editor/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveProject(@PathParam("projectId") String projectId, Project body) {
        var existing = projects.find(projectId)
                               .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        body.setId(existing.getId());
        body.ensureTracks();
        if (body.getMedias() == null) {
            body.setMedias(existing.getMedias());
        }
        if (body.getCreatedAt() == null) {
            body.setCreatedAt(existing.getCreatedAt());
        }
        projects.update(body);
        return Response.ok(body).build();
    }

    @DELETE
    @Path("/api/editor/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProject(@PathParam("projectId") String projectId) {
        var existing = projects.find(projectId);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Project not found"))
                           .build();
        }
        if (!projects.delete(projectId)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to delete project"))
                           .build();
        }
        return Response.ok(new ErrorResponse("Project deleted")).build();
    }

    @PUT
    @Path("/api/projects/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProjectMetadata(@PathParam("projectId") String projectId,
                                          ProjectMetadataRequest body) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        if (body.name() != null && !body.name().isBlank()) {
            project.setName(body.name().trim());
        }
        if (body.description() != null) {
            project.setDescription(body.description());
        }
        projects.update(project);
        return Response.ok(project).build();
    }

    @POST
    @Path("/api/editor/{projectId}/media")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(@PathParam("projectId") String projectId,
                               @FormParam("name") String name,
                               @RestForm("file") FileUpload file) {
        logger.info("Processing media upload");
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var tempDir = fileStorageService.createTempDirectory("upload");
            var safeFilename = SafePaths.safeBasename(file.fileName());
            var tempFile = tempDir.resolve(safeFilename);
            Files.copy(file.uploadedFile(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            var media = fileStorageService.store(tempFile, safeFilename);
            logger.info("Media upload completed");
            project.getMedias().add(media);
            projects.update(project);
            return Response.ok()
                           .entity(media)
                           .build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to upload file: %s".formatted(e.getMessage())))
                           .build();
        }
    }

    @DELETE
    @Path("/api/editor/{projectId}/media/{mediaHash}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeMedia(@PathParam("projectId") String projectId,
                                @PathParam("mediaHash") String mediaHash) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        var media = project.getMedias().stream()
                           .filter(m -> mediaHash.equals(m.getHash()))
                           .findFirst()
                           .orElseThrow(() -> new NotFoundException("Media not found"));
        boolean inUse = project.getClips().stream()
                               .anyMatch(c -> mediaHash.equals(c.getMediaHash()));
        if (inUse) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(new ErrorResponse("Media is used on the timeline. Remove clips first."))
                           .build();
        }
        project.getMedias().remove(media);
        fileStorageService.deleteFromGridFs(media.getMediaId());
        projects.update(project);
        return Response.ok(new ErrorResponse("Media removed")).build();
    }

    @PUT
    @Path("/api/editor/{projectId}/media/{mediaHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameMedia(@PathParam("projectId") String projectId,
                                @PathParam("mediaHash") String mediaHash,
                                MediaRenameRequest body) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        if (body == null || body.name() == null || body.name().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(new ErrorResponse("Name is required"))
                           .build();
        }
        var media = project.getMedias().stream()
                           .filter(m -> mediaHash.equals(m.getHash()))
                           .findFirst()
                           .orElseThrow(() -> new NotFoundException("Media not found"));
        media.setName(body.name().trim());
        projects.update(project);
        return Response.ok(media).build();
    }

    @POST
    @Path("/api/editor/{projectId}/preview/session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startPreviewSession(@PathParam("projectId") String projectId) {
        projects.find(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var status = previewSessionService.startSession(projectId);
            return Response.ok(new PreviewSessionResponse(
                    status.sessionId(),
                    status.status(),
                    status.manifestUrl(),
                    status.percent(),
                    status.etaSeconds(),
                    status.error())).build();
        } catch (Exception e) {
            logger.error("Preview session failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to start preview session: %s".formatted(e.getMessage())))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/preview/session/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPreviewSessionStatus(@PathParam("projectId") String projectId,
                                            @PathParam("sessionId") String sessionId) {
        var status = previewSessionService.getSessionStatus(sessionId);
        if (status == null || !projectId.equals(status.projectId())) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Preview session not found"))
                           .build();
        }
        return Response.ok(new PreviewSessionResponse(
                status.sessionId(),
                status.status(),
                status.manifestUrl(),
                status.percent(),
                status.etaSeconds(),
                status.error())).build();
    }

    @DELETE
    @Path("/api/editor/{projectId}/preview/session/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopPreviewSession(@PathParam("projectId") String projectId,
                                       @PathParam("sessionId") String sessionId) {
        var status = previewSessionService.getSessionStatus(sessionId);
        if (status == null || !projectId.equals(status.projectId())) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Preview session not found"))
                           .build();
        }
        previewSessionService.stopSession(sessionId);
        return Response.ok(new ErrorResponse("Preview session stopped")).build();
    }

    @GET
    @Path("/preview/{sessionId}/{path:.*}")
    public Response servePreviewFile(@PathParam("sessionId") String sessionId,
                                     @PathParam("path") String path) {
        var session = previewSessionService.getSession(sessionId);
        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            var filePath = SafePaths.resolveRelativeWithin(session.outputDir(), path);
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String contentType = contentTypeForPath(path);
            return Response.ok(filePath.toFile())
                    .type(contentType)
                    .header("Cache-Control", "no-cache")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Failed to serve preview file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/api/editor/{projectId}/render")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    @Produces(MediaType.APPLICATION_JSON)
    public Response renderProject(@PathParam("projectId") String projectId,
                                  RenderRequest request) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        String format = request != null && request.format() != null ? request.format() : "mp4";
        String quality = request != null && request.quality() != null ? request.quality() : "high";
        try {
            var timeline = timelineAssembler.assemble(project);
            applyExportQuality(timeline.getVideoSettings(), quality);
            applyExportFormat(timeline.getVideoSettings(), format);
            String ext = extensionForFormat(format);
            String outputFilename = "rendered_" + System.currentTimeMillis() + "." + ext;
            String outputPath = fileStorageService.getOutputPath(outputFilename).toString();
            videoProcessingService.processTimelineProject(timeline, outputPath);
            return Response.ok(new RenderResponse(
                    outputFilename,
                    "/download/" + outputFilename,
                    "Video rendered successfully")).build();
        } catch (Exception e) {
            logger.error("Render failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to render video: %s".formatted(e.getMessage())))
                           .build();
        }
    }

    @POST
    @Path("/api/editor/{projectId}/duplicate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response duplicateProject(@PathParam("projectId") String projectId) {
        var source = projects.find(projectId)
                             .orElseThrow(() -> new NotFoundException("Project not found"));
        var copy = projects.duplicate(source);
        return Response.ok(copy).build();
    }

    @POST
    @Path("/api/editor/{projectId}/render/queue")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    @Produces(MediaType.APPLICATION_JSON)
    public Response enqueueRender(@PathParam("projectId") String projectId,
                                  RenderQueueRequest request) {
        try {
            String format = request != null ? request.format() : null;
            String quality = request != null ? request.quality() : null;
            RenderJob job = renderJobService.enqueue(projectId, format, quality);
            return Response.accepted(job).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/render/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRenderJobs(@PathParam("projectId") String projectId) {
        return Response.ok(renderJobService.listJobsForProject(projectId)).build();
    }

    @GET
    @Path("/api/render/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRenderJob(@PathParam("jobId") String jobId) {
        RenderJob job = renderJobService.getJob(jobId);
        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse("Render job not found"))
                           .build();
        }
        return Response.ok(job).build();
    }

    @GET
    @Path("/api/templates")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTemplates() {
        return Response.ok(projectTemplateService.listTemplates()).build();
    }

    @GET
    @Path("/api/editor/{projectId}/archive")
    @Produces("application/zip")
    public Response downloadArchive(@PathParam("projectId") String projectId) {
        try {
            var archivePath = projectArchiveService.createArchive(projectId);
            return Response.ok(archivePath.toFile())
                    .header("Content-Disposition",
                            "attachment; filename=\"project_" + projectId + ".zip\"")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        } catch (Exception e) {
            logger.error("Archive failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to create archive: " + e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/share")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShareLink(@PathParam("projectId") String projectId) {
        try {
            return Response.ok(shareLinkService.createShareLink(projectId)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/export/otio")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportOtio(@PathParam("projectId") String projectId) {
        try {
            var path = otioExportService.export(projectId);
            return Response.ok(path.toFile())
                    .header("Content-Disposition",
                            "attachment; filename=\"project_" + projectId + ".otio\"")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("OTIO export failed: " + e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/export/edl")
    @Produces(MediaType.TEXT_PLAIN)
    public Response exportEdl(@PathParam("projectId") String projectId) {
        try {
            var path = edlExportService.export(projectId);
            return Response.ok(path.toFile())
                    .header("Content-Disposition",
                            "attachment; filename=\"project_" + projectId + ".edl\"")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new ErrorResponse(e.getMessage()))
                           .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("EDL export failed: " + e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/plugins")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPlugins() {
        return Response.ok(pluginRegistry.listAll()).build();
    }

    @POST
    @Path("/api/editor/{projectId}/media/{mediaHash}/analyze")
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeMedia(@PathParam("projectId") String projectId,
                                 @PathParam("mediaHash") String mediaHash) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        var media = project.getMedias().stream()
                           .filter(m -> mediaHash.equals(m.getHash()))
                           .findFirst()
                           .orElseThrow(() -> new NotFoundException("Media not found"));
        try {
            var result = mediaAnalysisService.analyze(media);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Analysis failed: " + e.getMessage()))
                           .build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/media/{mediaHash}/thumbnail")
    @Produces("image/jpeg")
    public Response getMediaThumbnail(@PathParam("projectId") String projectId,
                                      @PathParam("mediaHash") String mediaHash) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        var media = project.getMedias().stream()
                           .filter(m -> mediaHash.equals(m.getHash()))
                           .findFirst()
                           .orElseThrow(() -> new NotFoundException("Media not found"));
        try {
            var thumbPath = mediaAnalysisService.getThumbnailPath(mediaHash);
            if (!Files.exists(thumbPath)) {
                mediaAnalysisService.analyze(media);
            }
            thumbPath = mediaAnalysisService.getThumbnailPath(mediaHash);
            if (!Files.exists(thumbPath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(thumbPath.toFile()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/api/editor/{projectId}/media/{mediaHash}/waveform")
    @Produces("image/png")
    public Response getMediaWaveform(@PathParam("projectId") String projectId,
                                     @PathParam("mediaHash") String mediaHash) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found"));
        var media = project.getMedias().stream()
                           .filter(m -> mediaHash.equals(m.getHash()))
                           .findFirst()
                           .orElseThrow(() -> new NotFoundException("Media not found"));
        try {
            var waveformPath = mediaAnalysisService.getWaveformPath(mediaHash);
            if (!Files.exists(waveformPath)) {
                mediaAnalysisService.analyze(media);
            }
            waveformPath = mediaAnalysisService.getWaveformPath(mediaHash);
            if (!Files.exists(waveformPath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(waveformPath.toFile()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String contentTypeForPath(String path) {
        if (path == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        if (path.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (path.endsWith(".ts")) {
            return "video/mp2t";
        }
        if (path.endsWith(".mp4")) {
            return "video/mp4";
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private void applyPreviewQuality(VideoSettings settings) {
        if (settings == null) {
            return;
        }
        settings.setCrf(28);
        settings.setPreset("ultrafast");
        scalePreviewResolution(settings, 640, 360);
    }

    private void scalePreviewResolution(VideoSettings settings, int maxWidth, int maxHeight) {
        int width = settings.getWidth() != null && settings.getWidth() > 0 ? settings.getWidth() : 1280;
        int height = settings.getHeight() != null && settings.getHeight() > 0 ? settings.getHeight() : 720;
        double scale = Math.min(1.0, Math.min((double) maxWidth / width, (double) maxHeight / height));
        settings.setWidth(Math.max(2, (int) Math.round(width * scale)));
        settings.setHeight(Math.max(2, (int) Math.round(height * scale)));
    }

    private void applyExportQuality(VideoSettings settings, String quality) {
        if (settings == null) {
            return;
        }
        switch (quality == null ? "high" : quality.toLowerCase()) {
            case "low" -> {
                settings.setCrf(28);
                settings.setPreset("ultrafast");
                scalePreviewResolution(settings, 854, 480);
            }
            case "medium" -> {
                settings.setCrf(23);
                settings.setPreset("medium");
                scalePreviewResolution(settings, 1280, 720);
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
