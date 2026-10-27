package io.github.mattidragon.tlaapi.api.plugin;

import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
import io.github.mattidragon.tlaapi.api.annotation.SupportsExcept;
import io.github.mattidragon.tlaapi.api.gui.RecipePlacementHandler;
import io.github.mattidragon.tlaapi.api.recipe.StableRecipeIdentifier;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus;

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
        addRecipeGenerator(type, (id, display) -> generator.apply(display));
    }

    /**
     * Adds a recipe generator that creates recipe entries in the recipe viewer for all recipes of a given type.
     * @see TlaRecipe
     * @see #addGenerator
     */
    default <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, Function<T, Stream<TlaRecipe>> generator) {
        addRecipeMultiGenerator(type, (id, display) -> generator.apply(display));
    }

    /**
     * Adds a recipe generator that creates recipe entries in the recipe viewer for all recipes of a given type.
     * @see TlaRecipe
     * @see #addGenerator
     */
    default <I extends RecipeInput, T extends RecipeDisplay> void addRecipeGenerator(RecipeDisplay.Serializer<T> type, UnaryRecipeGenerator<I, T> generator) {
        addRecipeMultiGenerator(type, generator.streamed());
    }

    /**
     * Adds a recipe generator that creates recipe entries in the recipe viewer for all recipes of a given type.
     * @see TlaRecipe
     * @see #addGenerator
     */
    <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, StreamedRecipeGenerator<I, T> generator);

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
     *
     * @implNote The identifier can be the registry id of the recipe, or any other identifier.
     *           What is used is not always going to be the same for every recipe mod.
     *           It's sometimes better to match recipes on type, inputs, and outputs rather than relying on its ID.
     *           StableRecipeIdentifier can be used to make this easier to do.
     * @see StableRecipeIdentifier
     */
    default void removeRecipes(Identifier id) {
        removeRecipes(r -> r.getStableId().equals(id));
    }

    /**
     * Adds a predicate to run on all current and future recipes to prevent certain ones with the given identifier from being added.
     *
     * @see StableRecipeIdentifier
     */
    default void removeRecipes(StableRecipeIdentifier id) {
        removeRecipes(r -> r.getStableId().equals(id));
    }

    /**
     * Gets a registry used for registering the default comparisons for items and blocks.
     *
     * @see TlaStackComparison
     * @implNote EIV does not support custom comparisons so any registrations done through here will be ignored.
     */
    @SupportsExcept({ "eiv" })
    Comparisons<ItemConvertible> getItemComparisons();

    /**
     * Adds a handler for placing and auto-crafting recipes.
     * @param screenType The type of the screen handler for which this applies.
     * @param category The category which should be opened.
     * @param placementHandler Handler to define which slots to use when placing a recipe
     */
    @ApiStatus.Experimental
    <C extends ScreenHandler> void addPlacementHandler(ScreenHandlerType<C> screenType, TlaCategory category, RecipePlacementHandler<C> placementHandler);

    /**
     * Gets a registry used for registering the default comparisons for fluids.
     *
     * @see TlaStackComparison
     * @implNote EIV does not support custom comparisons so any registrations done through here will be ignored.
     */
    @SupportsExcept({ "eiv" })
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

    @FunctionalInterface
    interface UnaryRecipeGenerator<I extends RecipeInput, T extends RecipeDisplay> {
        TlaRecipe apply(RegistryKey<Recipe<?>> id, T display);

        default StreamedRecipeGenerator<I, T> streamed() {
            return (id, display) -> Stream.of(apply(id, display));
        }
    }

    @FunctionalInterface
    interface StreamedRecipeGenerator<I extends RecipeInput, T extends RecipeDisplay> {
        Stream<TlaRecipe> apply(RegistryKey<Recipe<?>> id, T display);
    }
}
