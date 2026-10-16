package io.github.mattidragon.tlaapi.impl.rei;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import dev.architectury.fluid.FluidStack;
import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
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
import io.github.mattidragon.tlaapi.impl.Untyped;
import io.github.mattidragon.tlaapi.impl.client.rei.TlaDisplayCategory;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.FluidComparatorRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.registry.display.DisplayRegistryCommon;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.input.RecipeInput;
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

    private static final BiMap<BuiltInRecipeCategory, CategoryIdentifier<?>> VANILLA_CATEGORIES = Arrays.stream(BuiltInRecipeCategory.values()).map(type -> {
        return Map.entry(type, switch (type) {
            case CRAFTING -> CategoryIdentifier.of("minecraft", "plugins/crafting");
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
        });
    }).filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, () -> HashBiMap.create(BuiltInRecipeCategory.values().length)));

    private final Map<CategoryIdentifier<?>, WrappedEmiCategory> builtInCategories = new HashMap<>();
    private final Map<TlaCategory, TlaDisplayCategory> categories = new HashMap<>();
    private final Map<CategoryIdentifier<?>, TlaDisplayCategory> categoryIdentifiers = new HashMap<>();
    private final Multimap<TlaCategory, TlaIngredient> workstations = HashMultimap.create();
    private final List<RecipeGenerator<?>> recipeGenerators = new ArrayList<>();
    private final List<Function<RecipeManager, Stream<TlaRecipe>>> customGenerators = new ArrayList<>();
    private final List<Comparator<Item, ItemStack>> itemComparators = new ArrayList<>();
    private final List<Comparator<Fluid, FluidStack>> fluidComparators = new ArrayList<>();

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

    public void registerCategories(CategoryRegistry registry) {
        registry.add(List.copyOf(categories.values()));
        workstations.forEach((category, workstation) -> registry.addWorkstations(getCategoryIdentifier(category), ReiUtil.convertIngredient(workstation)));
        builtInCategories.forEach((id, category) -> {
            category.category().set(registry.get(id).getCategory());
        });
    }

    public void registerDisplays(DisplayRegistryCommon<?> registry) {
        registry.beginFiller(RecipeDisplay.class)
                .filter(display -> recipeGenerators.stream().anyMatch(generator -> generator.type == display.serializer()))
                .fillMultiple(display -> recipeGenerators.stream()
                        .filter(generator -> generator.type == display.serializer())
                        .findFirst()
                        .stream()
                        .flatMap(generator -> generator.generator.apply(Untyped.cast(display)))
                        .map(TlaDisplay::of)
                        .toList());

        RecipeManager recipeManager = RecipeManager.of(bootstrapContext.getWorld());

        customGenerators.stream()
            .flatMap(generator -> generator.apply(recipeManager))
            .map(TlaDisplay::of)
            .forEach(registry::add);

        builtInCategories.forEach((id, category) -> {
            category.display().set(() -> registry.get(id));
        });
    }

    TlaCategory lookupCategory(CategoryIdentifier<?> categoryId) {
        TlaDisplayCategory tla = categoryIdentifiers.get(categoryId);
        if (tla != null) {
            return tla.getCategory();
        }

        return Optional.ofNullable(VANILLA_CATEGORIES.inverse().get(categoryId))
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
        var displayCategory = new TlaDisplayCategory(category);
        categories.put(category, displayCategory);
        categoryIdentifiers.put(displayCategory.getCategoryIdentifier(), displayCategory);
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
        return Objects.requireNonNull(categories.get(category), "Category " + category + " not registered").getCategoryIdentifier();
    }

    @Override
    public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
        this.workstations.putAll(category, List.of(workstations));
    }

    @Override
    public <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(RecipeDisplay.Serializer<T> type, Function<T, Stream<TlaRecipe>> generator) {
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
    private record RecipeGenerator<T extends RecipeDisplay>(RecipeDisplay.Serializer<T> type, Function<T, Stream<TlaRecipe>> generator) {}
    private record WrappedEmiCategory(
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
