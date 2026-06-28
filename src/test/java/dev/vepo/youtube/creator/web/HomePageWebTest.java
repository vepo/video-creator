package dev.vepo.youtube.creator.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.App;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.WebPlatformTest;

@WebPlatformTest
class HomePageWebTest {

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }

    @Test
    void homePageRendersSuccessfully(App app) {
        app.access()
           .assertOnHomePage()
           .assertDocumentTitleContains("Video Creator")
           .assertPageContainsText("Projects");
    }
}
