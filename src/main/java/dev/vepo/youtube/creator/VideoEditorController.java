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
    public record PreviewResponse(String previewFilename, String downloadUrl, String message) {}
    public record RenderResponse(String outputFilename, String downloadUrl, String message) {}

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

    @POST
    @Path("/api/editor/{projectId}/preview")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewProject(@PathParam("projectId") String projectId) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var timeline = timelineAssembler.assemble(project);
            double previewDuration = Math.min(30.0, Math.max(1.0, timeline.getDuration()));
            String path = videoProcessingService.generatePreview(timeline, 0, previewDuration);
            String previewFilename = Paths.get(path).getFileName().toString();
            return Response.ok(new PreviewResponse(
                    previewFilename,
                    "/download/" + previewFilename,
                    "Preview generated successfully")).build();
        } catch (Exception e) {
            logger.error("Preview failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new ErrorResponse("Failed to generate preview: %s".formatted(e.getMessage())))
                           .build();
        }
    }

    @POST
    @Path("/api/editor/{projectId}/render")
    @Produces(MediaType.APPLICATION_JSON)
    public Response renderProject(@PathParam("projectId") String projectId) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        try {
            var timeline = timelineAssembler.assemble(project);
            applyExportQuality(timeline.getVideoSettings(), "high");
            String outputFilename = "rendered_" + System.currentTimeMillis() + ".mp4";
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
            String previewPath = videoProcessingService.generatePreview(project, 0, 10);
            String previewFilename = Paths.get(previewPath).getFileName().toString();
            return Response.ok(new PreviewResponse(
                    previewFilename,
                    "/download/" + previewFilename,
                    "Preview generated successfully")).build();
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
}
