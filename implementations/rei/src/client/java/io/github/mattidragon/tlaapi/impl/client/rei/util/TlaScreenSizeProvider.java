package io.github.mattidragon.tlaapi.impl.client.rei.util;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;

import java.util.function.Function;

public record TlaScreenSizeProvider<T extends Screen> (Class<T> clazz, Function<T, TlaBounds> provider) implements DisplayBoundsProvider<T> {
    @Override
    public @Nullable Rectangle getScreenBounds(T screen) {
        return provider.apply(screen).apply(Rectangle::new);
    }

    @Override
    public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
        return clazz.isAssignableFrom(screen);
    }
}
