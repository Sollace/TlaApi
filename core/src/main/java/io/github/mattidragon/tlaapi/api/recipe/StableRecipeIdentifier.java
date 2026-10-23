package io.github.mattidragon.tlaapi.api.recipe;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import io.github.mattidragon.tlaapi.impl.PluginOnly;
import io.github.mattidragon.tlaapi.impl.StableIdentifierImpl;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

/**
 * Represents an identifier for a recipe that doesn't change.
 *
 * Use this for comparing recipes in cases where the recipe's ID isn't supplied or the
 * underlying mod doesn't provide IDs for its recipes.
 */
@PluginOnly
public interface StableRecipeIdentifier {
    /**
     * Gets a stable identifier for the supplied recipe
     */
    static StableRecipeIdentifier of(TlaRecipe recipe) {
        return of(recipe.getId(), recipe.getCategory(), recipe.getInputs(), recipe.getOutputs());
    }

    /**
     * Gets a stable identifier from a vanilla registry key
     */
    static StableRecipeIdentifier of(@NotNull RegistryKey<Recipe<?>> key) {
        return StableIdentifierImpl.get(key);
    }

    /**
     * Creates a stable identifier from a set of recipe characteristics
     */
    static StableRecipeIdentifier of(@Nullable RegistryKey<Recipe<?>> key, TlaCategory category, Collection<TlaIngredient> ingredients, Collection<TlaStack> results) {
        return StableIdentifierImpl.get(key, category, ingredients, results);
    }

    Pair<Optional<RegistryKey<Recipe<?>>>, Supplier<Optional<Identifier>>> getKey();

    default boolean equals(StableRecipeIdentifier other) {
        return StableIdentifierImpl.matches(this, other);
    }

    default boolean equals(@Nullable Identifier id) {
        return id != null && StableIdentifierImpl.matches(this, id);
    }

    default boolean equals(@Nullable RegistryKey<?> key) {
        return key != null && equals(key.getValue());
    }
}
