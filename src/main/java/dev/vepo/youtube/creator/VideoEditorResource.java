package dev.vepo.youtube.creator;

import java.io.IOException;
import java.nio.file.Files;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.vepo.youtube.creator.model.VideoEditRequest;
import dev.vepo.youtube.creator.model.VideoUploadResponse;
import dev.vepo.youtube.creator.service.FileStorageService;
import dev.vepo.youtube.creator.service.VideoProcessingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/video")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoEditorResource {

    @Inject
    VideoProcessingService videoProcessingService;

    @Inject
    FileStorageService fileStorageService;

    @GET
    @Path("/health")
    public Response healthCheck() {
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        String status = meltAvailable ? "OK" : "MLT not available";
        return Response.ok().entity(
            new HealthResponse(status, meltAvailable ? "MLT is available" : "MLT melt command not found")
        ).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadVideo(@RestForm("file") FileUpload fileUpload) {
        try {
            byte[] fileContent = Files.readAllBytes(fileUpload.uploadedFile());
            String storedPath = fileStorageService.storeUploadedFile(fileContent, fileUpload.fileName());
            
            VideoUploadResponse response = new VideoUploadResponse(
                fileUpload.fileName(),
                "File uploaded successfully",
                storedPath,
                fileUpload.size()
            );
            
            return Response.ok(response).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to upload file: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/edit")
    public Response editVideo(VideoEditRequest editRequest) {
        try {
            String outputPath = videoProcessingService.processVideo(editRequest);
            
            VideoEditResponse response = new VideoEditResponse(
                "Video processed successfully",
                outputPath,
                editRequest.getOutputFile()
            );
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Video processing failed: " + e.getMessage()))
                .build();
        }
    }

    // Additional helper classes for responses
    public static class HealthResponse {
        public String status;
        public String message;
        
        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    public static class VideoEditResponse {
        public String message;
        public String outputPath;
        public String outputFilename;
        
        public VideoEditResponse(String message, String outputPath, String outputFilename) {
            this.message = message;
            this.outputPath = outputPath;
            this.outputFilename = outputFilename;
        }
    }

    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}