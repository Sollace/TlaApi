package io.github.mattidragon.tlaapi.api.client;

import io.github.mattidragon.tlaapi.api.client.gui.GuiBuilder;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;

/**
 * Builds a GUI for a recipe.
 */
public interface TlaRecipeGui<R extends TlaRecipe> {
    /**
     * Builds the GUI for this recipe.
     * @see GuiBuilder
     */
    void buildGui(R recipe, GuiBuilder builder);
}
