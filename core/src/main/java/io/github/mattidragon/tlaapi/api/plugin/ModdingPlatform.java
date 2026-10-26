package io.github.mattidragon.tlaapi.api.plugin;

import java.util.List;

import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public interface ModdingPlatform {
    static ModdingPlatform getInstance() {
        return TlaApi.PLATFORM;
    }

    String getPlatformName();

    <T> List<T> getEntrypoints(String name, Class<T> type);

    long getBucketAmount();

    void registerClientTickHandler(Runnable handler);

    RecipeManager getRecipeManager(WrapperLookup registries, ServerRecipeManager recipeManager);
}
