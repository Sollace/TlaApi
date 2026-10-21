package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public record TestTlaRecipe(TestRecipeDisplay recipe) implements TlaRecipe {

    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return TestPlugin.TEST_TLA_RECIPE_SERIALIZER;
    }

    @Override
    public TlaCategory getCategory() {
        return TestPlugin.TEST_CATEGORY;
    }

    @Override
    public List<TlaIngredient> getInputs() {
        return List.of(TlaIngredient.ofSlotDisplay(recipe.result()));
    }

    @Override
    public List<TlaStack> getOutputs() {
        return List.of(TlaStack.of(recipe.result()));
    }
}
