package dev.vepo.youtube.creator.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PluginRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);

    public record PluginDescriptor(String id, String name, String type, String mltService,
                                   Map<String, Object> defaultParams) {
    }

    private final List<PluginDescriptor> plugins = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void loadPlugins() {
        loadFromResource("/plugins/effects.json", "effect");
        loadFromResource("/plugins/transitions.json", "transition");
    }

    private void loadFromResource(String resourcePath, String defaultType) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.debug("No plugin descriptor at {}", resourcePath);
                return;
            }
            List<Map<String, Object>> entries = objectMapper.readValue(in, new TypeReference<>() {});
            for (Map<String, Object> entry : entries) {
                plugins.add(new PluginDescriptor(
                        String.valueOf(entry.get("id")),
                        String.valueOf(entry.get("name")),
                        entry.containsKey("type") ? String.valueOf(entry.get("type")) : defaultType,
                        String.valueOf(entry.get("mltService")),
                        entry.containsKey("defaultParams")
                                ? objectMapper.convertValue(entry.get("defaultParams"), new TypeReference<>() {})
                                : Map.of()));
            }
        } catch (IOException e) {
            logger.warn("Failed to load plugins from {}: {}", resourcePath, e.getMessage());
        }
    }

    public List<PluginDescriptor> listAll() {
        return List.copyOf(plugins);
    }

    public List<PluginDescriptor> listByType(String type) {
        return plugins.stream()
                .filter(p -> type.equalsIgnoreCase(p.type()))
                .toList();
    }

    public PluginDescriptor findById(String id) {
        return plugins.stream()
                .filter(p -> id.equals(p.id()))
                .findFirst()
                .orElse(null);
    }
}
