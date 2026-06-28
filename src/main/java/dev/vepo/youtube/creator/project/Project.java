package dev.vepo.youtube.creator.project;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;

import dev.vepo.youtube.creator.infra.MongoSerializers;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;

public class Project {
    @BsonId
    @JsonbTypeAdapter(MongoSerializers.ObjectIdJsonbAdapter.class)
    @JsonSerialize(using = MongoSerializers.ObjectIdJacksonSerializer.class)
    @JsonDeserialize(using = MongoSerializers.ObjectIdJacksonDeserializer.class)
    private ObjectId id;
    private String name;
    private String description;
    private List<Media> medias;
    private List<Clip> clips;
    private List<Track> tracks;
    private long duration;
    private ScreenSize screenSize;
    private FrameRate frameRate;
    private List<MulticamGroup> multicamGroups;
    private String shareToken;
    @JsonbTypeSerializer(MongoSerializers.InstantJsonbSerializer.class)
    @JsonbTypeDeserializer(MongoSerializers.InstantJsonbDeserializer.class)
    @JsonSerialize(using = MongoSerializers.InstantJacksonSerializer.class)
    @JsonDeserialize(using = MongoSerializers.InstantJacksonDeserializer.class)
    private Instant createdAt;

    public Project() {
        this.name = "Project %s".formatted(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now()));
        this.medias = new ArrayList<>();
        this.clips = new ArrayList<>();
        this.tracks = new ArrayList<>();
        initializeDefaultTracks();
        this.duration = 1000 * 60 * 30; // 30 min
        this.screenSize = ScreenSize.getDefault();
        this.frameRate = FrameRate.getDefault();
        this.multicamGroups = new ArrayList<>();
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

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    private void initializeDefaultTracks() {
        tracks.add(new Track(0, "Video Track 1", MediaType.VIDEO));
        tracks.add(new Track(1, "Audio Track 1", MediaType.AUDIO));
    }

    public void ensureTracks() {
        if (tracks == null || tracks.isEmpty()) {
            tracks = new ArrayList<>();
            initializeDefaultTracks();
        }
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public FrameRate getFrameRate() {
        return frameRate;
    }
    
    public void setFrameRate(FrameRate frameRate) {
        this.frameRate = frameRate;
    }

    public ScreenSize getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSize screenSize) {
        this.screenSize = screenSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<MulticamGroup> getMulticamGroups() {
        return multicamGroups;
    }

    public void setMulticamGroups(List<MulticamGroup> multicamGroups) {
        this.multicamGroups = multicamGroups != null ? multicamGroups : new ArrayList<>();
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
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
