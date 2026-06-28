package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.project.RenderJob.Status;
import dev.vepo.youtube.creator.shared.Given;
import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class RenderJobServiceTest {

    @Inject
    RenderJobService renderJobService;

    private String projectId;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        projectId = Given.persistProject().getId().toHexString();
    }

    @Test
    void enqueueReturnsQueuedJob() {
        var job = renderJobService.enqueue(projectId, "mp4", "high");

        assertNotNull(job.getId());
        assertEquals(projectId, job.getProjectId());
        assertEquals("mp4", job.getFormat());
        assertTrue(job.getStatus() == Status.QUEUED || job.getStatus() == Status.RUNNING);
    }

    @Test
    void getJobReturnsEnqueuedJob() {
        var job = renderJobService.enqueue(projectId, "webm", "medium");

        var loaded = renderJobService.getJob(job.getId());

        assertNotNull(loaded);
        assertEquals(job.getId(), loaded.getId());
    }

    @Test
    void listJobsForProjectIncludesEnqueuedJob() {
        var job = renderJobService.enqueue(projectId, "mov", "low");

        var jobs = renderJobService.listJobsForProject(projectId);

        assertNotNull(jobs.stream().filter(j -> j.getId().equals(job.getId())).findFirst().orElse(null));
    }
}
