package io.github.mattidragon.tlaapi.api.client.plugin;

import io.github.mattidragon.tlaapi.impl.ImplementationOnly;
import io.github.mattidragon.tlaapi.impl.client.TlaApiClient;

/**
 * Provides implementations with the ability to load plugins.
 */
@ImplementationOnly
public final class ClientPluginLoader {
    private ClientPluginLoader() {}

    /**
     * Loads plugins into the provided context.
     */
    public static void loadPlugins(ClientPluginContextBackend context) {
        TlaApiClient.loadPlugins(context);
    }

}
