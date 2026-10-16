package io.github.mattidragon.tlaapi.api.plugin;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;
import io.github.mattidragon.tlaapi.impl.Untyped;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Provides access to regular recipes
 */
@ImplementationsExtend
public sealed interface RecipeManager permits RecipeManager.Impl {
    /**
     * Gets a stream of every recipe in this manager.
     */
    Stream<RecipeEntry<?>> values();

    /**
     * Gets a filtered stream of recipes matching a certain type.
     */
    <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeEntry<T>> allofType(RecipeType<T> type);

    /**
     * Gets the registries loaded for the current world.
     */
    WrapperLookup registries();

    static RecipeManager of(WrapperLookup registries) {
        return of(registries, Stream::empty, t -> List.of());
    }

    static RecipeManager of(World world) {
        return world instanceof ServerWorld sw ? of(sw.getRegistryManager(), sw.getRecipeManager()) : of(world.getRegistryManager());
    }

    static RecipeManager of(WrapperLookup registries, ServerRecipeManager recipeManager) {
        return of(registries, () -> recipeManager.values().stream(), recipeManager::getAllOfType);
    }

    static <I extends RecipeInput, T extends Recipe<I>> RecipeManager of(
            WrapperLookup registries,
            Supplier<Stream<RecipeEntry<?>>> supplier,
            Function<RecipeType<T>, Collection<RecipeEntry<T>>> typedLookup
    ) {
        return new Impl<>(registries, supplier, typedLookup);
    }

    final class Impl<I extends RecipeInput, T extends Recipe<I>> implements RecipeManager {
        private final WrapperLookup registries;
        private final Supplier<Stream<RecipeEntry<?>>> supplier;
        private final Function<RecipeType<T>, Collection<RecipeEntry<T>>> typedLookup;
        private Impl(WrapperLookup registries, Supplier<Stream<RecipeEntry<?>>> supplier, Function<RecipeType<T>, Collection<RecipeEntry<T>>> typedLookup) {
            this.registries = registries;
            this.supplier = supplier;
            this.typedLookup = typedLookup;
        }

        @Override
        public Stream<RecipeEntry<?>> values() {
            return supplier.get();
        }

        @Override
        public <S extends RecipeInput, V extends Recipe<S>> Stream<RecipeEntry<V>> allofType(RecipeType<V> type) {
            return Untyped.cast(typedLookup.apply(Untyped.cast(type)).stream());
        }

        @Override
        public WrapperLookup registries() {
            return registries;
        }
    }
}
