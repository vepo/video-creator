package dev.vepo.youtube.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.service.MediaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/capture")
@Produces(MediaType.APPLICATION_JSON)
public class CaptureResource {
    private static final Logger logger = LoggerFactory.getLogger(CaptureResource.class);

    @Inject
    Projects projects;

    @Inject
    MediaService mediaService;

    public record CaptureUploadResponse(String mediaHash, String message) {}

    @POST
    @Path("/{projectId}/webcam")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadWebcam(@PathParam("projectId") String projectId,
                                 @RestForm("file") FileUpload file) {
        return storeCapture(projectId, file, "webcam");
    }

    @POST
    @Path("/{projectId}/screen")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadScreen(@PathParam("projectId") String projectId,
                                 @RestForm("file") FileUpload file) {
        return storeCapture(projectId, file, "screen");
    }

    @POST
    @Path("/{projectId}/voiceover")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadVoiceover(@PathParam("projectId") String projectId,
                                    @RestForm("file") FileUpload file) {
        return storeCapture(projectId, file, "voiceover");
    }

    private Response storeCapture(String projectId, FileUpload file, String captureType) {
        var project = projects.find(projectId)
                              .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Project not found"));
        if (file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(new VideoEditorController.ErrorResponse("File is required"))
                           .build();
        }
        try {
            var tempDir = Files.createTempDirectory("capture_" + captureType);
            var tempFile = tempDir.resolve(file.fileName());
            Files.copy(file.uploadedFile(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            var media = mediaService.store(tempFile, captureType + "_" + file.fileName());
            project.getMedias().add(media);
            projects.update(project);
            logger.info("Stored {} capture for project {}: {}", captureType, projectId, media.getHash());
            return Response.ok(new CaptureUploadResponse(media.getHash(),
                    captureType + " capture uploaded successfully")).build();
        } catch (IOException e) {
            logger.error("Capture upload failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(new VideoEditorController.ErrorResponse(
                                   "Failed to upload capture: " + e.getMessage()))
                           .build();
        }
    }
}
