package io.github.mattidragon.tlaapi.impl.jei;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.Untyped;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.common.Internal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@JeiPlugin
public class TlaApiJeiPlugin implements IModPlugin {
    private TlaApiJeiPluginContext context;

    @Override
    public Identifier getPluginUid() {
        return Identifier.of("tla-api", "plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // This is the earliest event, and we don't get anything before that.
        context = new TlaApiJeiPluginContext();
        registration.addRecipeCategories(context.categories.stream()
                .map(category -> context.preparedCategories.computeIfAbsent(category, c -> new TlaRecipeCategory(registration.getJeiHelpers(), c)))
                .toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        context.subTypeProviders.forEach(provider -> provider.accept(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        context.workstations.forEach((category, workstations) ->
                JeiUtils.convertIngredient(registration.getJeiHelpers(), workstations).forEach(workstation -> addCatalyst(registration, category, workstation)));
    }

    private <T> void addCatalyst(IRecipeCatalystRegistration registration, TlaCategory category, ITypedIngredient<T> workstation) {
        registration.addCraftingStation(getRecipeType(category), workstation.getType(), workstation.getIngredient());
    }

    private IRecipeType<PreparedRecipe> getRecipeType(TlaCategory category) {
        if (category instanceof TlaApiJeiPluginContext.BuiltInTlaCategory builtIn) {
            return Untyped.cast(builtIn.type());
        }
        return Objects.requireNonNull(context.preparedCategories.get(category), "Category " + category.getId() + " must be registered").getRecipeType();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var client = MinecraftClient.getInstance();
        var jeiHelpers = registration.getJeiHelpers();
        var server = client.getServer();

        Stream.concat(
                (server == null ? Internal.getClientSyncedRecipes().recipes() : server.getRecipeManager().values())
                    .stream()
                    .flatMap(entry -> context.recipeFunctions.stream().flatMap(generator -> generator.apply(client, entry))),
                    context.recipeGenerators.stream().flatMap(generator -> generator.apply(client))
            )
            .map(recipe -> new PreparedRecipe(jeiHelpers, recipe))
            .collect(Collectors.groupingBy(PreparedRecipe::getCategory))
            .forEach((category, recipes) -> registration.addRecipes(getRecipeType(category), recipes));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
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

            <T extends HandledScreen<?>> void addExclusionZoneProvider(TlaApiJeiPluginContext.ExclusionZoneProvider<T> provider) {
                registration.addGuiContainerHandler(provider.clazz(), new IGuiContainerHandler<>() {
                    @Override
                    public List<Rect2i> getGuiExtraAreas(T containerScreen) {
                        return StreamSupport.stream(provider.provider().apply(containerScreen).spliterator(), false)
                                .map(bounds -> new Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height()))
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
}
