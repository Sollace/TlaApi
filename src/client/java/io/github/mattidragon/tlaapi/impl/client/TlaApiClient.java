package io.github.mattidragon.tlaapi.impl.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.TlaRecipeGui;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContext;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContextBackend;
import io.github.mattidragon.tlaapi.api.client.plugin.TlaApiClientPlugin;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import io.github.mattidragon.tlaapi.impl.Untyped;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class TlaApiClient implements ClientModInitializer {
    private static final Map<TlaRecipeSerializer<?>, TlaRecipeGui<?>> GUIS = new HashMap<>();
    private static final TlaRecipeGui<?> EMPTY_GUI = (recipe, builder) -> {};

    public static void loadPlugins(ClientPluginContextBackend context) {
        GUIS.clear();
        var entrypoints = FabricLoader.getInstance().getEntrypoints("tla-api-client", TlaApiClientPlugin.class);
        var clientContext = new ClientPluginContext() {
            @Override
            public PluginContext getContext() {
                return context.getContext();
            }

            @Override
            public <T extends TlaRecipe> void registerGui(TlaRecipeSerializer<T> type, TlaRecipeGui<T> gui) {
                GUIS.put(type, gui);
            }

            @Override
            public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
                context.addClickArea(clazz, category, boundsFunction);
            }

            @Override
            public <T extends HandledScreen<?>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
                context.addScreenHandlerClickArea(clazz, category, boundsFunction);
            }

            @Override
            public <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler) {
                context.addStackDragHandler(clazz, handler);
            }

            @Override
            public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
                context.addExclusionZoneProvider(clazz, provider);
            }
        };
        TlaApi.LOGGER.info("Loading {} plugins for {}", entrypoints.size(), context);
        for (var entrypoint : entrypoints) {
             try {
                entrypoint.register(clientContext);
            } catch (RuntimeException e) {
                 throw new RuntimeException("Error while handling tla api plugin " + entrypoint, e);
            }
        }
    }

    public static <T extends TlaRecipe> TlaRecipeGui<T> getGui(T recipe) {
        return Untyped.cast(GUIS.getOrDefault(recipe.getSerializer(), EMPTY_GUI));
    }

    @Override
    public void onInitializeClient() {
        TlaApi.WORLD = () -> MinecraftClient.getInstance().world;
    }
}
