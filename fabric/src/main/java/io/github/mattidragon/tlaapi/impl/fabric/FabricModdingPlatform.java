package io.github.mattidragon.tlaapi.impl.fabric;

import java.util.List;
import java.util.function.Consumer;

import io.github.mattidragon.tlaapi.api.plugin.ModdingPlatform;
import io.github.mattidragon.tlaapi.api.plugin.RecipeManager;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class FabricModdingPlatform implements ModdingPlatform, ModInitializer {
    public static Consumer<Runnable> CLIENT_TICK_HANDLERS = handler -> {};

    public FabricModdingPlatform() {
        // We set it before the actual initialise event so the value is available when needed
        TlaApi.PLATFORM = this;
    }

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public <T> List<T> getEntrypoints(String name, Class<T> type) {
        return FabricLoader.getInstance().getEntrypoints(name, type);
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(TlaApi.SERVER::set);
    }

    @Override
    public long getBucketAmount() {
        return FluidConstants.BUCKET;
    }

    @Override
    public void registerClientTickHandler(Runnable handler) {
        CLIENT_TICK_HANDLERS.accept(handler);
    }

    @Override
    public RecipeManager getRecipeManager(WrapperLookup registries, ServerRecipeManager recipeManager) {
        return RecipeManager.of(registries, () -> recipeManager.values().stream(), recipeManager::getAllOfType);
    }
}
