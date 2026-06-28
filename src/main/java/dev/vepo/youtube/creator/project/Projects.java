package dev.vepo.youtube.creator.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Projects {

    @ConfigProperty(name = "mongodb.database")
    String databaseName;

    @Inject
    MongoClient mongoClient;

    private MongoCollection<Project> collection() {
        return mongoClient.getDatabase(databaseName)
                          .getCollection("project", Project.class);
    }

    public List<Project> loadAll() {
        return collection().find()
                           .into(new ArrayList<>());
    }

    public Project newProject() {
        var project = new Project();
        collection().insertOne(project);
        return project;
    }

    public Optional<Project> find(String id) {
        if (id == null || id.isBlank() || !ObjectId.isValid(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(collection().find(Filters.eq("_id", new ObjectId(id))).first());
    }

    public Project update(Project project) {
        collection().replaceOne(Filters.eq("_id", project.getId()), project);
        return project;
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank() || !ObjectId.isValid(id)) {
            return false;
        }
        var result = collection().deleteOne(Filters.eq("_id", new ObjectId(id)));
        return result.getDeletedCount() > 0;
    }

    public Project duplicate(Project source) {
        var copy = new Project();
        copy.setName(source.getName() + " (copy)");
        copy.setDescription(source.getDescription());
        copy.setMedias(new ArrayList<>(source.getMedias()));
        copy.setClips(source.getClips() != null ? new ArrayList<>(source.getClips()) : new ArrayList<>());
        copy.setTracks(source.getTracks() != null ? new ArrayList<>(source.getTracks()) : new ArrayList<>());
        copy.setDuration(source.getDuration());
        copy.setScreenSize(source.getScreenSize());
        copy.setFrameRate(source.getFrameRate());
        collection().insertOne(copy);
        return copy;
    }
}
