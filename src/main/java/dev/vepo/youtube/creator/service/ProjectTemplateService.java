package dev.vepo.youtube.creator.service;

import java.util.List;

import dev.vepo.youtube.creator.project.FrameRate;
import dev.vepo.youtube.creator.project.ScreenSize;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectTemplateService {

    public record ProjectTemplate(String id, String name, String description,
                                  ScreenSize screenSize, FrameRate frameRate) {
    }

    public List<ProjectTemplate> listTemplates() {
        return List.of(
                new ProjectTemplate("youtube-1080p", "YouTube 1080p",
                        "Full HD landscape template for YouTube uploads",
                        ScreenSize.HD_1080p, FrameRate.FPS_30_WEB),
                new ProjectTemplate("youtube-shorts", "YouTube Shorts",
                        "Vertical 9:16 template for short-form video",
                        ScreenSize.SHORTS_1080x1920, FrameRate.FPS_30_WEB),
                new ProjectTemplate("podcast-audio", "Podcast / Audio",
                        "Audio-focused project with a single audio track layout",
                        ScreenSize.HD_720p, FrameRate.FPS_24),
                new ProjectTemplate("instagram-square", "Instagram Square",
                        "1:1 square format for social posts",
                        ScreenSize.SQUARE_1080p, FrameRate.FPS_30_WEB));
    }

    public ProjectTemplate findTemplate(String id) {
        return listTemplates().stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
