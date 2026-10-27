package io.github.mattidragon.tlaapi.api.plugin;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;

/**
 * Represents platform specific code and events.
 *
 * Note that some methods may not work prior to that platform's initialisation so caution is advised before using this.
 */
@ApiStatus.NonExtendable
public interface ModdingPlatform {
    /**
     * Gets the current in-use modding platform.
     */
    static ModdingPlatform getInstance() {
        return TlaApi.PLATFORM;
    }

    /**
     * Gets the name of the current platform. May be either "fabric" or "neoforge".
     */
    String getPlatformName();

    /**
     * Gets a list of entrypoints for the particular key and interface type.
     */
    <T> List<T> getEntrypoints(String name, Class<T> type);

    /**
     * Gets the platform's standard measurement for fluid bucket amounts.
     */
    long getBucketAmount();

    /**
     * Adds a callback to invoke on each tick of the client. Does nothing on dedicated servers.
     */
    void addClientTickListener(Runnable handler);

    /**
     * Creates a accessible recipe manager.
     */
    RecipeManager getRecipeManager(WrapperLookup registries, ServerRecipeManager recipeManager);

    /**
     * Adds a callback to invoke when a server is created.
     */
    default void addServerAvailableListener(Consumer<MinecraftServer> handler) {
        TlaApi.SERVER_AVAILABLE_CALLBACKS.add(handler);
    }
}
