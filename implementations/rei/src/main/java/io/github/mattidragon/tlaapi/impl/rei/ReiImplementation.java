package io.github.mattidragon.tlaapi.impl.rei;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import dev.architectury.fluid.FluidStack;
import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
import io.github.mattidragon.tlaapi.api.gui.RecipePlacementHandler;
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
import io.github.mattidragon.tlaapi.api.recipe.TlaStackComparison;
import io.github.mattidragon.tlaapi.impl.BuiltInCategoryRegistry;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import io.github.mattidragon.tlaapi.impl.Untyped;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.FluidComparatorRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.registry.display.DisplayConsumer.RecipeDisplayConsumer;
import me.shedaniel.rei.api.common.registry.display.DisplayRegistryCommon;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class ReiImplementation implements PluginContext {
    public static ReiImplementation INSTANCE;

    private static final BuiltInCategoryRegistry<CategoryIdentifier<?>> VANILLA_CATEGORIES = new BuiltInCategoryRegistry<>() {
        @Override
        protected @Nullable CategoryIdentifier<?> getValue(BuiltInRecipeCategory type) {
            return switch (type) {
                case CRAFTING_SHAPELESS, CRAFTING_SHAPED -> CategoryIdentifier.of("minecraft", "plugins/crafting");
                case SMELTING -> CategoryIdentifier.of("minecraft", "plugins/smelting");
                case BLASTING -> CategoryIdentifier.of("minecraft", "plugins/blasting");
                case SMOKING -> CategoryIdentifier.of("minecraft", "plugins/smoking");
                case CAMPFIRE_COOKING -> CategoryIdentifier.of("minecraft", "plugins/campfire");
                case STONECUTTING -> CategoryIdentifier.of("minecraft", "plugins/stone_cutting");
                case SMITHING -> CategoryIdentifier.of("minecraft", "plugins/smithing");
                case ANVIL_REPAIRING -> CategoryIdentifier.of("minecraft", "plugins/anvil");
                case GRINDING -> null;//RecipeTypes.GRINDING;
                case BREWING -> CategoryIdentifier.of("minecraft", "plugins/brewing");
                case BEACON_PAYMENT -> CategoryIdentifier.of("minecraft", "plugins/beacon_payment");
                case VILLAGER_TRADING -> null;
                case LOOT -> null;
                case WORLD_INTERACTION_BEACON_PYRAMID -> CategoryIdentifier.of("minecraft", "plugins/beacon_base");
                case WORLD_INTERACTION_OTHER -> null;//RecipeTypes.WORLD_INTERACTION;
                case WORLD_INTERACTION_STRIPPING -> CategoryIdentifier.of("minecraft", "plugins/stripping");
                case WORLD_INTERACTION_SCRAPING -> CategoryIdentifier.of("minecraft", "plugins/wax_scraping");
                case WORLD_INTERACTION_TILLING -> CategoryIdentifier.of("minecraft", "plugins/tilling");
                case WORLD_INTERACTION_FLATTENING -> CategoryIdentifier.of("minecraft", "plugins/pathing");
                case WORLD_INTERACTION_WAXING -> CategoryIdentifier.of("minecraft", "plugins/waxing");
                case WORLD_INTERACTION_OXIDIZING -> CategoryIdentifier.of("minecraft", "plugins/oxidizing");
                case WORLD_INTERACTION_DEOXIDIZING -> CategoryIdentifier.of("minecraft", "plugins/oxidation_scraping");
                case BLASTING_FUEL, SMELTING_FUEL, SMOKING_FUEL -> CategoryIdentifier.of("minecraft", "plugins/fuel");
                case COMPOSTING -> CategoryIdentifier.of("minecraft", "plugins/composting");
                case INFO -> CategoryIdentifier.of("roughlyenoughitems", "plugins/information");
            };
        }

        @Override
        protected BuiltInRecipeCategory getDistinctKey(BuiltInRecipeCategory type) {
            return switch(type) {
                case CRAFTING_SHAPELESS -> BuiltInRecipeCategory.CRAFTING_SHAPED;
                case BLASTING_FUEL, SMELTING_FUEL, SMOKING_FUEL -> BuiltInRecipeCategory.BLASTING_FUEL;
                default -> type;
            };
        }
    };

    public final Map<CategoryIdentifier<?>, WrappedEmiCategory> builtInCategories = new HashMap<>();
    public final BiMap<TlaCategory, CategoryIdentifier<TlaDisplay>> categories = HashBiMap.create();
    public final Multimap<TlaCategory, TlaIngredient> workstations = HashMultimap.create();
    private final List<RecipeGenerator<?>> recipeGenerators = new ArrayList<>();
    private final List<Function<RecipeManager, Stream<TlaRecipe>>> customGenerators = new ArrayList<>();
    private final List<Comparator<Item, ItemStack>> itemComparators = new ArrayList<>();
    private final List<Comparator<Fluid, FluidStack>> fluidComparators = new ArrayList<>();
    public final List<PlacementHandler<?>> placementHandlers = new ArrayList<>();

    public Predicate<TlaStack> hidePredicate = Predicates.alwaysFalse();
    public Predicate<TlaRecipe> recipeHidePredicate = Predicates.alwaysFalse();

    private final Comparisons<ItemConvertible> itemComparisons = new Comparisons<>() {
        @Override
        public void register(ItemConvertible item, TlaStackComparison comparison) {
            itemComparators.add(new Comparator<>(item.asItem(), (context, stack) -> {
                return comparison.hashFunction().hash(TlaStack.of(stack));
            }));
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.REI;
        }
    };
    private final Comparisons<Fluid> fluidComparisons = new Comparisons<>() {
        @Override
        public void register(Fluid fluid, TlaStackComparison comparison) {
            fluidComparators.add(new Comparator<>(fluid, (context, stack) -> {
                return comparison.hashFunction().hash(ReiUtil.convertStack(stack));
            }));
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.REI;
        }
    };

    private PluginLoader.BootstrapContext bootstrapContext;

    public ReiImplementation(PluginLoader.BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
        this.bootstrapContext.registerCategoryProvider(id -> lookupCategory(CategoryIdentifier.of(id)));
        INSTANCE = this;
    }

    private List<RecipeGenerator<RecipeDisplay>> getRecipeGenerators() {
        return Untyped.cast(recipeGenerators);
    }

    @SuppressWarnings("unchecked")
    public void registerDisplays(DisplayRegistryCommon<?> registry) {
        TlaApi.LOGGER.info("Register Displays called with " + registry);
        RecipeManager recipeManager = RecipeManager.of(bootstrapContext.getWorld());

        var groupedGenerators = getRecipeGenerators().stream().collect(Collectors.groupingBy(RecipeGenerator::type));

        if (registry instanceof RecipeDisplayConsumer consumer) {
            // TODO: Y u no work? U work 4 dem -> DefaultClientPlugin But no 4 us
            groupedGenerators.forEach((type, generators) -> {
                consumer.beginRecipeFiller(RecipeDisplay.class)
                    .filterType(type)
                    .fillMultiple((display, id) -> {
                        return generators.stream()
                                .flatMap(g -> g.generator().apply(null, display))
                                .map(TlaDisplay::of).toList();
                    });
                });
        } else {
            registry.beginFiller(RecipeEntry.class)
                    .fillMultiple(entry -> {
                return ((List<RecipeDisplay>)entry.value().getDisplays()).stream()
                        .filter(display -> groupedGenerators.containsKey(display.serializer()))
                        .flatMap(display -> groupedGenerators.get(display.serializer()).stream().flatMap(g -> g.generator().apply(Untyped.cast(entry.id()), display)))
                        .map(TlaDisplay::of)
                        .toList();
            });
        }

        customGenerators.stream()
            .flatMap(generator -> generator.apply(recipeManager))
            .map(TlaDisplay::of)
            .forEach(registry::add);

        builtInCategories.forEach((id, category) -> {
            category.display().set(() -> registry.get(id));
        });
    }

    TlaCategory lookupCategory(CategoryIdentifier<?> categoryId) {
        TlaCategory tla = categories.inverse().get(categoryId);
        if (tla != null) {
            return tla;
        }

        return VANILLA_CATEGORIES.getOptional(categoryId)
            .flatMap(this::getVanillaCategory)
            .orElseGet(() -> builtInCategories.computeIfAbsent(categoryId, i -> new WrappedEmiCategory(null, i, new int[] { 0, 0 })));
    }

    void registerItemComparators(ItemComparatorRegistry registry) {
        itemComparators.forEach(comparator -> registry.register(comparator.comparator(), comparator.key()));
    }

    void registerFluidComparators(FluidComparatorRegistry registry) {
        fluidComparators.forEach(comparator -> registry.register(comparator.comparator(), comparator.key()));
    }

    @Override
    public void addCategory(TlaCategory category) {
        if (bootstrapContext.getOwnerOf(category) != null) {
            return;
        }

        categories.put(category, CategoryIdentifier.of(category.getId()));
        bootstrapContext.registerCategory(category);
    }

    @Override
    public Optional<TlaCategory> getVanillaCategory(BuiltInRecipeCategory type) {
        return Optional.ofNullable(VANILLA_CATEGORIES.get(type))
                .map(identifier -> builtInCategories.computeIfAbsent(identifier, id -> new WrappedEmiCategory(type, id, type.getSize())));
    }

    @Override
    public Optional<BuiltInRecipeCategory> getVanillaCategory(TlaCategory category) {
        if (category instanceof WrappedEmiCategory c) {
            return Optional.ofNullable(c.type());
        }

        RecipeViewer viewer = bootstrapContext.getOwnerOf(category);
        return viewer == getActiveViewer() ? Optional.empty() : bootstrapContext.getPluginContext(viewer).flatMap(ctx -> ctx.getVanillaCategory(category));
    }

    public CategoryIdentifier<?> getCategoryIdentifier(TlaCategory category) {
        if (category instanceof WrappedEmiCategory builtIn) {
            return builtIn.id();
        }
        return Objects.requireNonNull(categories.get(category), "Category " + category + " not registered");
    }

    @Override
    public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
        this.workstations.putAll(category, List.of(workstations));
    }

    @Override
    public <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, StreamedRecipeGenerator<I, T> generator) {
        recipeGenerators.add(new RecipeGenerator<>(type, generator));
    }

    @Override
    public void addGenerator(Function<RecipeManager, Stream<TlaRecipe>> generator) {
        customGenerators.add(generator);
    }

    @Override
    public void removeStacks(Predicate<TlaStack> predicate) {
        hidePredicate = hidePredicate.or(predicate);
    }

    @Override
    public void removeRecipes(Predicate<TlaRecipe> predicate) {
        recipeHidePredicate = recipeHidePredicate.and(predicate);
    }

    @Override
    public <C extends ScreenHandler> void addPlacementHandler(ScreenHandlerType<C> screenType, TlaCategory category, RecipePlacementHandler<C> placementHandler) {
        placementHandlers.add(new PlacementHandler<>(screenType, category, placementHandler));
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
        return RecipeViewer.REI;
    }

    @Override
    public String toString() {
        return "REI plugin handler";
    }

    private record Comparator<T, S>(T key, EntryComparator<S> comparator) {}
    public record RecipeGenerator<T extends RecipeDisplay>(RecipeDisplay.Serializer<T> type, StreamedRecipeGenerator<?, T> generator) {}
    public record PlacementHandler<C extends ScreenHandler>(ScreenHandlerType<C> screenType, TlaCategory category, RecipePlacementHandler<C> placementHandler) {}
    public record WrappedEmiCategory(
            @Nullable BuiltInRecipeCategory type,
            CategoryIdentifier<?> id,
            int[] fallbackSize,
            AtomicReference<DisplayCategory<?>> category,
            AtomicReference<Supplier<List<? extends Display>>> display
    ) implements TlaCategory, RecipeViewerOwned {
        public WrappedEmiCategory(@Nullable BuiltInRecipeCategory type, CategoryIdentifier<?> id, int[] fallbackSize) {
            this(type, id, fallbackSize, new AtomicReference<>(null), new AtomicReference<>(List::of));
        }

        @Override
        public Identifier getId() {
            return id.getIdentifier();
        }

        @Override
        public int getDisplayHeight() {
            DisplayCategory<?> i = category.get();
            return i == null ? fallbackSize[1] : i.getDisplayHeight();
        }

        @Override
        public int getDisplayWidth() {
            DisplayCategory<?> i = category.get();
            return i == null ? fallbackSize[0] : display.get().get().stream().mapToInt(d -> i.getDisplayWidth(Untyped.cast(d))).max().orElse(fallbackSize[0]);
        }

        @Override
        public CategoryIcon getIcon() {
            return CategoryIcon.empty();
        }

        @Override
        public CategoryIcon getSimpleIcon() {
            return CategoryIcon.empty();
        }

        @Override
        public RecipeViewer getRecipeViewer() {
            return RecipeViewer.REI;
        }
    }
}
