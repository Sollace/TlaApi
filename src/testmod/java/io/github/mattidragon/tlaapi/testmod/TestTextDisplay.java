package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class TestTextDisplay implements TlaRecipe {
    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return TestPlugin.TEST_TEXT_SERIALIZER;
    }

    @Override
    public TlaCategory getCategory() {
        return TestPlugin.TEST_CATEGORY;
    }
}
