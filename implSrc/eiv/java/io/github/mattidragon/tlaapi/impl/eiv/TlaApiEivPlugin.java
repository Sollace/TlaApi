package io.github.mattidragon.tlaapi.impl.eiv;

import de.crafty.eiv.common.api.IExtendedItemViewIntegration;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;

public class TlaApiEivPlugin implements IExtendedItemViewIntegration {
    @Override
    public void onIntegrationInitialize() {
        ClientPluginLoader.loadPlugins(PluginLoader.loadPlugins(RecipeViewer.EIV, bootstrapContext -> new EivImplementation(bootstrapContext)));
    }
}
