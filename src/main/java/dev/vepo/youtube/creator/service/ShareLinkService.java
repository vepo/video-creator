package dev.vepo.youtube.creator.service;

import java.util.UUID;

import dev.vepo.youtube.creator.project.Project;
import dev.vepo.youtube.creator.project.Projects;
import dev.vepo.youtube.creator.project.UserAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShareLinkService {

    @Inject
    Projects projects;

    public record ShareLinkResponse(String shareUrl, String token, UserAccount owner) {
    }

    public ShareLinkResponse createShareLink(String projectId) {
        Project project = projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getShareToken() == null || project.getShareToken().isBlank()) {
            project.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            projects.update(project);
        }
        var owner = new UserAccount("local-user", "Local User");
        return new ShareLinkResponse(
                "/editor/" + projectId + "?token=" + project.getShareToken(),
                project.getShareToken(),
                owner);
    }

    public Project resolveSharedProject(String projectId, String token) {
        Project project = projects.find(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getShareToken() == null || !project.getShareToken().equals(token)) {
            throw new IllegalArgumentException("Invalid share token");
        }
        return project;
    }
}
