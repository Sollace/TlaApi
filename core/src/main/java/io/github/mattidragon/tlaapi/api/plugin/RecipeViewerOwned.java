package io.github.mattidragon.tlaapi.api.plugin;

import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;

/**
 * Interface used to mark objects that are created (and thus "owned") by a particular recipe viewer.
 * Implementations are encouraged to apply this interface to any objects they create and return to
 * plugins so the object's ownership can be correctly traced back to them.
 */
@ImplementationsExtend
public interface RecipeViewerOwned {
    RecipeViewer getRecipeViewer();
}
