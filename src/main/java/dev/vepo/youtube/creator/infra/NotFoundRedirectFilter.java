package dev.vepo.youtube.creator.infra;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundRedirectFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (responseContext.getStatus() != 404) {
            return;
        }
        if (!"GET".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        if (NotFoundRedirects.shouldNotRedirect(requestContext.getUriInfo().getRequestUri().getPath())) {
            return;
        }
        responseContext.setStatus(303);
        responseContext.getHeaders().putSingle("Location", "/");
        responseContext.setEntity(null);
    }
}
