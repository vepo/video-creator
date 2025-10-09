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
        return Optional.ofNullable(collection().find(Filters.eq("_id", new ObjectId(id))).first());
    }

    public Project update(Project project) {
        collection().replaceOne(Filters.eq("_id", project.getId()), project);
        return project;
    }
}
