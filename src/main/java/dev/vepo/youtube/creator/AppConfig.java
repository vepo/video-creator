package dev.vepo.youtube.creator;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "app")
public interface AppConfig {
    String uploadDir();
    String outputDir();
    String tempDir();
    String meltCommand();
    String defaultVideoCodec();
    String defaultAudioCodec();
    int defaultCrf();
}