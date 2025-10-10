package dev.vepo.youtube.creator.project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;

import dev.vepo.youtube.creator.infra.MongoSerializers;

public class Project {
    @BsonId
    @JsonSerialize(using = MongoSerializers.ObjectIdJacksonSerializer.class)
    private ObjectId id;
    private String name;
    private String description;
    private List<Media> medias;
    private List<Clip> clips;
    private long duration;
    @JsonSerialize(using = MongoSerializers.InstantJacksonSerializer.class)
    private Instant createdAt;

    public Project() {
        this.medias = new ArrayList<>();
        this.clips = new ArrayList<>();
        this.duration = 1000 * 60 * 30; // 30 min
        this.createdAt = Instant.now();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Media> getMedias() {
        return medias;
    }

    public void setMedias(List<Media> medias) {
        this.medias = medias;
    }

    public List<Clip> getClips() {
        return clips;
    }

    public void setClips(List<Clip> clips) {
        this.clips = clips;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            var other = (Project) obj;
            return Objects.equals(id, other.id);
        }
    }

    @Override
    public String toString() {
        return "Project[id=%s, medias=%s, clips=%s]".formatted(id, medias, clips);
    }

    private static final ObjectWriter mapper = JsonMapper.builder()
                                                         .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
                                                         .build()
                                                         .writerFor(Project.class);
    public String asJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }
}
