package io.github.mattidragon.tlaapi.impl.client.rei.util;

import java.util.function.Function;

import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.rei.ReiImplementation;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.screen.SimpleClickArea;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.client.gui.screen.Screen;

public record ClickAreaTuple<T extends Screen>(
        Class<T> clazz,
        TlaCategory category,
        Function<T, TlaBounds> boundsFunction,
        RegistrationFunction<T> registrationFunction) implements SimpleClickArea<T> {

    public interface RegistrationFunction<T extends Screen> {
        void registerClickArea(ScreenRegistry registry, SimpleClickArea<T> area, Class<? extends T> screenClass, CategoryIdentifier<?>... categories);
    }

    @Override
    public Rectangle provide(T screen) {
        return boundsFunction().apply(screen).apply(Rectangle::new);
    }

    public void register(ScreenRegistry registry) {
        registrationFunction().registerClickArea(registry, this, clazz(), ReiImplementation.INSTANCE.getCategoryIdentifier(category()));
    }
}