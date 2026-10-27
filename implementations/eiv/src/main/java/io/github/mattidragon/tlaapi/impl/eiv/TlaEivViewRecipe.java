package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import de.crafty.eiv.common.api.recipe.IEivRecipeViewType;
import de.crafty.eiv.common.api.recipe.IEivViewRecipe;
import de.crafty.eiv.common.recipe.inventory.RecipeViewMenu.SlotFillContext;
import de.crafty.eiv.common.recipe.inventory.RecipeViewScreen;
import de.crafty.eiv.common.recipe.inventory.SlotContent;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.Untyped;
import io.github.mattidragon.tlaapi.impl.client.TlaApiClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

class TlaEivViewRecipe implements IEivViewRecipe {
    private final IEivRecipeViewType type;

    private final TlaRecipe recipe;
    private final List<SlotContent> ingredients;
    private final List<SlotContent> results;

    final EivGuiBuilder gui;

    public TlaEivViewRecipe(TlaRecipe recipe, IEivRecipeViewType type) {
        this.recipe = recipe;
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

    @Nullable
    @Override
    public List<Class<? extends HandledScreen<?>>> getTransferClasses() {
        return supportsItemTransfer() ? Untyped.cast(List.of(HandledScreen.class)) : List.of();
    }

    @Override
    public boolean canTransferToScreen(HandledScreen<?> screen) {
        return type instanceof TlaEivRecipeViewType viewType
                && viewType.placementHandlers.get(screen.getScreenHandler().getType())
                .stream()
                .anyMatch(handler -> handler.isApplicable(viewType.category, recipe));
    }

    @Override
    public boolean supportsItemTransfer() {
        return type instanceof TlaEivRecipeViewType viewType && !viewType.placementHandlers.isEmpty();
    }

    @Override
    public void mapRecipeItems(RecipeTransferMap transferMap, HandledScreen<?> screen) {
        if (type instanceof TlaEivRecipeViewType viewType) {
            viewType.placementHandlers.get(screen.getScreenHandler().getType()).stream()
                .filter(handler -> handler.isApplicable(viewType.category, recipe))
                .findFirst().ifPresent(handler -> {
                    var position = new Object() {
                        int index;
                    };
                    // TODO: Not really sure if this is valid...
                    handler.getCraftingSlots(recipe, Untyped.cast(screen.getScreenHandler()))
                            .filter(Objects::nonNull)
                            .limit(Math.min(gui.slots.size(), ingredients.size()))
                            .forEach(slot -> {
                                transferMap.linkSlots(position.index++, slot.id);
                            });
                });
        }
    }
}
