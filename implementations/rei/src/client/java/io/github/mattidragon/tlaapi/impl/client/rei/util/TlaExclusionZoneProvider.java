package io.github.mattidragon.tlaapi.impl.client.rei.util;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZonesProvider;
import net.minecraft.client.gui.screen.Screen;

import java.util.Collection;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;

public record TlaExclusionZoneProvider<T extends Screen>(
        Class<T> clazz,
        TlaBounds.Collection<T> provider
    ) implements ExclusionZonesProvider<T> {
    @Override
    public Collection<Rectangle> provide(T screen) {
        return provider.get(screen, Rectangle::new);
    }
}
