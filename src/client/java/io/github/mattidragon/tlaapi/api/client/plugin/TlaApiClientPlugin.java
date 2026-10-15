package io.github.mattidragon.tlaapi.api.client.plugin;

import io.github.mattidragon.tlaapi.impl.PluginOnly;

/**
 * This interface serves as the entrypoint for all plugins to the TLA API.
 * Plugins should implement this interface and register themselves with the provided {@link PluginContext}.
 * Plugins are registered using {@code tla-api} entrypoint in their {@code fabric.mod.json}.
 * @see PluginContext
 */
@PluginOnly
public interface TlaApiClientPlugin {
    /**
     * Called on clients when the plugin is registered.
     * Register your gui handlers for any recipe types you want here.
     */
    void register(ClientPluginContext context);
}
