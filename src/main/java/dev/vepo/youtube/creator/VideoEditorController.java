package dev.vepo.youtube.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.vepo.youtube.creator.model.TimelineProject;
import dev.vepo.youtube.creator.service.FileStorageService;
import dev.vepo.youtube.creator.service.VideoProcessingService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class VideoEditorController {

    @Inject
    @Location("index.html")
    Template index;

    @Inject
    @Location("timeline-editor.html")
    Template timelineEditor;

    @Inject
    VideoProcessingService videoProcessingService;

    @Inject
    FileStorageService fileStorageService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance home() {
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        return index.data("meltAvailable", meltAvailable);
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
    @Path("/editor")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editorPage() {
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        return timelineEditor.data("meltAvailable", meltAvailable);
    }

    @POST
    @Path("/api/timeline/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTimelineProject(TimelineProject project) {
        try {
            if (project.getId() == null) {
                project.setId("project_" + System.currentTimeMillis());
            }
            if (project.getName() == null) {
                project.setName("New Project");
            }
            
            project.updateDuration();
            
            return Response.ok(project).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Failed to create timeline project: " + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/api/timeline/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(@FormParam("file") FileUpload fileUpload) {
        try {
            byte[] fileContent = Files.readAllBytes(fileUpload.uploadedFile());
            String storedPath = fileStorageService.storeUploadedFile(fileContent, fileUpload.fileName());
            
            return Response.ok()
                .entity("{\"fileId\": \"" + UUID.randomUUID().toString() + "\", \"fileName\": \"" + fileUpload.fileName() + "\", \"filePath\": \"" + storedPath + "\", \"message\": \"File uploaded successfully\"}")
                .build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Failed to upload file: " + e.getMessage() + "\"}")
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