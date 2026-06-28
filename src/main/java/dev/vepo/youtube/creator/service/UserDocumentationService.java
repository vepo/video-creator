package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserDocumentationService {
    private static final String RESOURCE_PATH = "/documentation/USER_GUIDE.md";
    private static final Pattern LAST_UPDATED = Pattern.compile(
            "^Last updated:\\s*(.+)$", Pattern.MULTILINE);

    public String loadMarkdown() {
        try (InputStream input = UserDocumentationService.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Missing documentation resource: " + RESOURCE_PATH);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read documentation", e);
        }
    }

    public String renderHtml() {
        List<Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
        Node document = parser.parse(loadMarkdown());
        return renderer.render(document);
    }

    public String lastUpdated() {
        Matcher matcher = LAST_UPDATED.matcher(loadMarkdown());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "unknown";
    }
}
