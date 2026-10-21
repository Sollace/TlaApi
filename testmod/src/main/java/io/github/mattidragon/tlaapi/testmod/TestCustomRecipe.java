package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TestCustomRecipe implements TlaRecipe {
    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return TestPlugin.TEST_CUSTOM_SERIALIZER;
    }

    @Override
    public TlaCategory getCategory() {
        return TestPlugin.TEST_CATEGORY;
    }

    @Override
    public @Nullable RegistryKey<Recipe<?>> getId() {
        return null;
    }

    @Override
    public List<TlaIngredient> getInputs() {
        return List.of(TlaIngredient.ofStacks(TlaStack.of(Items.DIAMOND).withAmount(4)));
    }

    @Override
    public List<TlaStack> getOutputs() {
        return List.of(TlaStack.of(Items.EMERALD).withAmount(3));
    }

    @Override
    public List<TlaIngredient> getCatalysts() {
        return List.of(TlaIngredient.ofStacks(TlaStack.of(Items.NETHER_STAR)));
    }
}
