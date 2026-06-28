package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class UserDocumentationServiceTest {

    private final UserDocumentationService service = new UserDocumentationService();

    @Test
    void loadMarkdownContainsGuideTitle() {
        assertTrue(service.loadMarkdown().contains("Video Creator"));
    }

    @Test
    void renderHtmlProducesStructuredContent() {
        var html = service.renderHtml();
        assertTrue(html.contains("<h2"));
        assertTrue(html.contains("Timeline"));
    }

    @Test
    void lastUpdatedIsParsedFromHeader() {
        var updated = service.lastUpdated();
        assertFalse(updated.isBlank());
        assertFalse("unknown".equals(updated));
    }
}
