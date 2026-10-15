package io.github.mattidragon.tlaapi.api.plugin;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.ImplementationOnly;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

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

    public static MinecraftServer getServer() {
        return TlaApi.getServer();
    }

    public interface BootstrapFunction<C extends PluginContext> {
        C bootstrap(BootstrapContext context);
    }

    public interface BootstrapContext {
        MinecraftServer getServer();

        void registerCategory(TlaCategory category);

        void registerCategoryProvider(Function<Identifier, @Nullable TlaCategory> provider);
    }
}
