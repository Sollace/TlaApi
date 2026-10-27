package io.github.mattidragon.tlaapi.impl.jei;

import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContextBackend;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginLoader;
import io.github.mattidragon.tlaapi.api.gui.RecipePlacementHandler;
import io.github.mattidragon.tlaapi.api.plugin.Comparisons;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeManager;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewerOwned;
import io.github.mattidragon.tlaapi.api.recipe.BasicTlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.api.recipe.TlaStackComparison;
import io.github.mattidragon.tlaapi.impl.Untyped;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.common.platform.IPlatformFluidHelperInternal;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

class TlaApiJeiPluginContext implements PluginContext, ClientPluginContextBackend {
    final Map<Identifier, BuiltInTlaCategory> builtInCategories = new HashMap<>();
    final List<TlaCategory> categories = new ArrayList<>();
    final List<Function<RecipeEntry<?>, Stream<TlaRecipe>>> recipeFunctions = new ArrayList<>();
    final List<Function<RecipeManager, Stream<TlaRecipe>>> recipeGenerators = new ArrayList<>();
    final Multimap<TlaCategory, TlaIngredient> workstations = HashMultimap.create();
    final List<ClickAreaTuple<?>> clickAreas = new ArrayList<>();
    final List<GhostHandler<?>> ghostHandlers = new ArrayList<>();
    final List<ScreenSizeProvider<?>> sizeProviders = new ArrayList<>();
    final List<ExclusionZoneProvider<?>> exclusionZoneProviders = new ArrayList<>();
    final List<Consumer<ISubtypeRegistration>> subTypeProviders = new ArrayList<>();
    final List<TransferHandler<?, ?>> transferHandlers = new ArrayList<>();

    private Predicate<TlaStack> stackHideFilter = Predicates.alwaysFalse();
    private Predicate<TlaRecipe> recipeHideFilter = Predicates.alwaysFalse();

    final Map<TlaCategory, TlaRecipeCategory> preparedCategories = new HashMap<>();

    final Comparisons<ItemConvertible> itemComparisons = new Comparisons<>() {
        @Override
        public void register(ItemConvertible item, TlaStackComparison comparison) {
            subTypeProviders.add(registration -> registration.registerSubtypeInterpreter(item.asItem(), (stack, context) -> {
                return comparison.hashFunction().hash(TlaStack.of(stack)) + "";
            }));
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.JEI;
        }
    };
    final Comparisons<Fluid> fluidComparisons = new Comparisons<>() {
        @Override
        public void register(Fluid fluid, TlaStackComparison comparison) {
            subTypeProviders.add(registration -> {
                IPlatformFluidHelperInternal<Object> helpers = Untyped.cast(TlaApiJeiPlugin.JEI_HELPERS.getPlatformFluidHelper());
                registration.registerSubtypeInterpreter(helpers.getFluidIngredientType(), fluid, (ingredient, context) -> {
                    return comparison.hashFunction().hash(TlaStack.bucketOf(helpers.getFluidIngredientType().getBase(ingredient), helpers.getComponentsPatch(ingredient))) + "";
                });
            });
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.JEI;
        }
    };

    PluginLoader.BootstrapContext bootstrapContext;

    TlaApiJeiPluginContext() {
        ClientPluginLoader.loadPlugins(PluginLoader.loadPlugins(RecipeViewer.JEI, bootstrapContext -> {
            this.bootstrapContext = bootstrapContext;
            this.bootstrapContext.registerCategoryProvider(builtInCategories::get);
            return this;
        }));
    }

    public boolean shouldDisplayRecipe(@Nullable TlaRecipe recipe) {
        return recipe == null || !recipeHideFilter.test(recipe);
    }

    public boolean shouldDisplayIngredient(@Nullable TlaStack stack) {
        return stack == null || stack.isEmpty() || !stackHideFilter.test(stack);
    }

    @Override
    public PluginContext getContext() {
        return this;
    }

    @Override
    public void addCategory(TlaCategory category) {
        if (bootstrapContext.getOwnerOf(category) != null) {
            return;
        }
        categories.add(category);
        bootstrapContext.registerCategory(category);
    }

    public TlaCategory lookupCategory(IRecipeCategory<?> category) {
        if (category instanceof TlaRecipeCategory c) {
            return c.category;
        }

        var tlaCategory = bootstrapContext.lookupCategory(category.getRecipeType().getUid());

        if (tlaCategory != null) {
            return tlaCategory;
        }

        // TODO: Proper reverse lookup
        return new BuiltInTlaCategory(BuiltInRecipeCategory.INFO,
                category.getRecipeType(), category.getRecipeType().getUid(), BuiltInRecipeCategory.INFO.getSize()
        );
    }

    public IRecipeCategory<?> lookupCategory(TlaCategory category, IJeiHelpers helpers) {
        var jeiCategory = preparedCategories.get(category);

        if (jeiCategory != null) {
            return jeiCategory;
        }

        helpers.getRecipeType(category.getId());
        return null;
    }

    @Override
    public Optional<TlaCategory> getVanillaCategory(BuiltInRecipeCategory type) {
        return Optional.ofNullable(getRecipeType(type)).map(jeiCategory -> builtInCategories.computeIfAbsent(jeiCategory.getUid(), id -> new BuiltInTlaCategory(type, jeiCategory, id, type.getSize())));
    }

    private @Nullable IRecipeType<?> getRecipeType(BuiltInRecipeCategory type) {
        return switch (type) {
            case CRAFTING_SHAPELESS, CRAFTING_SHAPED -> RecipeTypes.CRAFTING;
            case SMELTING -> RecipeTypes.SMELTING;
            case BLASTING -> RecipeTypes.BLASTING;
            case SMOKING -> RecipeTypes.SMOKING;
            case CAMPFIRE_COOKING -> RecipeTypes.CAMPFIRE_COOKING;
            case STONECUTTING -> RecipeTypes.STONECUTTING;
            case SMITHING -> RecipeTypes.SMITHING;
            case ANVIL_REPAIRING -> RecipeTypes.ANVIL;
            case GRINDING -> RecipeTypes.GRINDSTONE;
            case BREWING -> RecipeTypes.BREWING;
            case BEACON_PAYMENT -> null;
            case VILLAGER_TRADING -> null;
            case LOOT -> null;
            case WORLD_INTERACTION_BEACON_PYRAMID, WORLD_INTERACTION_STRIPPING, WORLD_INTERACTION_SCRAPING,
                WORLD_INTERACTION_TILLING, WORLD_INTERACTION_FLATTENING, WORLD_INTERACTION_WAXING,
                WORLD_INTERACTION_OXIDIZING, WORLD_INTERACTION_DEOXIDIZING, WORLD_INTERACTION_OTHER -> null;//RecipeTypes.WORLD_INTERACTION;
            case SMELTING_FUEL -> RecipeTypes.SMELTING_FUEL;
            case BLASTING_FUEL -> RecipeTypes.BLASTING_FUEL;
            case SMOKING_FUEL -> RecipeTypes.SMOKING_FUEL;
            case COMPOSTING -> RecipeTypes.COMPOSTING;
            case INFO -> RecipeTypes.INFORMATION;
        };
    }

    @Override
    public Optional<BuiltInRecipeCategory> getVanillaCategory(TlaCategory category) {
        if (category instanceof BuiltInTlaCategory builtin) {
            return Optional.of(builtin.category());
        }
        var owner = bootstrapContext.getOwnerOf(category);
        return owner == getActiveViewer() ? Optional.empty() : bootstrapContext.getPluginContext(owner).flatMap(ctx -> ctx.getVanillaCategory(category));
    }

    @Override
    public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
        this.workstations.putAll(category, Arrays.asList(workstations));
    }

    @Override
    public <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, StreamedRecipeGenerator<I, T> generator) {
        recipeFunctions.add(entry -> entry.value().getDisplays().stream()
                    .filter(display -> display.serializer() == type)
                    .map(Untyped::<RecipeDisplay, T>cast)
                    .flatMap(display -> generator.apply(entry.id(), display)));
    }

    @Override
    public void addGenerator(Function<RecipeManager, Stream<TlaRecipe>> generator) {
        recipeGenerators.add(generator);
    }

    @Override
    public void removeStacks(Predicate<TlaStack> predicate) {
        stackHideFilter = stackHideFilter.or(predicate);
    }

    @Override
    public void removeRecipes(Predicate<TlaRecipe> predicate) {
        recipeHideFilter = recipeHideFilter.or(predicate);
    }

    @Override
    public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        // JEI doesn't support click areas for non-handled screens
    }

    @Override
    public <C extends ScreenHandler, T extends HandledScreen<C>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        clickAreas.add(new ClickAreaTuple<>(clazz, category, boundsFunction));
    }

    @Override
    public <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler) {
        ghostHandlers.add(new GhostHandler<>(clazz, handler));
    }

    @Override
    public <T extends Screen> void addScreenSizeProvider(Class<T> clazz, Function<T, TlaBounds> provider) {
        sizeProviders.add(new ScreenSizeProvider<>(clazz, provider));
    }

    @Override
    public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
        if (!HandledScreen.class.isAssignableFrom(clazz)) return; // JEI only supports exclusion zones on handled screens
        exclusionZoneProviders.add(new ExclusionZoneProvider<>(Untyped.cast(clazz), Untyped.cast(provider)));
    }

    @Override
    public <C extends ScreenHandler> void addPlacementHandler(ScreenHandlerType<C> screenType, TlaCategory category, RecipePlacementHandler<C> placementHandler) {
        transferHandlers.add(new TransferHandler<>(screenType, category, placementHandler));
    }

    @Override
    public Comparisons<ItemConvertible> getItemComparisons() {
        return itemComparisons;
    }

    @Override
    public Comparisons<Fluid> getFluidComparisons() {
        return fluidComparisons;
    }

    @Override
    public RecipeViewer getActiveViewer() {
        return RecipeViewer.JEI;
    }

    @Nullable
    public <T> TlaRecipe convertRecipe(IRecipeCategory<T> category, T recipe, IPlatformFluidHelper<?> helper, IRecipeManager recipeManager) {
        return switch (recipe) {
            case TlaRecipe t -> t;
            case PreparedRecipe p -> p.getRecipe();
            default -> reconstructExternalRecipe(category, recipe, helper, recipeManager);
        };
    }

    private <T> TlaRecipe reconstructExternalRecipe(IRecipeCategory<T> category, T recipe, IPlatformFluidHelper<?> helper, IRecipeManager recipeManager) {
        var ingredientSupplier = recipeManager.getRecipeIngredients(category, recipe);
        var id = category.getRegistryName(recipe);
        return new BasicTlaRecipe(lookupCategory(category), id == null ? null : RegistryKey.of(RegistryKeys.RECIPE, id), () -> {
            return ingredientSupplier.getIngredients(RecipeIngredientRole.INPUT).stream()
                .map(input -> JeiUtils.convertStack(helper, input).asIngredient())
                .toList();
        }, List::of, List::of);
    }

    record ClickAreaTuple<T extends HandledScreen<?>>(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {}
    record GhostHandler<T extends Screen>(Class<T> clazz, StackDragHandler<T> handler) {}
    record ScreenSizeProvider<T extends Screen>(Class<T> clazz, Function<T, TlaBounds> provider) {}
    record ExclusionZoneProvider<T extends HandledScreen<?>>(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {}
    record TransferHandler<C extends ScreenHandler, T extends HandledScreen<C>>(ScreenHandlerType<C> type, TlaCategory category, RecipePlacementHandler<C> placementHandler) {}
    record BuiltInTlaCategory(BuiltInRecipeCategory category, IRecipeType<?> type, Identifier id, int[] size) implements TlaCategory, RecipeViewerOwned {
        static final CategoryIcon ICON = CategoryIcon.stack(TlaStack.empty());

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        public int getDisplayHeight() {
            return size[1];
        }

        @Override
        public int getDisplayWidth() {
            return size[0];
        }

        @Override
        public CategoryIcon getIcon() {
            return ICON;
        }

        @Override
        public CategoryIcon getSimpleIcon() {
            return ICON;
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.JEI;
        }
    }
}
