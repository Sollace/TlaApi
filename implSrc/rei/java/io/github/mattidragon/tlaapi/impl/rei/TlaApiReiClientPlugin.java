package io.github.mattidragon.tlaapi.impl.rei;

import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;

public class TlaApiReiClientPlugin implements REIClientPlugin {
    @Override
    public void preStage(PluginManager<REIClientPlugin> manager, ReloadStage stage) {
        // REI doesn't have a good reload start event, so we have to do this
        if (stage == ReloadStage.START && manager == PluginManager.getClientInstance()) {
            TlaApiReiPluginContext.INSTANCE.reset();
            PluginLoader.loadPlugins(TlaApiReiPluginContext.INSTANCE);
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerCategories(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerDisplays(registry);
        TlaApiReiPluginContext.INSTANCE.registerVisibilityPredicates(registry);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerEntries(registry);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerScreens(registry);
    }
}
