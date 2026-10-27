package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestTextDisplay implements TlaRecipe {
    private static final RegistryKey<Recipe<?>> ID = RegistryKey.of(RegistryKeys.RECIPE, Identifier.of("testmod", "test_text"));

    @Override
    @Nullable
    public RegistryKey<Recipe<?>> getId() {
        return ID;
    }

    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return TestPlugin.TEST_TEXT_SERIALIZER;
    }

    @Override
    public TlaCategory getCategory() {
        return TestPlugin.TEST_CATEGORY;
    }
}
