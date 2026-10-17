package io.github.mattidragon.tlaapi.impl.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.client.TlaApiClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class TlaEmiRecipe implements EmiRecipe {
    private final TlaRecipe recipe;
    private final EmiRecipeCategory category;

    public TlaEmiRecipe(TlaRecipe recipe, EmiRecipeCategory category) {
        this.recipe = recipe;
        this.category = category;
    }

    public TlaRecipe getUnderlying() {
        return recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public @Nullable Identifier getId() {
        return recipe.getId() == null ? null : recipe.getId().getValue();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return recipe.getInputs().stream().map(EmiUtils::convertIngredient).toList();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return recipe.getCatalysts().stream().map(EmiUtils::convertIngredient).toList();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return recipe.getOutputs().stream().map(EmiUtils::convertStack).toList();
    }

    @Override
    public int getDisplayWidth() {
        return recipe.getCategory().getDisplayWidth();
    }

    @Override
    public int getDisplayHeight() {
        return recipe.getCategory().getDisplayHeight();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        TlaApiClient.getGui(recipe).buildGui(recipe, new EmiGuiBuilder(this, widgets));
    }
}
