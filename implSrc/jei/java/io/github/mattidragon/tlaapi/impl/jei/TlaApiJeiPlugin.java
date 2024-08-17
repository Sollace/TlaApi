package io.github.mattidragon.tlaapi.impl.jei;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.mattidragon.tlaapi.api.StackDragHandler;
import io.github.mattidragon.tlaapi.api.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.plugin.Comparisons;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.api.recipe.TlaStackComparison;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@JeiPlugin
public class TlaApiJeiPlugin implements IModPlugin, PluginContext {
    private final List<TlaCategory> categories = new ArrayList<>();
    private final List<BiFunction<MinecraftClient, RecipeManager, Stream<TlaRecipe>>> recipeFunctions = new ArrayList<>();
    private final Multimap<TlaCategory, TlaIngredient> workstations = HashMultimap.create();
    private final List<ClickAreaTuple<?>> clickAreas = new ArrayList<>();
    private final List<GhostHandler<?>> ghostHandlers = new ArrayList<>();
    private final List<ScreenSizeProvider<?>> sizeProviders = new ArrayList<>();
    private final List<ExclusionZoneProvider<?>> exclusionZoneProviders = new ArrayList<>();
    private final List<Consumer<ISubtypeRegistration>> subTypeProviders = new ArrayList<>();

    private final Map<TlaCategory, TlaRecipeCategory> preparedCategories = new HashMap<>();

    private final Comparisons<ItemConvertible> itemComparisons = new Comparisons<>() {
        @Override
        public void register(ItemConvertible item, TlaStackComparison comparison) {
            subTypeProviders.add(registration -> registration.registerSubtypeInterpreter(item.asItem(), (stack, context) -> {
                return comparison.hashFunction().hash(TlaStack.of(stack)) + "";
            }));
        }

    };
    private final Comparisons<Fluid> fluidComparisons = new Comparisons<>() {
        @Override
        public void register(Fluid fluid, TlaStackComparison comparison) {
            subTypeProviders.add(registration -> registration.registerSubtypeInterpreter(FabricTypes.FLUID_STACK, fluid, (fluidVariant, context) -> {
                return comparison.hashFunction().hash(TlaStack.bucketOf(fluidVariant.getFluidVariant())) + "";
            }));
        }
    };

    @Override
    public Identifier getPluginUid() {
        return Identifier.of("tla-api", "plugin");
    }

    private void init() {
        categories.clear();
        recipeFunctions.clear();
        workstations.clear();
        preparedCategories.clear();
        subTypeProviders.clear();
        PluginLoader.loadPlugins(this);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // This is the earliest event, and we don't get anything before that.
        init();

        for (var category : categories) {
            var jeiCategory = new TlaRecipeCategory(registration.getJeiHelpers(), category);
            preparedCategories.put(category, jeiCategory);
            registration.addRecipeCategories(jeiCategory);
        }
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        subTypeProviders.forEach(provider -> provider.accept(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        workstations.forEach((category, workstations) ->
                JeiUtils.convertIngredient(registration.getJeiHelpers(), workstations).forEach(workstation -> addCatalyst(registration, category, workstation)));
    }

    private <T> void addCatalyst(IRecipeCatalystRegistration registration, TlaCategory category, ITypedIngredient<T> workstation) {
        registration.addRecipeCatalyst(workstation.getType(), workstation.getIngredient(), preparedCategories.get(category).getRecipeType());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var client = MinecraftClient.getInstance();
        var manager = Objects.requireNonNull(client.world, "World should exist").getRecipeManager();
        var jeiHelpers = registration.getJeiHelpers();

        recipeFunctions.stream()
                .flatMap(function -> function.apply(client, manager))
                .map(recipe -> new PreparedRecipe(jeiHelpers, recipe))
                .collect(Collectors.groupingBy(PreparedRecipe::getCategory))
                .forEach((category, recipes) -> registration.addRecipes(Objects.requireNonNull(preparedCategories.get(category), "Category must be registered").getRecipeType(), recipes));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        class Adders {
            <T extends HandledScreen<?>> void addClickArea(ClickAreaTuple<T> tuple) {
                registration.addGuiContainerHandler(tuple.clazz, new IGuiContainerHandler<>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(T containerScreen, double guiMouseX, double guiMouseY) {
                        var bounds = tuple.boundsFunction.apply(containerScreen);
                        var area = IGuiClickableArea.createBasic(bounds.x(), bounds.y(), bounds.width(), bounds.height(), preparedCategories.get(tuple.category).getRecipeType());
                        return List.of(area);
                    }
                });
            }

            <T extends Screen> void addSizeProvider(ScreenSizeProvider<T> provider) {
                registration.addGuiScreenHandler(provider.clazz, screen -> {
                    var bounds = provider.provider.apply(screen);
                    return new IGuiProperties() {
                        @Override
                        public Class<? extends Screen> getScreenClass() {
                            return provider.clazz;
                        }

                        @Override
                        public int getGuiLeft() {
                            return bounds.left();
                        }

                        @Override
                        public int getGuiTop() {
                            return bounds.top();
                        }

                        @Override
                        public int getGuiXSize() {
                            return bounds.width();
                        }

                        @Override
                        public int getGuiYSize() {
                            return bounds.height();
                        }

                        @Override
                        public int getScreenWidth() {
                            return screen.width;
                        }

                        @Override
                        public int getScreenHeight() {
                            return screen.height;
                        }
                    };
                });
            }

            <T extends Screen> void addGhostHandler(GhostHandler<T> ghostHandler) {
                var handler = ghostHandler.handler;
                registration.addGhostIngredientHandler(ghostHandler.clazz, new IGhostIngredientHandler<>() {
                    @Override
                    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> typedIngredient, boolean doStart) {
                        if (!handler.appliesTo(gui)) return List.of();
                        return handler.getDropTargets(gui)
                                .stream()
                                .<Target<I>>map(target -> new Target<>() {
                                    @Override
                                    public Rect2i getArea() {
                                        var bounds = target.bounds();
                                        return new Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height());
                                    }

                                    @Override
                                    public void accept(I ingredient) {
                                        target.ingredientConsumer().apply(JeiUtils.convertStack(typedIngredient));
                                    }
                                })
                                .toList();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
            }

            <T extends HandledScreen<?>> void addExclusionZoneProvider(ExclusionZoneProvider<T> provider) {
                registration.addGuiContainerHandler(provider.clazz, new IGuiContainerHandler<>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(T containerScreen) {
                        return StreamSupport.stream(provider.provider.apply(containerScreen).spliterator(), false)
                                .map(bounds -> new Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height()))
                                .toList();
                    }
                });
            }
        }
        var adders = new Adders();
        clickAreas.forEach(adders::addClickArea);
        ghostHandlers.forEach(adders::addGhostHandler);
        sizeProviders.forEach(adders::addSizeProvider);
        exclusionZoneProviders.forEach(adders::addExclusionZoneProvider);
    }

    @Override
    public void addCategory(TlaCategory category) {
        categories.add(category);
    }

    @Override
    public void addWorkstation(TlaCategory category, TlaIngredient... workstations) {
        this.workstations.putAll(category, Arrays.asList(workstations));
    }

    @Override
    public <I extends RecipeInput, T extends Recipe<I>> void addRecipeGenerator(RecipeType<T> type, Function<RecipeEntry<T>, TlaRecipe> generator) {
        recipeFunctions.add((client, manager) -> manager.listAllOfType(type).stream().map(generator));
    }

    @Override
    public void addGenerator(Function<MinecraftClient, List<TlaRecipe>> generator) {
        recipeFunctions.add((client, manager) -> generator.apply(client).stream());
    }

    @Override
    public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        // JEI doesn't support click areas for non-handled screens
    }

    @Override
    public <T extends HandledScreen<?>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
        if (!HandledScreen.class.isAssignableFrom(clazz)) return; // JEI only supports exclusion zones on handled screens
        exclusionZoneProviders.add(new ExclusionZoneProvider<>((Class<HandledScreen<?>>) clazz, (Function<HandledScreen<?>, ? extends Iterable<TlaBounds>>) provider));
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

    private record ClickAreaTuple<T extends HandledScreen<?>>(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {}
    private record GhostHandler<T extends Screen>(Class<T> clazz, StackDragHandler<T> handler) {}
    private record ScreenSizeProvider<T extends Screen>(Class<T> clazz, Function<T, TlaBounds> provider) {}
    private record ExclusionZoneProvider<T extends HandledScreen<?>>(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {}
}
