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

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.service.MediaService;
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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home() {
        return index.data("meltAvailable", videoProcessingService.isMeltAvailable(), 
                     "projects", projects.loadAll());
    }


    @GET
    @Path("/download/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("filename") String filename) {
        try {
            java.nio.file.Path filePath = fileStorageService.getOutputPath(filename);
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(filePath.toFile())
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
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
        var project = projects.find(projectId)
                              .orElseThrow(() -> new NotFoundException("Project not found!!!"));
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        return Response.ok()
                       .entity(editor.data("meltAvailable", meltAvailable, 
                                           "project", project)
                                     .render())
                       .build();
    }

    public record UploadResponse(String fileId, String fileName, String filePath, String message){}
    public record ErrorResponse(String error) {}

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
    @Path("/api/timeline/preview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generatePreview(TimelineProject project) {
        try {
            String previewPath = videoProcessingService.generatePreview(project, 0, 10);
            
            return Response.ok()
                .entity("{\"previewPath\": \"" + previewPath + "\", \"message\": \"Preview generated successfully\"}")
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Failed to generate preview: " + e.getMessage() + "\"}")
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
            String resultPath = videoProcessingService.processTimelineProject(project, outputPath);
            
            return Response.ok()
                .entity("{\"outputPath\": \"" + resultPath + "\", \"outputFilename\": \"" + outputFilename + "\", \"message\": \"Video rendered successfully\"}")
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Failed to render video: " + e.getMessage() + "\"}")
                .build();
        }
    }
}