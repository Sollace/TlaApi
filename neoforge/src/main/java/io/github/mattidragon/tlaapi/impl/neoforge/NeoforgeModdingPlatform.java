package io.github.mattidragon.tlaapi.impl.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.github.mattidragon.tlaapi.api.plugin.ModdingPlatform;
import io.github.mattidragon.tlaapi.api.plugin.RecipeManager;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidType;

@Mod(TlaApi.DEFAULT_NAMESPACE)
public class NeoforgeModdingPlatform implements ModdingPlatform {
    public static final List<Runnable> CLIENT_TICK_HANDLERS = new ArrayList<>();

    public NeoforgeModdingPlatform(IEventBus bus, ModContainer modContainer) {
        TlaApi.PLATFORM = this;
        bus.register(this);
    }

    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    @Override
    public <T> List<T> getEntrypoints(String name, Class<T> type) {
        return ModList.get().getSortedMods().stream()
            .flatMap(container -> container.getModInfo().getConfig().<String>getConfigElement(name).stream())
            .map(entrypoint -> {
                try {
                    Class<?> entrypointType = Class.forName(entrypoint);
                    if (type.isAssignableFrom(Objects.requireNonNull(entrypointType))) {
                        return type.cast(entrypointType.getConstructor().newInstance());
                    } else {
                        TlaApi.LOGGER.warn("Could not load mod entrypoint: {}={} does not extend or implement type or interface '{}'", name, entrypoint, type);
                    }
                } catch (Throwable t) {
                    TlaApi.LOGGER.warn("Could not load mod entrypoint: {}={}", name, entrypoint, t);
                }
                return null;
            }).filter(Objects::nonNull).toList();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        TlaApi.onServerLoaded(event.getServer());
    }

    @Override
    public long getBucketAmount() {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public void addClientTickListener(Runnable handler) {
        CLIENT_TICK_HANDLERS.add(handler);
    }

    @Override
    public RecipeManager getRecipeManager(WrapperLookup registries, ServerRecipeManager recipeManager) {
        return RecipeManager.of(registries, () -> recipeManager.values().stream(), type -> recipeManager.recipeMap().getAll(type));
    }
}
