package dev.vepo.youtube.creator.infra;

import java.net.URI;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(0)
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException> {

    @Context
    ContainerRequestContext requestContext;

    @Override
    public Response toResponse(NotFoundException exception) {
        var path = requestContext.getUriInfo().getRequestUri().getPath();
        var method = requestContext.getMethod();
        if (NotFoundRedirects.shouldRedirectBrowserRequest(method, path)) {
            return Response.seeOther(URI.create("/")).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(Map.of("error", "Not found"))
                       .build();
    }
}
