package io.github.mattidragon.tlaapi.api.plugin;

import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.Identifier;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The main way plugins interact with the API.
 * Provided by implementations to the plugin entrypoint.
 * @see TlaApiPlugin
 */
@ImplementationsExtend
public interface PluginContext {
    /**
     * Adds a category to the recipe viewer.
     * Categories are groups of recipes that work the same way and share a tab.
     * Usually corresponds to recipe types.
     * @see TlaCategory
     */
    void addCategory(TlaCategory category);

    /**
     * Gets a vanilla category for a specified id
     * @param id
     * @see BuiltInRecipeCategory
     */
    Optional<TlaCategory> getVanillaCategory(BuiltInRecipeCategory type);

    /**
     * Gets the vanilla category type corresponding to the provided tla category.
     * This provides the reverse of the lookup done by {@linkplain PluginContext#getVanillaCategory getVanillaCategory(type)}
     * @param category
     * @see BuiltInRecipeCategory
     */
    Optional<BuiltInRecipeCategory> getVanillaCategory(TlaCategory category);

    /**
     * Adds a block as the workstation for a recipe category.
     *
     * @param category The recipe category to assign to this workstation.
     * @param workstations Ingredients matching the blocks or items that make up this workstation.
     */
    void addWorkstation(TlaCategory category, TlaIngredient... workstations);

    /**
     * Adds a recipe generator that creates recipe entries in the recipe viewer for all recipes of a given type.
     * @see TlaRecipe
     * @see #addGenerator
     */
    default <I extends RecipeInput, T extends RecipeDisplay> void addRecipeGenerator(RecipeDisplay.Serializer<T> type, Function<T, TlaRecipe> generator) {
        addRecipeMultiGenerator(type, entry -> Stream.of(generator.apply(entry)));
    }

    /**
     * Adds a recipe generator that creates recipe entries in the recipe viewer for all recipes of a given type.
     * @see TlaRecipe
     * @see #addGenerator
     */
    <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, Function<T, Stream<TlaRecipe>> generator);

    /**
     * Adds a recipe generator that can create recipe entries in the recipe viewer from any source.
     * The generator is provided the world reference for convenience.
     * @see TlaRecipe
     * @see #addRecipeGenerator
     */
    void addGenerator(Function<RecipeManager, Stream<TlaRecipe>> generator);

    /**
     * Adds a predicate to run on all current and future TlaStacks to prevent certain ones from being added to the sidebar.
     */
    void removeStacks(Predicate<TlaStack> predicate);

    /**
     * Adds a predicate to run on all current and future TlaStacks to prevent matching ones from being added to the sidebar.
     */
    default void removeStacks(TlaStack stack) {
        removeStacks(s -> s.equals(stack));
    }

    /**
     * Adds a predicate to run on all current and future recipes to prevent certain ones from being added.
     */
    void removeRecipes(Predicate<TlaRecipe> predicate);

    /**
     * Adds a predicate to run on all current and future recipes to prevent certain ones with the given identifier from being added.
     */
    default void removeRecipes(Identifier id) {
        removeRecipes(r -> r.getId() != null && id.equals(r.getId().getValue()));
    }

    /**
     * Gets a registry used for registering the default comparisons for items and blocks.
     *
     * @see TlaStackComparison
     */
    Comparisons<ItemConvertible> getItemComparisons();

    /**
     * Gets a registry used for registering the default comparisons for fluids.
     *
     * @see TlaStackComparison
     */
    Comparisons<Fluid> getFluidComparisons();

    /**
     * Provides users of TLA-api knowledge of which recipe viewer invoked the plugin.
     */
    RecipeViewer getActiveViewer();

    /**
     * Checks whether the passed object was created by this plugin context.
     */
    default boolean owns(Object o) {
        return o instanceof RecipeViewerOwned r && r.getRecipeViewer().equals(getActiveViewer());
    }
}
