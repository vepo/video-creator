package dev.vepo.youtube.creator.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.bson.types.ObjectId;

import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.MediaType;
import dev.vepo.youtube.creator.support.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import jakarta.inject.Inject;

public class MediaSteps {

    private static final ObjectId KNOWN_ID = new ObjectId();

    @Inject
    ScenarioContext context;

    @Given("media with a known id and name {string}")
    public void mediaWithKnownId(String name) {
        context.setMedia(new Media(KNOWN_ID, name, "abc123", "video/mp4", 1000L, Instant.now()));
    }

    @Given("another media instance with the same id and name {string}")
    public void anotherMediaWithSameId(String name) {
        context.setSecondMedia(new Media(KNOWN_ID, name, "abc123", "video/mp4", 1000L, Instant.now()));
    }

    @Given("media with mime type {string} and name {string}")
    public void mediaWithMimeType(String mimeType, String name) {
        context.setMedia(new Media(new ObjectId(), name, "hash", mimeType, 0L, Instant.now()));
    }

    @Then("the media instances should be equal")
    public void mediaInstancesShouldBeEqual() {
        assertEquals(context.getMedia(), context.getSecondMedia());
    }

    @Then("the media type should be VIDEO")
    public void mediaTypeShouldBeVideo() {
        assertEquals(MediaType.VIDEO, context.getMedia().getType());
    }
}
