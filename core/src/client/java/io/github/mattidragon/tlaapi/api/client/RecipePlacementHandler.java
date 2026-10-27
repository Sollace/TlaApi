package io.github.mattidragon.tlaapi.api.client;

import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.impl.PluginsExtend;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Provides logic for plugins to control how their recipes should be placed
 * when filling the crafting grid from the player's inventory and container slots.
 */
@PluginsExtend
@ApiStatus.Experimental
public interface RecipePlacementHandler<S extends ScreenHandler> {
    /**
     * Returns slots from the open screen handler that contain crafting resources.
     *
     * This should include the player's inventory and hotbar if they are available, and any extra storage
     * the screen provides (like a chest inventory).
     *
     * @param screen The current screen
     * @return A collection of available slots
     */
    Stream<@Nullable Slot> getInputSlots(S screen);

    /**
     * Returns slots that should have resources placed into them to craft the recipe.
     *
     * @param screen The current screen
     * @return A collection of slots
     */
    Stream<@Nullable Slot> getCraftingSlots(S screen);

    /**
     * Returns slots that should have resources placed into them to craft the recipe.
     *
     * @param screen The current screen
     * @return A collection of slots
     */
    default Stream<@Nullable Slot> getCraftingSlots(TlaRecipe recipe, S screen) {
        return getCraftingSlots(screen);
    }

    /**
     * Gets the output slot where the crafting result would appear.
     *
     * @param screen The current screen
     * @return The output slot
     */
    default @Nullable Slot getOutputSlot(S screen) {
        return null;
    }

    /**
     * Returns whether this placement handler can work with a particular recipe.
     */
    default boolean isApplicable(TlaCategory category, TlaRecipe recipe) {
        return Objects.equals(category, recipe.getCategory());
    }
}
