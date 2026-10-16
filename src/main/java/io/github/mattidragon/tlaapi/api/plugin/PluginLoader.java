package io.github.mattidragon.tlaapi.api.plugin;

import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.ImplementationOnly;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Provides implementations with the ability to load plugins.
 */
@ImplementationOnly
public final class PluginLoader {
    private PluginLoader() {}

    /**
     * Loads plugins into the provided context.
     */
    public static <C extends PluginContext> C loadPlugins(RecipeViewer viewer, BootstrapFunction<C> implementation) {
        return TlaApi.loadPlugins(viewer, implementation);
    }

    public static TlaCategory getCategory(Identifier id) {
        return TlaApi.getCategory(id);
    }

    public static World getWorld() {
        return TlaApi.getWorld();
    }

    public interface BootstrapFunction<C extends PluginContext> {
        C bootstrap(BootstrapContext context);
    }

    public interface BootstrapContext {
        MinecraftServer getServer();

        /**
         * Gets the recipe viewer that created the provided object if one exists
         */
        @Nullable RecipeViewer getOwnerOf(@Nullable Object o);

        /**
         * Gets the plugin context of a particular viewer if it has been loaded.
         * If the viewer is the same as the current context, will return that.
         */
        Optional<PluginContext> getPluginContext(@Nullable RecipeViewer viewer);

        void registerCategory(TlaCategory category);

        void registerCategoryProvider(Function<Identifier, @Nullable TlaCategory> provider);
    }
}
