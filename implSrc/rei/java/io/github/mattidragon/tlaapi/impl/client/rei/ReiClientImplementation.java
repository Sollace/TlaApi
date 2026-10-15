package io.github.mattidragon.tlaapi.impl.client.rei;

import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContextBackend;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.rei.ReiImplementation;
import io.github.mattidragon.tlaapi.impl.rei.util.TlaDragHandler;
import io.github.mattidragon.tlaapi.impl.rei.util.TlaExclusionZoneProvider;
import io.github.mattidragon.tlaapi.impl.rei.util.TlaScreenSizeProvider;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import java.util.*;
import java.util.function.Function;

class ReiClientImplementation implements ClientPluginContextBackend {
    static ReiClientImplementation INSTANCE;

    public final List<TlaDragHandler<?>> stackDragHandlers = new ArrayList<>();
    public final List<TlaScreenSizeProvider<?>> screenSizeProviders = new ArrayList<>();
    public final List<TlaExclusionZoneProvider<?>> exclusionZoneProviders = new ArrayList<>();
    public final List<ClickAreaTuple<?>> clickAreas = new ArrayList<>();

    public ReiClientImplementation() {
        INSTANCE = this;
    }

    @Override
    public PluginContext getContext() {
        return ReiImplementation.INSTANCE;
    }

    @Override
    public <T extends Screen> void addClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        clickAreas.add(new ClickAreaTuple<>(clazz, category, boundsFunction, false));
    }

    @Override
    public <T extends HandledScreen<?>> void addScreenHandlerClickArea(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction) {
        clickAreas.add(new ClickAreaTuple<>(clazz, category, boundsFunction, true));
    }

    @Override
    public <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler) {
        stackDragHandlers.add(new TlaDragHandler<>(handler, clazz));
    }

    @Override
    public <T extends Screen> void addScreenSizeProvider(Class<T> clazz, Function<T, TlaBounds> provider) {
        screenSizeProviders.add(new TlaScreenSizeProvider<>(clazz, provider));
    }

    @Override
    public <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider) {
        exclusionZoneProviders.add(new TlaExclusionZoneProvider<>(clazz, provider));
    }

    @Override
    public String toString() {
        return "REI plugin handler";
    }

    public record ClickAreaTuple<T extends Screen>(Class<T> clazz, TlaCategory category, Function<T, TlaBounds> boundsFunction, boolean handledScreenCoords) {}
}
