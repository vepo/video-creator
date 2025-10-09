package dev.vepo.youtube.creator.infra;

import io.quarkus.jsonb.JsonbConfigCustomizer;
import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;

@Singleton
public class RegisterJsonbSerializers implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withSerializers(new MongoSerializers.ObjectIdJsonbSerializer())
                   .withDeserializers(new MongoSerializers.ObjectIdJsonbDeserializer());
    }

}