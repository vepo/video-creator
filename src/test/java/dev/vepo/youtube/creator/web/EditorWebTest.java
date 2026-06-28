package dev.vepo.youtube.creator.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.App;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.WebEditorTest;

@WebEditorTest
class EditorWebTest {

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }

    @Test
    void openingEditorForNewCreatesProjectAndRedirects(App app) {
        app.openNewProject()
           .assertEditorLoaded()
           .assertUrlContainsProjectId();
    }

    @Test
    void openingEditorForExistingProjectRendersEditor(App app) {
        var project = Given.persistProject();

        app.openEditor(project.getId().toHexString())
           .assertEditorLoaded();
    }

    @Test
    void openingEditorForUnknownProjectRedirectsToHome(App app) {
        app.openEditor("000000000000000000000000")
           .assertOnHomePage();
    }

    @Test
    void openingEditorForInvalidProjectIdRedirectsToHome(App app) {
        app.openEditor("not-a-valid-id")
           .assertOnHomePage();
    }

    @Test
    void openingNonExistentPageRedirectsToHome(App app) {
        app.goTo("/unknown-page")
           .assertOnHomePage();
    }
}
