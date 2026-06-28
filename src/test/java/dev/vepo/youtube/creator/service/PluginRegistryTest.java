package dev.vepo.youtube.creator.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vepo.youtube.creator.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PluginRegistryTest {

    @Inject
    PluginRegistry pluginRegistry;

    @Test
    void loadsEffectAndTransitionPlugins() {
        assertFalse(pluginRegistry.listAll().isEmpty());
    }

    @Test
    void listByTypeFiltersPlugins() {
        var effects = pluginRegistry.listByType("effect");
        var transitions = pluginRegistry.listByType("transition");

        assertFalse(effects.isEmpty());
        assertFalse(transitions.isEmpty());
        assertTrue(effects.stream().allMatch(p -> "effect".equalsIgnoreCase(p.type())));
    }

    @Test
    void findByIdReturnsKnownPluginOrNull() {
        var first = pluginRegistry.listAll().getFirst();
        assertNotNull(pluginRegistry.findById(first.id()));
        assertNull(pluginRegistry.findById("nonexistent-plugin-id"));
    }
}
