package dev.vepo.youtube.creator.shared;

import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import jakarta.enterprise.inject.spi.CDI;

public interface Given {

    static <T> T inject(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    static void cleanup() {
        TestTimes.reset();
        var projects = inject(Projects.class);
        projects.loadAll().forEach(project -> projects.delete(project.getId().toHexString()));
    }

    static Project persistProject() {
        return inject(Projects.class).newProject();
    }
}
