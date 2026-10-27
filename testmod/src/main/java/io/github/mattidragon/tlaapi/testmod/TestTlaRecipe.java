package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TestTlaRecipe(@Nullable RegistryKey<Recipe<?>> id, TestRecipeDisplay recipe) implements TlaRecipe {
    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return TestPlugin.TEST_TLA_RECIPE_SERIALIZER;
    }

    @Override
    @Nullable
    public RegistryKey<Recipe<?>> getId() {
        return id;
    }

    @Override
    public TlaCategory getCategory() {
        return TestPlugin.TEST_CATEGORY;
    }

    @Override
    public List<TlaIngredient> getInputs() {
        return List.of(TlaIngredient.ofSlotDisplay(recipe.input()));
    }

    @Override
    public List<TlaStack> getOutputs() {
        return List.of(TlaStack.of(recipe.result()));
    }
}
