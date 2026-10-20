package io.github.mattidragon.tlaapi.impl.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContextBackend;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginLoader;
import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;
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
import io.github.mattidragon.tlaapi.api.recipe.TlaStackComparison;
import io.github.mattidragon.tlaapi.impl.BuiltInCategoryRegistry;
import io.github.mattidragon.tlaapi.impl.Untyped;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.RecipeDisplay.Serializer;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class TlaApiEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        ClientPluginLoader.loadPlugins(PluginLoader.loadPlugins(RecipeViewer.EMI, bootstrapContext -> new EmiImplementation(registry, bootstrapContext)));
    }

    private static final class EmiImplementation implements PluginContext, ClientPluginContextBackend {
        private final EmiRegistry registry;
        private final Map<EmiRecipeCategory, TlaCategory> builtInCategories = new HashMap<>();
        private final Map<TlaCategory, EmiRecipeCategory> categories = new HashMap<>();

        private final Comparisons<ItemConvertible> itemComparisons = new EmiComparisons<>();
        private final Comparisons<Fluid> fluidComparisons = new EmiComparisons<>();

        private final RecipeManager recipeManager;

        private PluginLoader.BootstrapContext bootstrapContext;

        private final BuiltInCategoryRegistry<EmiRecipeCategory> vanillaCategories = new BuiltInCategoryRegistry<>() {
            @Override
            protected @Nullable EmiRecipeCategory getValue(BuiltInRecipeCategory type) {
                return switch (type) {
                    case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> VanillaEmiRecipeCategories.CRAFTING;
                    case SMELTING -> VanillaEmiRecipeCategories.SMELTING;
                    case BLASTING -> VanillaEmiRecipeCategories.BLASTING;
                    case SMOKING -> VanillaEmiRecipeCategories.SMOKING;
                    case CAMPFIRE_COOKING -> VanillaEmiRecipeCategories.CAMPFIRE_COOKING;
                    case STONECUTTING -> VanillaEmiRecipeCategories.STONECUTTING;
                    case SMITHING -> VanillaEmiRecipeCategories.SMITHING;
                    case ANVIL_REPAIRING -> VanillaEmiRecipeCategories.ANVIL_REPAIRING;
                    case GRINDING -> VanillaEmiRecipeCategories.GRINDING;
                    case BREWING -> VanillaEmiRecipeCategories.BREWING;
                    case BEACON_PAYMENT, VILLAGER_TRADING, LOOT -> null;
                    case WORLD_INTERACTION_BEACON_PYRAMID, WORLD_INTERACTION_STRIPPING, WORLD_INTERACTION_SCRAPING,
                            WORLD_INTERACTION_TILLING, WORLD_INTERACTION_FLATTENING, WORLD_INTERACTION_WAXING,
                            WORLD_INTERACTION_OXIDIZING, WORLD_INTERACTION_DEOXIDIZING, WORLD_INTERACTION_OTHER -> VanillaEmiRecipeCategories.WORLD_INTERACTION;
                    case BLASTING_FUEL, SMELTING_FUEL, SMOKING_FUEL -> VanillaEmiRecipeCategories.FUEL;
                    case COMPOSTING -> VanillaEmiRecipeCategories.COMPOSTING;
                    case INFO -> VanillaEmiRecipeCategories.INFO;
                };
            }

            protected BuiltInRecipeCategory getDistinctKey(BuiltInRecipeCategory type) {
                return switch (type) {
                    case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> BuiltInRecipeCategory.CRAFTING_SHAPED;
                    case WORLD_INTERACTION_BEACON_PYRAMID, WORLD_INTERACTION_STRIPPING, WORLD_INTERACTION_SCRAPING,
                            WORLD_INTERACTION_TILLING, WORLD_INTERACTION_FLATTENING, WORLD_INTERACTION_WAXING,
                            WORLD_INTERACTION_OXIDIZING, WORLD_INTERACTION_DEOXIDIZING, WORLD_INTERACTION_OTHER -> BuiltInRecipeCategory.WORLD_INTERACTION_OTHER;
                    case BLASTING_FUEL, SMELTING_FUEL, SMOKING_FUEL -> BuiltInRecipeCategory.BLASTING_FUEL;
                    default -> type;
                };
            }
        };

        private EmiImplementation(EmiRegistry registry, PluginLoader.BootstrapContext bootstrapContext) {
            this.registry = registry;
            this.bootstrapContext = bootstrapContext;
            this.bootstrapContext.registerCategoryProvider(id -> builtInCategories.values().stream().filter(category -> category.getId().equals(id)).findFirst().orElse(null));
            this.recipeManager = RecipeManager.of(bootstrapContext.getWorld().getRegistryManager(), registry.getRecipeManager());
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
            var emiCategory = new TlaEmiRecipeCategory(category);
            categories.put(category, emiCategory);
            registry.addCategory(emiCategory);
            bootstrapContext.registerCategory(category);
        }

        @Override
        public Optional<TlaCategory> getVanillaCategory(BuiltInRecipeCategory type) {
            return vanillaCategories.getOptional(type).map(category -> builtInCategories.computeIfAbsent(category, id -> new WrappedEmiCategory(type, id, type.getSize())));
        }

        @Override
        public Optional<BuiltInRecipeCategory> getVanillaCategory(TlaCategory category) {
            if (category instanceof WrappedEmiCategory c) {
                return Optional.ofNullable(c.type());
            }

            RecipeViewer viewer = bootstrapContext.getOwnerOf(category);
            return viewer == getActiveViewer() ? Optional.empty() : bootstrapContext.getPluginContext(viewer).flatMap(ctx -> ctx.getVanillaCategory(category));
        }

        @Override
        public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
            var emiCategory = getEmiCategory(category);
            for (TlaIngredient workstation : workstations) {
                registry.addWorkstation(emiCategory, EmiUtils.convertIngredient(workstation));
            }
        }

        private EmiRecipeCategory getEmiCategory(TlaCategory category) {
            if (category instanceof WrappedEmiCategory builtIn) {
                return builtIn.category();
            }

            EmiRecipeCategory emiCategory = categories.get(category);
            if (emiCategory != null) {
                return emiCategory;
            }

            RecipeViewer viewer = bootstrapContext.getOwnerOf(category);

            return Objects.requireNonNull(viewer == getActiveViewer() ? null : bootstrapContext.getPluginContext(viewer)
                    .flatMap(ctx -> ctx.getVanillaCategory(category))
                    .map(vanillaCategories::get)
                    .orElse(null), "Category " + category + " not registered");
        }

        @Override
        public <I extends RecipeInput, T extends RecipeDisplay> void addRecipeMultiGenerator(Serializer<T> type, Function<T, Stream<TlaRecipe>> generator) {
            registry.getRecipeManager().values().stream()
                .flatMap(recipe -> recipe.value().getDisplays().stream())
                .filter(display -> type == display.serializer())
                .flatMap(display -> generator.apply(Untyped.cast(display)))
                .map(tlaRecipe -> new TlaEmiRecipe(tlaRecipe, getEmiCategory(tlaRecipe.getCategory())))
                .forEach(registry::addRecipe);
        }

        @Override
        public void addGenerator(Function<RecipeManager, Stream<TlaRecipe>> generator) {
            generator.apply(recipeManager)
                .map(tlaRecipe -> new TlaEmiRecipe(tlaRecipe, getEmiCategory(tlaRecipe.getCategory())))
                .forEach(registry::addRecipe);
        }

        @Override
        public void removeStacks(TlaStack stack) {
            registry.removeEmiStacks(EmiUtils.convertStack(stack));
        }

        @Override
        public void removeStacks(Predicate<TlaStack> predicate) {
            registry.removeEmiStacks(stack -> predicate.test(EmiUtils.convertStack(stack)));
        }

        TlaCategory lookupCategory(EmiRecipeCategory categoryId, int[] size) {
            if (categoryId instanceof TlaEmiRecipeCategory c) {
                return c.category;
            }

            return vanillaCategories.getOptional(categoryId)
                .flatMap(this::getVanillaCategory)
                .orElseGet(() -> builtInCategories.computeIfAbsent(categoryId, i -> new WrappedEmiCategory(null, i, size)));
        }

        public TlaRecipe recipeOf(EmiRecipe recipe) {
            return switch(recipe) {
                case TlaEmiRecipe r -> r.getUnderlying();
                default -> new BasicTlaRecipe(
                        lookupCategory(recipe.getCategory(), new int[] {recipe.getDisplayWidth(), recipe.getDisplayHeight() }),
                        recipe.getId() == null ? null : RegistryKey.of(RegistryKeys.RECIPE, recipe.getId()),
                        () -> recipe.getInputs().stream().map(EmiUtils::convertIngredient).toList(),
                        () -> recipe.getOutputs().stream().map(EmiUtils::convertStack).toList(),
                        () -> recipe.getCatalysts().stream().map(EmiUtils::convertIngredient).toList()
                );
            };
        }

        @Override
        public void removeRecipes(Predicate<TlaRecipe> predicate) {
            registry.removeRecipes(recipe -> predicate.test(recipeOf(recipe)));
        }

        @Override
        public void removeRecipes(Identifier id) {
            registry.removeRecipes(id);
        }

        @Override
        public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> tTlaBoundsFunction) {
            // Emi doesn't provide this feature
        }

        @Override
        public <T extends HandledScreen<?>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> tTlaBoundsFunction) {
            // Emi doesn't provide this feature
        }

        @Override
        public <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler) {
            registry.addDragDropHandler(clazz, new EmiDragDropHandler<T>() {
                @Override
                public boolean dropStack(T screen, EmiIngredient ingredient, int x, int y) {
                    if (!handler.appliesTo(screen)) return false;

                    var stacks = ingredient.getEmiStacks();
                    if (stacks.isEmpty()) return false;
                    var stack = EmiUtils.convertStack(stacks.getFirst());

                    var targets = handler.getDropTargets(screen);
                    for (StackDragHandler.DropTarget target : targets) {
                        if (target.bounds().contains(x, y)) {
                            var accepted = target.ingredientConsumer().apply(stack);
                            if (accepted) return true;
                        }
                    }

                    return false;
                }

                @Override
                public void render(T screen, EmiIngredient dragged, DrawContext draw, int mouseX, int mouseY, float delta) {
                    if (!handler.appliesTo(screen)) return;

                    var targets = handler.getDropTargets(screen);
                    for (StackDragHandler.DropTarget target : targets) {
                        var bounds = target.bounds();
                        draw.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), 0x8822BB33);
                    }
                }
            });
        }

        @Override
        public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
            registry.addExclusionArea(clazz, (screen, consumer) -> {
                for (var tlaBounds : provider.apply(screen)) {
                    var bounds = new Bounds(tlaBounds.x(), tlaBounds.y(), tlaBounds.width(), tlaBounds.height());
                    consumer.accept(bounds);
                }
            });
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
            return RecipeViewer.EMI;
        }

        @Override
        public String toString() {
            return "EMI plugin handler";
        }

        private class EmiComparisons<T> implements Comparisons<T> {
            @Override
            public void register(T key, TlaStackComparison comparison) {
                registry.setDefaultComparison(key, Comparison.of(
                        (a, b) -> comparison.equalityPredicate().test(EmiUtils.convertStack(a), EmiUtils.convertStack(b)),
                        stack -> comparison.hashFunction().hash(EmiUtils.convertStack(stack)))
                );
            }

            @Override
            public RecipeViewer getRecipeViewer() {
                return RecipeViewer.EMI;
            }
        }
    }

    private record WrappedEmiCategory(BuiltInRecipeCategory type, EmiRecipeCategory category, int[] size) implements TlaCategory, RecipeViewerOwned {
        private static final CategoryIcon ICON = CategoryIcon.stack(TlaStack.empty());

        @Override
        public Identifier getId() {
            return category.getId();
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
            return RecipeViewer.REI;
        }
    }
}
