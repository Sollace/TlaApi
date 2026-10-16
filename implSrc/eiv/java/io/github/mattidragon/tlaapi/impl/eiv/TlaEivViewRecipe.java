package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.List;

import de.crafty.eiv.common.api.recipe.IEivRecipeViewType;
import de.crafty.eiv.common.api.recipe.IEivViewRecipe;
import de.crafty.eiv.common.recipe.inventory.RecipeViewMenu.SlotFillContext;
import de.crafty.eiv.common.recipe.inventory.RecipeViewScreen;
import de.crafty.eiv.common.recipe.inventory.SlotContent;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.client.TlaApiClient;
import net.minecraft.client.gui.DrawContext;

class TlaEivViewRecipe implements IEivViewRecipe {
    private final IEivRecipeViewType type;

    private final List<SlotContent> ingredients;
    private final List<SlotContent> results;

    final EivGuiBuilder gui;

    public TlaEivViewRecipe(TlaRecipe recipe, IEivRecipeViewType type) {
        this.type = type instanceof TlaEivRecipeViewType t ? t.bindTo(this) : type;
        this.ingredients = recipe.getInputs().stream().map(EivUtil::convertIngredient).toList();
        this.results = recipe.getOutputs().stream().map(EivUtil::convertStack).toList();

        gui = new EivGuiBuilder(type.getDisplayWidth(), type.getDisplayHeight());

        TlaApiClient.getGui(recipe).buildGui(recipe, gui);
    }

    @Override
    public void bindSlots(SlotFillContext context) {
        for (int i = 0; i < Math.min(gui.slots.size(), type.getSlotCount()); i++) {
            context.bindOptionalSlot(i, EivUtil.convertIngredient(gui.slots.get(i).ingredient), gui.slots.get(i));
        }
    }

    @Override
    public void renderRecipe(RecipeViewScreen screen, DrawContext context, int mouseX, int mouseY, float tickDelta) {
        gui.elements.forEach(element -> {
            element.render(context, mouseX, mouseY, tickDelta);
        });
    }

    @Override
    public void tick() {
        gui.tickers.forEach(Runnable::run);
    }

    @Override
    public List<SlotContent> getIngredients() {
        return ingredients;
    }

    @Override
    public List<SlotContent> getResults() {
        return results;
    }

    @Override
    public IEivRecipeViewType getViewType() {
        return type;
    }
}
