package dev.vepo.youtube.creator.infra;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonParser;

public interface MongoSerializers {

    public static class ObjectIdJacksonSerializer extends StdSerializer<ObjectId> {
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

    public static class InstantJacksonSerializer extends StdSerializer<Instant> {
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

    public static class ObjectIdJsonbSerializer implements JsonbSerializer<ObjectId> {

        @Override
        public void serialize(ObjectId obj, jakarta.json.stream.JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.toHexString());
        }
    }

    public static class ObjectIdJsonbDeserializer implements JsonbDeserializer<ObjectId> {

        @Override
        public ObjectId deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return new ObjectId(parser.getString());
        }
    }
}
