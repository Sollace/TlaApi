package io.github.mattidragon.tlaapi.impl.rei;

import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.FluidComparatorRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;

public class TlaApiReiCommonPlugin implements REICommonPlugin {
    @Override
    public void preStage(PluginManager<REICommonPlugin> manager, ReloadStage stage) {
        // REI doesn't have a good reload start event, so we have to do this
        if (stage == ReloadStage.START && manager == PluginManager.getInstance()) {
            TlaApiReiPluginContext.INSTANCE.reset();
            PluginLoader.loadPlugins(TlaApiReiPluginContext.INSTANCE);
        }
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerItemComparators(registry);
    }

    @Override
    public void registerFluidComparators(FluidComparatorRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerFluidComparators(registry);
    }

    @Override
    public void registerDisplays(ServerDisplayRegistry registry) {
        TlaApiReiPluginContext.INSTANCE.registerDisplays(registry);
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(TlaDisplay.ID, TlaDisplay.SERIALIZER);
    }

    @Override
    public String toString() {
        return "REI common plugin handler";
    }
}
