package io.github.mattidragon.tlaapi.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Identifiers for the built-in recipe categories that come with the game.
 *
 * Use these with {@link PluginContext#getVanillaCategory} to get reference to an instance of a category in the active plugin.
 */
@ApiStatus.Experimental
public enum BuiltInRecipeCategory {
    CRAFTING,
    SMELTING,
    BLASTING,
    SMOKING,
    CAMPFIRE_COOKING,
    STONECUTTING,
    SMITHING,
    ANVIL_REPAIRING,
    GRINDING,
    BREWING,
    FUEL,
    COMPOSTING,
    INFO,
    WORLD_INTERACTION_OTHER,
    WORLD_INTERACTION_STRIPPING,
    WORLD_INTERACTION_TILLING,
    WORLD_INTERACTION_FLATTENING,
    WORLD_INTERACTION_WAXING,
    WORLD_INTERACTION_SCRAPING,
    /**
     * Only supported by REI
     */
    WORLD_INTERACTION_OXIDIZING,
    /**
     * Only supported by REI
     */
    WORLD_INTERACTION_DEOXIDIZING,
    /**
     * Only supported by REI
     */
    WORLD_INTERACTION_BEACON_PYRAMID,
    /**
     * Only supported by REI
     */
    BEACON_PAYMENT
}
