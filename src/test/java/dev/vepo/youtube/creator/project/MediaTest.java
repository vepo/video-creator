package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.TestTimes;
import dev.vepo.youtube.creator.shared.UnitTest;

@UnitTest
class MediaTest {

    private static final ObjectId KNOWN_ID = new ObjectId();

    @Test
    void mediaInstancesAreEqualWhenTheyShareIdAndName() {
        var first = new Media(KNOWN_ID, "clip.mp4", "abc123", "video/mp4", 1000L, TestTimes.REFERENCE_INSTANT);
        var second = new Media(KNOWN_ID, "clip.mp4", "abc123", "video/mp4", 1000L, TestTimes.REFERENCE_INSTANT);

        assertEquals(first, second);
    }

    @Test
    void mediaTypeIsResolvedFromMimeType() {
        var media = new Media(new ObjectId(), "photo.png", "hash", "image/png", 0L, TestTimes.REFERENCE_INSTANT);

        assertEquals(MediaType.IMAGE, media.getType());
    }
}
