package io.github.mattidragon.tlaapi.api.client.plugin;

import io.github.mattidragon.tlaapi.api.client.TlaRecipeGui;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;

public interface ClientPluginContext extends ClientPluginContextBackend {
    /**
     * Adds a GUI function to be called when a recipe belonging to the specified category needs to be drawn on the client.
     *
     * @param type  The recipe type represented by its serializer.
     * @param gui   A function to call to render the GUI elements for a recipe of this type.
     */
    <T extends TlaRecipe> void registerGui(TlaRecipeSerializer<T> type, TlaRecipeGui<T> gui);
}
