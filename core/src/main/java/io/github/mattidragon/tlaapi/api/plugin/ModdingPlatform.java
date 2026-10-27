package io.github.mattidragon.tlaapi.api.plugin;

import java.util.List;
import java.util.function.Consumer;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;

/**
 * Represents platform specific code and events.
 *
 * Note that some methods may not work prior to that platform's initialisation so caution is advised before using this.
 */
public interface ModdingPlatform {
    static ModdingPlatform getInstance() {
        return TlaApi.PLATFORM;
    }

    String getPlatformName();

    <T> List<T> getEntrypoints(String name, Class<T> type);

    long getBucketAmount();

    void addClientTickListener(Runnable handler);

    RecipeManager getRecipeManager(WrapperLookup registries, ServerRecipeManager recipeManager);

    default void addServerAvailableListener(Consumer<MinecraftServer> handler) {
        TlaApi.SERVER_AVAILABLE_CALLBACKS.add(handler);
    }
}
