package dev.vepo.youtube.creator;

import dev.vepo.youtube.creator.service.VideoProcessingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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

    @GET
    @Path("/health")
    public Response healthCheck() {
        boolean meltAvailable = videoProcessingService.isMeltAvailable();
        String status = meltAvailable ? "OK" : "MLT not available";
        return Response.ok().entity(
            new HealthResponse(status, meltAvailable ? "MLT is available" : "MLT melt command not found")
        ).build();
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
}