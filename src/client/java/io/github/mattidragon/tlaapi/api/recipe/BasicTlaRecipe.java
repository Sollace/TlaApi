package io.github.mattidragon.tlaapi.api.recipe;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.mattidragon.tlaapi.api.gui.GuiBuilder;
import io.github.mattidragon.tlaapi.impl.Untyped;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;

public record BasicTlaRecipe (
        TlaCategory category,
        @Nullable RegistryKey<Recipe<?>> id,
        Supplier<List<TlaIngredient>> inputs,
        Supplier<List<TlaStack>> outputs,
        Supplier<List<TlaIngredient>> catalysts
) implements TlaRecipe {

    public BasicTlaRecipe {
        inputs = Untyped.memoize(inputs);
        outputs = Untyped.memoize(outputs);
        catalysts = Untyped.memoize(catalysts);
    }

    @Override
    public @Nullable RegistryKey<Recipe<?>> getId() {
        return id;
    }

    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return null;
    }

    @Override
    public TlaCategory getCategory() {
        return category;
    }

    @Override
    public List<TlaIngredient> getInputs() {
        return inputs.get();
    }

    @Override
    public List<TlaStack> getOutputs() {
        return outputs.get();
    }

    @Override
    public List<TlaIngredient> getCatalysts() {
        return catalysts.get();
    }

    @Override
    public void buildGui(GuiBuilder builder) {
    }
}
