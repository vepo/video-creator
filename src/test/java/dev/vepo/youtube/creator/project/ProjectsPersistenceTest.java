package dev.vepo.youtube.creator.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class ProjectsPersistenceTest {

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }

    @Test
    void persistedProjectCanBeLoadedById() {
        var persisted = Given.persistProject();

        var loaded = Given.inject(Projects.class).find(persisted.getId().toHexString()).orElseThrow();

        assertEquals(persisted.getName(), loaded.getName());
    }

    @Test
    void projectListContainsPersistedProject() {
        var persisted = Given.persistProject();

        var contains = Given.inject(Projects.class).loadAll().stream()
                .map(Project::getId)
                .anyMatch(id -> id.equals(persisted.getId()));

        assertTrue(contains);
    }
}
