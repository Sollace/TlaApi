package io.github.mattidragon.tlaapi.impl.jei;

import io.github.mattidragon.tlaapi.api.plugin.ModdingPlatform;
import io.github.mattidragon.tlaapi.api.plugin.RecipeManager;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import io.github.mattidragon.tlaapi.impl.Untyped;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IRuntimeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.Nullable;

@JeiPlugin
public class TlaApiJeiPlugin implements IModPlugin {
    private final Untyped.Resettable<TlaApiJeiPluginContext> context = Untyped.memoize(TlaApiJeiPluginContext::new);

    static IJeiHelpers JEI_HELPERS;

    static {
        ModdingPlatform.getInstance().addServerAvailableListener(server -> {
            PreparedRecipes clientSyncedRecipes = Internal.getClientSyncedRecipes();
            if (clientSyncedRecipes.recipes().isEmpty()) {
                if (server != null) {
                    Internal.setClientSyncedRecipes(PreparedRecipes.of(server.getRecipeManager().values()));
                } else {
                    TlaApi.LOGGER.info("Server not available to populate JEI's recipes cache :'(");
                }
            }
        });
    }

    @Nullable
    private IJeiRuntime runtime;

    @Nullable
    private IRecipeManager getRecipeManager() {
        return runtime == null ? null : runtime.getRecipeManager();
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.of("tla-api", "plugin");
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        context.reset();
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var context = this.context.get();
        registration.addRecipeCategories(context.categories.stream()
                .map(category -> context.preparedCategories.computeIfAbsent(category, c -> new TlaRecipeCategory(registration.getJeiHelpers(), c)))
                .toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        context.get().subTypeProviders.forEach(provider -> provider.accept(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        context.get().workstations.forEach((category, workstations) ->
                JeiUtils.convertIngredient(registration.getJeiHelpers(), workstations).forEach(workstation -> addCatalyst(registration, category, workstation)));
    }

    private <T> void addCatalyst(IRecipeCatalystRegistration registration, TlaCategory category, ITypedIngredient<T> workstation) {
        registration.addCraftingStation(getRecipeType(category), workstation.getType(), workstation.getIngredient());
    }

    private IRecipeType<PreparedRecipe> getRecipeType(TlaCategory category) {
        if (category instanceof TlaApiJeiPluginContext.BuiltInTlaCategory builtIn) {
            return Untyped.cast(builtIn.type());
        }
        return Objects.requireNonNull(context.get().preparedCategories.get(category), "Category " + category.getId() + " must be registered").getRecipeType();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var jeiHelpers = registration.getJeiHelpers();
        var context = this.context.get();
        RecipeManager recipeManager = context.bootstrapContext.getWorld() instanceof ServerWorld sw ? RecipeManager.of(sw.getRegistryManager(), sw.getRecipeManager()) : RecipeManager.of(
                context.bootstrapContext.getWorld().getRegistryManager(),
                () -> Internal.getClientSyncedRecipes().recipes().stream(),
                type -> Internal.getClientSyncedRecipes().getAll(type)
        );

        Stream.concat(
                recipeManager.values()
                    .flatMap(entry -> context.recipeFunctions.stream().flatMap(generator -> generator.apply(entry))),
                    context.recipeGenerators.stream().flatMap(generator -> generator.apply(recipeManager))
            )
            .map(recipe -> new PreparedRecipe(jeiHelpers, recipe))
            .collect(Collectors.groupingBy(PreparedRecipe::getCategory))
            .forEach((category, recipes) -> registration.addRecipes(getRecipeType(category), recipes));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        var context = this.context.get();
        var adders = new Object() {
            <T extends HandledScreen<?>> void addClickArea(TlaApiJeiPluginContext.ClickAreaTuple<T> tuple) {
                registration.addGuiContainerHandler(tuple.clazz(), new IGuiContainerHandler<>() {
                    @Override
                    public Collection<IGuiClickableArea> getGuiClickableAreas(T containerScreen, double guiMouseX, double guiMouseY) {
                        var bounds = tuple.boundsFunction().apply(containerScreen);
                        var area = IGuiClickableArea.createBasic(bounds.x(), bounds.y(), bounds.width(), bounds.height(), getRecipeType(tuple.category()));
                        return List.of(area);
                    }
                });
            }

            <T extends Screen> void addSizeProvider(TlaApiJeiPluginContext.ScreenSizeProvider<T> provider) {
                registration.addGuiScreenHandler(provider.clazz(), screen -> {
                    var bounds = provider.provider().apply(screen);
                    return new IGuiProperties() {
                        @Override
                        public Class<? extends Screen> screenClass() {
                            return provider.clazz();
                        }

                        @Override
                        public int guiLeft() {
                            return bounds.left();
                        }

                        @Override
                        public int guiTop() {
                            return bounds.top();
                        }

                        @Override
                        public int guiXSize() {
                            return bounds.width();
                        }

                        @Override
                        public int guiYSize() {
                            return bounds.height();
                        }

                        @Override
                        public int screenWidth() {
                            return screen.width;
                        }

                        @Override
                        public int screenHeight() {
                            return screen.height;
                        }
                    };
                });
            }

            <T extends Screen> void addGhostHandler(TlaApiJeiPluginContext.GhostHandler<T> ghostHandler) {
                var handler = ghostHandler.handler();
                registration.addGhostIngredientHandler(ghostHandler.clazz(), new IGhostIngredientHandler<>() {
                    @Override
                    public <I> List<Target<I>> getTargetsTyped(T gui, ITypedIngredient<I> typedIngredient, boolean doStart) {
                        if (!handler.appliesTo(gui)) return List.of();
                        return handler.getDropTargets(gui)
                                .stream()
                                .<Target<I>>map(target -> new Target<>() {
                                    @Override
                                    public Rect2i getArea() {
                                        return target.bounds().apply(Rect2i::new);
                                    }

                                    @Override
                                    public void accept(I ingredient) {
                                        target.ingredientConsumer().apply(JeiUtils.convertStack(registration.getJeiHelpers().getPlatformFluidHelper(), typedIngredient));
                                    }
                                })
                                .toList();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
            }

            <T extends HandledScreen<?>> void addExclusionZoneProvider(TlaApiJeiPluginContext.ExclusionZoneProvider<T> provider) {
                registration.addGuiContainerHandler(provider.clazz(), new IGuiContainerHandler<>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(T containerScreen) {
                        return StreamSupport.stream(provider.provider().apply(containerScreen).spliterator(), false)
                                .map(bounds -> bounds.apply(Rect2i::new))
                                .toList();
                    }
                });
            }
        };
        context.clickAreas.forEach(adders::addClickArea);
        context.ghostHandlers.forEach(adders::addGhostHandler);
        context.sizeProviders.forEach(adders::addSizeProvider);
        context.exclusionZoneProviders.forEach(adders::addExclusionZoneProvider);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var context = this.context.get();
        context.transferHandlers.forEach(handler -> {
            registration.getJeiHelpers().getRecipeType(handler.category().getId()).ifPresentOrElse(type -> {
                registration.addRecipeTransferHandler(new IRecipeTransferInfo<>() {
                    @Override
                    public Class<? extends ScreenHandler> getContainerClass() {
                        return Untyped.cast(ScreenHandler.class);
                    }

                    @Override
                    public Optional<ScreenHandlerType<ScreenHandler>> getMenuType() {
                        return Optional.of(Untyped.cast(handler.type()));
                    }

                    @Override
                    public IRecipeType<Object> getRecipeType() {
                        return Untyped.cast(type);
                    }

                    @Override
                    public boolean canHandle(ScreenHandler container, Object recipe) {
                        var recipeManager = getRecipeManager();
                        return recipeManager != null && container.getType() == handler.type()
                            && handler.placementHandler().isApplicable(
                                    handler.category(),
                                    context.convertRecipe(
                                            Untyped.cast(recipeManager.getRecipeCategory(type)), recipe,
                                            registration.getJeiHelpers().getPlatformFluidHelper(), recipeManager
                                    ));
                    }

                    @Override
                    public List<Slot> getRecipeSlots(ScreenHandler container, Object recipe) {
                        return handler.placementHandler().getCraftingSlots(Untyped.cast(container)).toList();
                    }

                    @Override
                    public List<Slot> getInventorySlots(ScreenHandler container, Object recipe) {
                        return handler.placementHandler().getInputSlots(Untyped.cast(container)).toList();
                    }
                });
            }, () -> TlaApi.LOGGER.error("Cannot register placement handler for unknown recipe category {}", handler.category().getId()));

        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        var manager = runtime.getIngredientManager();
        manager.getRegisteredIngredientTypes().stream().map(type -> {
            var hidden = manager.getAllIngredients(type).stream().filter(ingredient -> {
                var typed = runtime.getIngredientManager().createTypedIngredient(Untyped.cast(type), ingredient, false).orElse(null);
                return typed != null && !context.get().shouldDisplayIngredient(JeiUtils.convertStack(runtime.getJeiHelpers().getPlatformFluidHelper(), typed));
            }).toList();
            return hidden.isEmpty() ? null : Map.entry(type, hidden);
        }).filter(Objects::nonNull).forEach(entry -> manager.removeIngredientsAtRuntime(Untyped.cast(entry.getKey()), entry.getValue()));
    }

    @Override
    public void registerRuntime(IRuntimeRegistration registration) {
        var recipeManager = registration.getRecipeManager();
        var context = this.context.get();
        recipeManager.createRecipeCategoryLookup().get().forEach(category -> {
            recipeManager.hideRecipes(category.getRecipeType(), Untyped.cast(recipeManager.createRecipeLookup(category.getRecipeType())
                    .get()
                    .filter(recipe -> {
                        var convertedRecipe = context.convertRecipe(Untyped.cast(category), recipe, registration.getJeiHelpers().getPlatformFluidHelper(), recipeManager);
                        var result = !context.shouldDisplayRecipe(convertedRecipe);
                        if (result) {
                            TlaApi.LOGGER.debug("Hiding recipe: " + convertedRecipe.getId());
                        }
                        return result;
                    }).toList()));
        });
    }
}
