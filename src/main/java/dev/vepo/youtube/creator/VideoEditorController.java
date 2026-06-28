package dev.vepo.youtube.creator;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.model.VideoSettings;
import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.service.MediaService;
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

    public record UploadResponse(String fileId, String fileName, String filePath, String message) {}
    public record ErrorResponse(String error) {}
    public record PreviewResponse(String previewFilename, String downloadUrl, String message, Double durationSeconds) {}
    public record RenderResponse(String outputFilename, String downloadUrl, String message) {}
    public record RenderRequest(String format, String quality) {}
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
            var disposition = filename.startsWith("preview_")
                    ? "inline; filename=\"" + filename + "\""
                    : "attachment; filename=\"" + filename + "\"";
            return Response.ok(filePath.toFile())
                    .header("Content-Disposition", disposition)
                    .build();
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
        logger.info("Request! name={} file={}", name, file.uploadedFile());
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var tempDir = Files.createTempDirectory("upload");
            var tempFile = tempDir.resolve(file.fileName());
            Files.copy(file.uploadedFile(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            var media = fileStorageService.store(tempFile, file.fileName());
            logger.info("Created media={}", media);
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
    @Path("/api/editor/{projectId}/preview")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewProject(@PathParam("projectId") String projectId) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var timeline = timelineAssembler.assemble(project);
            applyPreviewQuality(timeline.getVideoSettings());
            double previewDuration = Math.max(1.0, timeline.getDuration());
            String path = videoProcessingService.generatePreview(timeline);
            String previewFilename = Paths.get(path).getFileName().toString();
            return Response.ok(new PreviewResponse(
                    previewFilename,
                    "/download/" + previewFilename,
                    "Preview generated successfully",
                    previewDuration)).build();
        } catch (Exception e) {
            logger.error("Preview failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to generate preview: %s".formatted(e.getMessage())))
                           .build();
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
    @Path("/api/timeline/preview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generatePreview(TimelineProject project) {
        try {
            project.updateDuration();
            applyPreviewQuality(project.getVideoSettings());
            String previewPath = videoProcessingService.generatePreview(project);
            String previewFilename = Paths.get(previewPath).getFileName().toString();
            return Response.ok(new PreviewResponse(
                    previewFilename,
                    "/download/" + previewFilename,
                    "Preview generated successfully",
                    Math.max(1.0, project.getDuration()))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to generate preview: %s".formatted(e.getMessage())))
                           .build();
        }
    }

    @POST
    @Path("/api/timeline/render")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renderTimelineProject(TimelineProject project) {
        try {
            String outputFilename = "rendered_" + System.currentTimeMillis() + ".mp4";
            String outputPath = fileStorageService.getOutputPath(outputFilename).toString();
            videoProcessingService.processTimelineProject(project, outputPath);
            return Response.ok(new RenderResponse(
                    outputFilename,
                    "/download/" + outputFilename,
                    "Video rendered successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to render video: %s".formatted(e.getMessage())))
                           .build();
        }
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
