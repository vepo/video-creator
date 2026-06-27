package dev.vepo.youtube.creator.project;

import java.time.Instant;
import java.util.Objects;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dev.vepo.youtube.creator.infra.MongoSerializers;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;

public class Media {
    @JsonbTypeAdapter(MongoSerializers.ObjectIdJsonbAdapter.class)
    @JsonSerialize(using = MongoSerializers.ObjectIdJacksonSerializer.class)
    @JsonDeserialize(using = MongoSerializers.ObjectIdJacksonDeserializer.class)
    private ObjectId mediaId;
    private String name;
    private String hash;
    private String mimeType;
    private MediaType type;
    private long duration;
    @JsonbTypeSerializer(MongoSerializers.InstantJsonbSerializer.class)
    @JsonbTypeDeserializer(MongoSerializers.InstantJsonbDeserializer.class)
    @JsonSerialize(using = MongoSerializers.InstantJacksonSerializer.class)
    @JsonDeserialize(using = MongoSerializers.InstantJacksonDeserializer.class)
    private Instant uploadedAt;

    public Media() {
    }

    public Media(ObjectId mediaId, String name, String hash, String mimeType, long duration, Instant uploadedAt) {
        this.name = name;
        this.mediaId = mediaId;
        this.hash = hash;
        this.mimeType = mimeType;
        this.type = MediaType.load(mimeType);
        this.duration = duration;
        this.uploadedAt = uploadedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectId getMediaId() {
        return mediaId;
    }

    public void setMediaId(ObjectId mediaId) {
        this.mediaId = mediaId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            var other = (Media) obj;
            return Objects.equals(mediaId, other.mediaId);
        }
    }

    @Override
    public String toString() {
        return "Media [mediaId=%s, name=%s, hash=%s, mimeType=%s, type=%s, duration=%d, uploadedAt=%s]".formatted(
                mediaId, name, hash, mimeType, type, duration, uploadedAt);
    }

}
