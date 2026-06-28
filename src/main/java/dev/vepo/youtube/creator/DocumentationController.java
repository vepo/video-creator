package dev.vepo.youtube.creator;

import dev.vepo.youtube.creator.service.UserDocumentationService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class DocumentationController {

    @Inject
    @Location("docs.html")
    Template docs;

    @Inject
    UserDocumentationService userDocumentation;

    @GET
    @Path("/docs")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance documentationPage() {
        return docs.data("content", userDocumentation.renderHtml())
                   .data("lastUpdated", userDocumentation.lastUpdated())
                   .data("appVersion", "1.0.0");
    }
}
