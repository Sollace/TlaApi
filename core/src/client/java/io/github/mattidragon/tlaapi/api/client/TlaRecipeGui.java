package io.github.mattidragon.tlaapi.api.client;

import io.github.mattidragon.tlaapi.api.client.gui.GuiBuilder;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.PluginsExtend;

/**
 * Builds a GUI for a recipe.
 */
@PluginsExtend
public interface TlaRecipeGui<R extends TlaRecipe> {
    /**
     * Builds the GUI for this recipe.
     * @see GuiBuilder
     */
    void buildGui(R recipe, GuiBuilder builder);
}
