package io.github.mattidragon.tlaapi.impl;

import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewerOwned;
import io.github.mattidragon.tlaapi.api.plugin.TlaApiPlugin;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader.BootstrapFunction;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlaApi implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TLA Api");

    public static final String DEFAULT_NAMESPACE = "tla-api";

    private static final Map<RecipeViewer, Bootsrap> VIEWER_REGISTRY = new HashMap<>();

    private static final AtomicReference<MinecraftServer> SERVER = new AtomicReference<>(null);
    public static Supplier<World> WORLD = () -> Objects.requireNonNull(SERVER.get()).getOverworld();

    public static Identifier id(String path) {
        return Identifier.of(DEFAULT_NAMESPACE, path);
    }

    public static TlaCategory getCategory(Identifier id) {
        synchronized (VIEWER_REGISTRY) {
            return VIEWER_REGISTRY.values().stream()
                    .map(viewer -> viewer.getCategory(id))
                    .filter(Objects::nonNull)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("No mapping available for unregistered recipe category: " + id));
        }
    }

    public static World getWorld() {
        var server = SERVER.get();
        return server != null ? server.getOverworld() : WORLD.get();
    }

    public static <C extends PluginContext> C loadPlugins(RecipeViewer viewer, BootstrapFunction<C> implementation) {
        var bootstrap = new Bootsrap(viewer, new HashMap<>(), new HashSet<>(), new AtomicReference<>(null));
        var context = implementation.bootstrap(bootstrap);
        bootstrap.pluginContext().set(context);
        var entrypoints = FabricLoader.getInstance().getEntrypoints("tla-api", TlaApiPlugin.class);
        LOGGER.info("Loading {} plugins for {}", entrypoints.size(), context);
        for (var entrypoint : entrypoints) {
             try {
                entrypoint.register(context);
            } catch (RuntimeException e) {
                 throw new RuntimeException("Error while handling tla api plugin " + entrypoint, e);
            }
        }

        synchronized (VIEWER_REGISTRY) {
            VIEWER_REGISTRY.put(viewer, bootstrap);
        }

        return context;
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(SERVER::set);
    }

    private record Bootsrap(
            RecipeViewer viewer,
            Map<Identifier, TlaCategory> categories,
            Set<Function<Identifier, TlaCategory>> categoryLookups,
            AtomicReference<PluginContext> pluginContext
    ) implements PluginLoader.BootstrapContext {
        @Override
        public void registerCategoryProvider(Function<Identifier, @Nullable TlaCategory> provider) {
            categoryLookups.add(provider);
        }

        @Nullable
        @Override
        public RecipeViewer getOwnerOf(@Nullable Object o) {
            return o instanceof RecipeViewerOwned r ? r.getRecipeViewer() : null;
        }

        @Override
        public Optional<PluginContext> getPluginContext(@Nullable RecipeViewer viewer) {
            return viewer == null ? Optional.empty() : viewer == viewer() ? Optional.ofNullable(pluginContext.get())
                    : Optional.ofNullable(VIEWER_REGISTRY.get(viewer))
                    .map(Bootsrap::pluginContext)
                    .map(AtomicReference::get);
        }

        @Override
        public void registerCategory(TlaCategory category) {
            categories.put(category.getId(), category);
        }

        public TlaCategory getCategory(Identifier id) {
            TlaCategory category = categories.get(id);
            return category != null ? category : categoryLookups.stream()
                    .map(lookup -> lookup.apply(id))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public World getWorld() {
            return TlaApi.getWorld();
        }

        @Nullable
        @Override
        public TlaCategory lookupCategory(Identifier id) {
            return getCategory(id);
        }
    }
}