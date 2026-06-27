package dev.vepo.youtube.creator.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.Media;
import dev.vepo.youtube.creator.project.Project;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

class MongoSerializersTest {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Test
    void roundTripsProjectWithInstantAndObjectIdFields() {
        var project = new Project();
        project.setId(new ObjectId());
        project.setCreatedAt(Instant.parse("2024-06-01T12:00:00Z"));

        var media = new Media();
        media.setMediaId(new ObjectId());
        media.setUploadedAt(Instant.parse("2024-06-02T08:30:00Z"));
        media.setName("clip.mp4");
        media.setHash("abc123");
        project.getMedias().add(media);

        var json = jsonb.toJson(project);
        var restored = jsonb.fromJson(json, Project.class);

        assertEquals(project.getId(), restored.getId());
        assertEquals(project.getCreatedAt(), restored.getCreatedAt());
        assertEquals(1, restored.getMedias().size());
        assertEquals(media.getMediaId(), restored.getMedias().getFirst().getMediaId());
        assertEquals(media.getUploadedAt(), restored.getMedias().getFirst().getUploadedAt());
    }

    @Test
    void deserializesNullInstantFields() {
        var json = """
                {
                  "createdAt": null,
                  "medias": []
                }
                """;

        var project = jsonb.fromJson(json, Project.class);

        assertNull(project.getCreatedAt());
    }

}
