package io.github.mattidragon.tlaapi.impl.client.rei.util;

import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.impl.rei.ReiUtil;
import me.shedaniel.math.Dimension;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;

public class TlaWidgets {
    private static final Renderer EMPTY_RENDERER = (graphics, bounds, mouseX, mouseY, delta) -> {};

    public static Renderer iconToRenderer(CategoryIcon icon) {
        return switch (icon) {
            case CategoryIcon.StackIcon stackIcon -> ReiUtil.convertStack(stackIcon.stack());
            case CategoryIcon.TextureIcon textureIcon -> new TextureWidget(0, 0, textureIcon.texture());
            case CategoryIcon.EmptyIcon empty -> EMPTY_RENDERER;
        };
    }

    public static AnimatedTextureWidget animatedFlame(int x, int y, int animationDurationMs) {
        return new AnimatedTextureWidget.Builder(x, y, 14, 14)
                .animationDuration(animationDurationMs)
                .fullToEmpty()
                .endToStart()
                .defaultTexture()
                .uv(82, 77)
                .build();
    }

    public static TextureWidget reiTexture(Point point, int u, int v, int width, int height) {
        return new TextureWidget.Builder(new Rectangle(point, new Dimension(width, height)))
                .defaultTexture()
                .uv(u, v)
                .build();
    }
}
