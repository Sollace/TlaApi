package io.github.mattidragon.tlaapi.api;

import io.github.mattidragon.tlaapi.api.annotation.SupportsExcept;
import io.github.mattidragon.tlaapi.api.annotation.SupportsOnly;

/**
 * Identifiers for the built-in recipe categories that come with the game.
 *
 * Use these with {@link PluginContext#getVanillaCategory} to get reference to an instance of a category in the active plugin.
 */
public enum BuiltInRecipeCategory {
    CRAFTING_SHAPELESS(false),
    CRAFTING_SHAPED(false),
    SMELTING(false),
    BLASTING(false),
    SMOKING(false),
    CAMPFIRE_COOKING(false),
    STONECUTTING(false),
    SMITHING(false),
    @SupportsExcept("eiv")
    ANVIL_REPAIRING(false),
    @SupportsExcept({"rei", "eiv"})
    GRINDING(false),
    BREWING(false),
    SMELTING_FUEL(false),
    BLASTING_FUEL(false),
    SMOKING_FUEL(false),
    @SupportsExcept({ "eiv" })
    COMPOSTING(false),
    @SupportsExcept("eiv")
    INFO(false),
    @SupportsExcept({"rei", "eiv"})
    WORLD_INTERACTION_OTHER(true),
    @SupportsExcept("eiv")
    WORLD_INTERACTION_STRIPPING(true),
    @SupportsExcept("eiv")
    WORLD_INTERACTION_TILLING(true),
    @SupportsExcept("eiv")
    WORLD_INTERACTION_FLATTENING(true),
    @SupportsExcept("eiv")
    WORLD_INTERACTION_WAXING(true),
    @SupportsExcept("eiv")
    WORLD_INTERACTION_SCRAPING(true),
    /**
     * @implNote Only supported by REI
     */
    @SupportsOnly("rei")
    WORLD_INTERACTION_OXIDIZING(true),
    /**
     * @implNote Only supported by REI
     */
    @SupportsOnly("rei")
    WORLD_INTERACTION_DEOXIDIZING(true),
    /**
     * @implNote Only supported by REI
     */
    @SupportsOnly("rei")
    WORLD_INTERACTION_BEACON_PYRAMID(true),
    /**
     * @implNote Only supported by REI
     */
    @SupportsOnly("rei")
    BEACON_PAYMENT(false),
    @SupportsOnly("eiv")
    VILLAGER_TRADING(false),
    /**
     * Recipe category for items dropped by entities
     */
    @SupportsOnly("eiv")
    LOOT(true);

    private final boolean worldInteraction;

    BuiltInRecipeCategory(boolean worldInteraction) {
        this.worldInteraction = worldInteraction;
    }

    public boolean isWorldInteraction() {
        return worldInteraction;
    }

    public boolean isBlockGuiInteraction() {
        return !isWorldInteraction();
    }

    public boolean isCrafting() {
        return this == CRAFTING_SHAPED || this == CRAFTING_SHAPELESS;
    }

    public boolean isFurnaceRecipe() {
        return this == SMOKING || this == BLASTING || this == SMELTING;
    }

    public boolean isBeaconRecipe() {
        return this == BEACON_PAYMENT || this == WORLD_INTERACTION_BEACON_PYRAMID;
    }

    public boolean isAxeAction() {
        return this == WORLD_INTERACTION_STRIPPING || this == WORLD_INTERACTION_SCRAPING;
    }

    public boolean isFuel() {
        return this == SMELTING_FUEL || this == BLASTING_FUEL || this == SMOKING_FUEL;
    }

    public int[] getSize() {
        return switch (this) {
            case CRAFTING_SHAPELESS, CRAFTING_SHAPED -> new int[] {118, 54};
            case SMELTING, BLASTING, SMOKING, CAMPFIRE_COOKING -> new int[] {82, 38};
            case STONECUTTING -> new int[] {76, 18};
            case SMITHING -> new int[] {112, 18};
            case ANVIL_REPAIRING -> new int[] {125, 18};
            case GRINDING -> new int[] {116, 56};
            case BREWING -> new int[] {120, 61};
            case BEACON_PAYMENT -> new int[] {150, 140};
            case VILLAGER_TRADING -> new int[] {140, 36};
            case LOOT -> new int[] {162, 152};
            case WORLD_INTERACTION_BEACON_PYRAMID, WORLD_INTERACTION_STRIPPING, WORLD_INTERACTION_SCRAPING,
                    WORLD_INTERACTION_TILLING, WORLD_INTERACTION_FLATTENING, WORLD_INTERACTION_WAXING,
                    WORLD_INTERACTION_OXIDIZING, WORLD_INTERACTION_DEOXIDIZING, WORLD_INTERACTION_OTHER -> new int[] {125, 18};
            case SMELTING_FUEL, BLASTING_FUEL, SMOKING_FUEL -> new int[] {144, 18};
            case COMPOSTING -> new int[] {108, 18};
            case INFO -> new int[] {144, 18};
        };
    }
}
