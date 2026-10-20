package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import de.crafty.eiv.common.api.recipe.IEivRecipeViewType;
import de.crafty.eiv.common.api.recipe.IEivServerRecipe;
import de.crafty.eiv.common.api.recipe.ItemView;
import de.crafty.eiv.common.builtin.blasting.BlastingViewType;
import de.crafty.eiv.common.builtin.brewing.BrewingViewType;
import de.crafty.eiv.common.builtin.burning.BurningViewType;
import de.crafty.eiv.common.builtin.campfire.CampfireViewType;
import de.crafty.eiv.common.builtin.entity.EntityViewType;
import de.crafty.eiv.common.builtin.shaped.CraftingViewType;
import de.crafty.eiv.common.builtin.shapeless.ShapelessViewType;
import de.crafty.eiv.common.builtin.smelting.SmeltingViewType;
import de.crafty.eiv.common.builtin.smithing.SmithingViewType;
import de.crafty.eiv.common.builtin.smoking.SmokingViewType;
import de.crafty.eiv.common.builtin.stonecutting.StonecutterViewType;
import de.crafty.eiv.common.builtin.villager.VillagerViewType;
import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContextBackend;
import io.github.mattidragon.tlaapi.api.plugin.Comparisons;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeManager;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewerOwned;
import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.BuiltInCategoryRegistry;
import io.github.mattidragon.tlaapi.impl.Untyped;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.RecipeDisplay.Serializer;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.Identifier;

class EivImplementation implements PluginContext, ClientPluginContextBackend {
    public static EivImplementation INSTANCE;

    public static final Codec<IEivRecipeViewType> RECIPE_TYPE_CODEC = Identifier.CODEC.xmap(
            id -> INSTANCE.lookupCategory(id),
            IEivRecipeViewType::getId
    );

    private static final BuiltInCategoryRegistry<IEivRecipeViewType> VANILLA_CATEGORIES = new BuiltInCategoryRegistry<>() {
        @Override
        protected @Nullable IEivRecipeViewType getValue(BuiltInRecipeCategory type) {
            return switch (type) {
                case CRAFTING_SHAPED -> new CraftingViewType();
                case CRAFTING_SHAPELESS -> new ShapelessViewType();
                case SMELTING -> new SmeltingViewType();
                case BLASTING -> new BlastingViewType();
                case SMOKING -> new SmokingViewType();
                case CAMPFIRE_COOKING -> new CampfireViewType();
                case STONECUTTING -> new StonecutterViewType();
                case SMITHING -> new SmithingViewType();
                case BREWING -> new BrewingViewType();
                case VILLAGER_TRADING -> new VillagerViewType();
                case SMELTING_FUEL, BLASTING_FUEL, SMOKING_FUEL -> new BurningViewType();
                case LOOT -> new EntityViewType();
                case ANVIL_REPAIRING, BEACON_PAYMENT, COMPOSTING, GRINDING, INFO,
                    WORLD_INTERACTION_BEACON_PYRAMID,
                    WORLD_INTERACTION_OTHER, WORLD_INTERACTION_STRIPPING, WORLD_INTERACTION_TILLING, WORLD_INTERACTION_FLATTENING,
                    WORLD_INTERACTION_WAXING, WORLD_INTERACTION_SCRAPING,
                    WORLD_INTERACTION_OXIDIZING, WORLD_INTERACTION_DEOXIDIZING -> null;
            };
        }

        @Override
        protected BuiltInRecipeCategory getDistinctKey(BuiltInRecipeCategory type) {
            return switch (type) {
                case SMELTING_FUEL, BLASTING_FUEL, SMOKING_FUEL -> BuiltInRecipeCategory.SMELTING_FUEL;
                default -> type;
            };
        }
    };

    private static final Comparisons<?> COMPARISONS = Comparisons.noop(RecipeViewer.EIV);
    private PluginLoader.BootstrapContext bootstrapContext;

    private final Map<Identifier, TlaEivRecipeViewType> categories = new HashMap<>();
    private final Map<Identifier, BuiltInCategory> builtInCategories = new HashMap<>();
    private final List<Function<RecipeManager, Stream<TlaRecipe>>> serverRecipeGenerators = new ArrayList<>();
    private final List<Pair<Serializer<?>, Function<?, Stream<TlaRecipe>>>> typedServerRecipeGenerators = new ArrayList<>();

    public EivImplementation(PluginLoader.BootstrapContext bootstrapContext) {
        INSTANCE = this;
        this.bootstrapContext = bootstrapContext;
        bootstrapContext.registerCategoryProvider(builtInCategories::get);

        addGenerator(this::provideTypedServerRecipes);

        ItemView.addRecipeProvider(this::provideServerRecipes);
        ItemView.addReloadCallback(this::onReload);
        ItemView.registerRecipeWrapper(TlaEivRecipe.TYPE, TlaEivRecipe::getRecipeViews);
    }

    private void onReload() {
        // TODO: Register item sensitives/comparisons here
    }

    @Override
    public PluginContext getContext() {
        return this;
    }

    @Override
    public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        // Not supported by EIV
    }

    @Override
    public <T extends HandledScreen<?>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        // Not supported by EIV
    }

    @Override
    public <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler) {
        // Not supported by EIV
    }

    @Override
    public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
        // Not supported by EIV
    }

    public IEivRecipeViewType lookupCategory(Identifier id) {
        var eivCategory = categories.get(id);
        if (eivCategory != null) {
            return eivCategory;
        }

        var builtInCategory = builtInCategories.get(id);
        if (builtInCategory != null) {
            return builtInCategory.viewType();
        }

        return lookupCategory(Objects.requireNonNull(bootstrapContext.lookupCategory(id)));
    }

    public IEivRecipeViewType lookupCategory(TlaCategory category) {
        if (category instanceof BuiltInCategory builtIn) {
            return builtIn.viewType();
        }

        var eivCategory = categories.get(category.getId());
        if (eivCategory != null) {
            return eivCategory;
        }

        return getVanillaCategory(category)
                .map(type -> VANILLA_CATEGORIES.get(type))
                .orElse(null);
    }

    @Override
    public void addCategory(TlaCategory category) {
        if (bootstrapContext.getOwnerOf(category) != null) {
            return;
        }
        var eivCategory = new TlaEivRecipeViewType(category);
        categories.put(category.getId(), eivCategory);
        bootstrapContext.registerCategory(category);
    }

    @Override
    public Optional<TlaCategory> getVanillaCategory(BuiltInRecipeCategory type) {
        return Optional.ofNullable(VANILLA_CATEGORIES.get(type)).map(viewType -> builtInCategories.computeIfAbsent(viewType.getId(), id -> new BuiltInCategory(type, viewType)));
    }

    @Override
    public Optional<BuiltInRecipeCategory> getVanillaCategory(TlaCategory category) {
        if (category instanceof BuiltInCategory builtIn) {
            return Optional.of(builtIn.category());
        }

        var viewer = bootstrapContext.getOwnerOf(category);
        return viewer == getActiveViewer() ? Optional.empty() : bootstrapContext.getPluginContext(viewer).flatMap(ctx -> ctx.getVanillaCategory(category));
    }

    @Override
    public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
        Objects.requireNonNull(categories.get(category.getId()), "Category " + category.getId() + " was not registered")
            .craftReferences
            .addAll(List.of(workstations).stream().flatMap(EivUtil::getAsItemStacks).distinct().toList());
    }

    @Override
    public <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(Serializer<T> type, Function<T, Stream<TlaRecipe>> generator) {
        typedServerRecipeGenerators.add(new Pair<>(type, generator));
    }

    private Stream<TlaRecipe> provideTypedServerRecipes(RecipeManager recipeManager) {
        return recipeManager.values()
                .flatMap(recipe -> recipe.value().getDisplays().stream())
                .flatMap(display -> typedServerRecipeGenerators.stream()
                            .filter(generator -> generator.getFirst() == display.serializer())
                            .map(generator -> (TlaRecipe)generator.getSecond().apply(Untyped.cast(display))));
    }

    private void provideServerRecipes(List<IEivServerRecipe> output) {
        RecipeManager manager = RecipeManager.of(bootstrapContext.getWorld());

        serverRecipeGenerators.stream()
            .flatMap(generator -> generator.apply(manager))
            .map(recipe -> new TlaEivRecipe(recipe, lookupCategory(recipe.getCategory())))
            .forEach(output::add);
    }

    @Override
    public void addGenerator(Function<RecipeManager, Stream<TlaRecipe>> generator) {
        serverRecipeGenerators.add(generator);
    }

    @Override
    public void removeStacks(Predicate<TlaStack> predicate) {
        // Not supported by EIV
    }

    @Override
    public void removeRecipes(Predicate<TlaRecipe> predicate) {
        // Not supported by EIV
    }

    @Override
    public Comparisons<ItemConvertible> getItemComparisons() {
        return Untyped.cast(COMPARISONS);
    }

    @Override
    public Comparisons<Fluid> getFluidComparisons() {
        return Untyped.cast(COMPARISONS);
    }

    @Override
    public RecipeViewer getActiveViewer() {
        return RecipeViewer.EIV;
    }

    private record BuiltInCategory(BuiltInRecipeCategory category, IEivRecipeViewType viewType) implements TlaCategory, RecipeViewerOwned {
        @Override
        public Identifier getId() {
            return viewType.getId();
        }

        @Override
        public int getDisplayHeight() {
            return viewType.getDisplayHeight();
        }

        @Override
        public int getDisplayWidth() {
            return viewType.getDisplayWidth();
        }

        @Override
        public CategoryIcon getIcon() {
            return CategoryIcon.stack(TlaStack.of(viewType.getIcon()));
        }

        @Override
        public CategoryIcon getSimpleIcon() {
            return CategoryIcon.empty();
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.EIV;
        }
    }
}