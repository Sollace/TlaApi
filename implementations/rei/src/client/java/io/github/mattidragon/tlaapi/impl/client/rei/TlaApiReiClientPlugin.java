package io.github.mattidragon.tlaapi.impl.client.rei;

import dev.architectury.event.EventResult;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.api.plugin.RecipeViewer;
import io.github.mattidragon.tlaapi.impl.rei.ReiImplementation;
import io.github.mattidragon.tlaapi.impl.rei.ReiTlaRecipe;
import io.github.mattidragon.tlaapi.impl.rei.ReiUtil;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.screen.SimpleClickArea;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

public class TlaApiReiClientPlugin implements REIClientPlugin {
    @Override
    public void preStage(PluginManager<REIClientPlugin> manager, ReloadStage stage) {
        // REI doesn't have a good reload start event, so we have to do this
        if (stage == ReloadStage.START && manager == PluginManager.getClientInstance()) {
            PluginLoader.loadPlugins(RecipeViewer.REI, ReiImplementation::new);
            ClientPluginLoader.loadPlugins(new ReiClientImplementation());
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        ReiImplementation.INSTANCE.categories.forEach((category, id) -> {
            registry.add(new TlaDisplayCategory(category, id));
        });
        ReiImplementation.INSTANCE.workstations.forEach((category, workstation) -> registry.addWorkstations(ReiImplementation.INSTANCE.getCategoryIdentifier(category), ReiUtil.convertIngredient(workstation)));
        ReiImplementation.INSTANCE.builtInCategories.forEach((id, category) -> {
            category.category().set(registry.get(id).getCategory());
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        ReiImplementation.INSTANCE.registerDisplays(registry);
        registry.registerVisibilityPredicate((category, display) -> {
            var recipe = ReiTlaRecipe.of(display);

            if (ReiImplementation.INSTANCE.recipeHidePredicate.test(recipe)) {
                return EventResult.interruptFalse();
            }

            return EventResult.pass();
        });
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry -> ReiImplementation.INSTANCE.hidePredicate.test(ReiUtil.convertStack(entry)));
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        ReiClientImplementation.INSTANCE.exclusionZoneProviders.forEach(provider -> zones.register(provider.clazz, provider));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        ReiClientImplementation.INSTANCE.stackDragHandlers.forEach(registry::registerDraggableStackVisitor);
        ReiClientImplementation.INSTANCE.screenSizeProviders.forEach(registry::registerDecider);
        ReiClientImplementation.INSTANCE.clickAreas.forEach(tuple -> registerClickArea(registry, tuple));
    }

    @SuppressWarnings("unchecked")
    private <T extends Screen> void registerClickArea(ScreenRegistry registry, ReiClientImplementation.ClickAreaTuple<T> tuple) {
        SimpleClickArea<T> clickArea = screen -> {
            var bounds = tuple.boundsFunction().apply(screen);
            return new Rectangle(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        };
        if (tuple.handledScreenCoords()) {
            if (!HandledScreen.class.isAssignableFrom(tuple.clazz())) {
                throw new IllegalArgumentException("Can't use screen handler coordinates for non-HandledScreen! (%s is not assignable to %s)".formatted(tuple.clazz().getSimpleName(), HandledScreen.class.getSimpleName()));
            }
            // We check that this works manually above
            //noinspection unchecked
            registry.registerContainerClickArea((SimpleClickArea<HandledScreen<ScreenHandler>>) clickArea,
                    (Class<HandledScreen<ScreenHandler>>) tuple.clazz(),
                    ReiImplementation.INSTANCE.getCategoryIdentifier(tuple.category()));
        } else {
            registry.registerClickArea(clickArea, tuple.clazz(), ReiImplementation.INSTANCE.getCategoryIdentifier(tuple.category()));
        }
    }
}
