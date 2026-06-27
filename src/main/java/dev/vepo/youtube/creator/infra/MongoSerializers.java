package dev.vepo.youtube.creator.infra;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonParser;

public interface MongoSerializers {

    final class ObjectIdJacksonSerializer extends StdSerializer<ObjectId> {
        public ObjectIdJacksonSerializer() {
            this(null);
        }

        public ObjectIdJacksonSerializer(Class<ObjectId> t) {
            super(t);
        }

        @Override
        public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    final class ObjectIdJacksonDeserializer extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(com.fasterxml.jackson.core.JsonParser parser,
                                    com.fasterxml.jackson.databind.DeserializationContext context) throws IOException {
            return new ObjectId(parser.getText());
        }
    }

    final class InstantJacksonSerializer extends StdSerializer<Instant> {
        public InstantJacksonSerializer() {
            this(null);
        }

        public InstantJacksonSerializer(Class<Instant> t) {
            super(t);
        }

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.toEpochMilli());
        }
    }

    final class InstantJacksonDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(com.fasterxml.jackson.core.JsonParser parser,
                                   com.fasterxml.jackson.databind.DeserializationContext context) throws IOException {
            if (parser.isExpectedNumberIntToken()) {
                return Instant.ofEpochMilli(parser.getLongValue());
            }
            return Instant.parse(parser.getText());
        }
    }

    final class ObjectIdJsonbAdapter implements JsonbAdapter<ObjectId, String> {
        @Override
        public String adaptToJson(ObjectId obj) {
            return obj == null ? null : obj.toHexString();
        }

        @Override
        public ObjectId adaptFromJson(String obj) {
            return obj == null || obj.isBlank() ? null : new ObjectId(obj);
        }
    }

    final class InstantJsonbSerializer implements JsonbSerializer<Instant> {
        @Override
        public void serialize(Instant obj, jakarta.json.stream.JsonGenerator generator, SerializationContext ctx) {
            if (obj == null) {
                generator.writeNull();
            } else {
                generator.write(obj.toEpochMilli());
            }
        }
    }

    final class InstantJsonbDeserializer implements JsonbDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return readInstant(parser.getValue());
        }

        private static Instant readInstant(JsonValue value) {
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                return null;
            }
            return switch (value.getValueType()) {
                case NUMBER -> Instant.ofEpochMilli(((JsonNumber) value).longValue());
                case STRING -> Instant.parse(((JsonString) value).getString());
                default -> throw new IllegalStateException("Cannot deserialize Instant from " + value.getValueType());
            };
        }
    }

    final class ObjectIdJsonbSerializer implements JsonbSerializer<ObjectId> {

        @Override
        public void serialize(ObjectId obj, jakarta.json.stream.JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.toHexString());
        }
    }

    final class ObjectIdJsonbDeserializer implements JsonbDeserializer<ObjectId> {

        @Override
        public ObjectId deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonValue value = parser.getValue();
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                return null;
            }
            if (value.getValueType() != JsonValue.ValueType.STRING) {
                throw new IllegalStateException("Cannot deserialize ObjectId from " + value.getValueType());
            }
            String hex = ((JsonString) value).getString();
            return hex.isBlank() ? null : new ObjectId(hex);
        }
    }
}
