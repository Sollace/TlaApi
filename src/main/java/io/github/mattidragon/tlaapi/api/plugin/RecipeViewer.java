package io.github.mattidragon.tlaapi.api.plugin;

import java.util.Locale;

/**
 * Pseudo-enum of recipe viewers. Users of TLA-Api will be given one of these to signify which recipe viewer the plugin is running under.
 * The same plugin may be called multiple times with different viewers if multiple are installed.
 */
public record RecipeViewer(String id) {
    public static final RecipeViewer EMI = new RecipeViewer("emi");
    public static final RecipeViewer REI = new RecipeViewer("rei");
    public static final RecipeViewer JEI = new RecipeViewer("jei");
    public static final RecipeViewer EIV = new RecipeViewer("eiv");

    public RecipeViewer {
        id = id.toUpperCase(Locale.ROOT);
    }
}
