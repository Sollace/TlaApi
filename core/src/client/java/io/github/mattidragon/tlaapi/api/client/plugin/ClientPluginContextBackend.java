package io.github.mattidragon.tlaapi.api.client.plugin;

import java.util.List;
import java.util.function.Function;

import io.github.mattidragon.tlaapi.api.annotation.SupportsExcept;
import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

@ImplementationsExtend
public interface ClientPluginContextBackend {
    /**
     * Gets the main plugin context corresponding to this client plugin context.
     */
    PluginContext getContext();

    /**
     * Adds an area which can be clicked to open a category if the recipe viewer supports it.
     * The bounds are based on screen coordinates. To use handled screen coordinates use {@link #addScreenHandlerClickArea}.
     * It should also be preferred whenever possible due to better compatibility.
     * @param screenType The class of the screen for which this applies.
     * @param category The category which should be opened.
     * @param boundsFunction The function that supplies the bounds.
     * @see #addScreenHandlerClickArea(Class, TlaCategory, Function)
     * @implNote EMI does not provide this functionality, as such this method is a no-op when running through it.
     * <br>JEI doesn't provide the ability to add click areas to non-handled screens, as such this method won't work there.
     */
    @SupportsExcept({ "eiv", "emi", "jei" })
    <T extends Screen> void addClickArea(Class<T> screenType, TlaCategory category, Function<T, TlaBounds> boundsFunction);

    /**
     * Adds an area which can be clicked to open a category if the recipe viewer supports it.
     * The bounds are based on handled screen coordinates based on the reported screen size. For non-handled screens, see {@link #addClickArea}.
     * @param screenType The class of the screen for which this applies.
     * @param category The category which should be opened.
     * @param boundsFunction The function that supplies the bounds.
     * @see #addClickArea(Class, TlaCategory, Function)
     * @implNote EMI does not provide this functionality, as such this method is a no-op when running through it.
     */
    @SupportsExcept({ "eiv", "emi" })
    <C extends ScreenHandler, T extends HandledScreen<C>> void addScreenHandlerClickArea(Class<T> screenType, TlaCategory category, Function<T, TlaBounds> boundsFunction);

    /**
     * Adds a stack drag handler for a given screen type.
     * Stack drag handlers are used to handle dragging stacks from the recipe viewer to a screen,
     * usually for setting filters or similar things.
     */
    @SupportsExcept("eiv")
    <T extends Screen> void addStackDragHandler(Class<T> clazz, StackDragHandler<T> handler);

    /**
     * Adds a screen size provider for a given screen type.
     * This is used by REI to position itself.
     * With EMI this adds an exclusion zone instead preventing EMI from extending onto the screen.
     * Usually calling this is unnecessary as the default implementations based on handled screens are sufficient.
     * @see #addExclusionZoneProvider
     */
    @SupportsExcept({ "eiv" })
    default <T extends Screen> void addScreenSizeProvider(Class<T> clazz, Function<T, TlaBounds> provider) {
        addExclusionZoneProvider(clazz, screen -> List.of(provider.apply(screen)));
    }

    /**
     * Adds an exclusion zone provider for a given screen type.
     * This prevents recipe viewers from extending onto areas of the screen.
     */
    @SupportsExcept({ "eiv" })
    <T extends Screen> void addExclusionZoneProvider(Class<T> clazz, Function<T, ? extends Iterable<TlaBounds>> provider);
}
