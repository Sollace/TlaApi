package io.github.mattidragon.tlaapi.impl.client.rei;

import io.github.mattidragon.tlaapi.api.client.gui.*;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.impl.rei.ReiUtil;
import io.github.mattidragon.tlaapi.impl.Untyped;
import io.github.mattidragon.tlaapi.impl.client.rei.util.AnimatedTextureWidget;
import io.github.mattidragon.tlaapi.impl.client.rei.util.ReiCustomWidget;
import io.github.mattidragon.tlaapi.impl.client.rei.util.TextureWidget;
import io.github.mattidragon.tlaapi.impl.client.rei.util.TlaWidgets;
import me.shedaniel.math.Dimension;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

class ReiGuiBuilder implements GuiBuilder {
    private final List<Widget> widgets = new ArrayList<>();
    private final Rectangle bounds;

    public ReiGuiBuilder(Rectangle bounds) {
        this.bounds = bounds.clone();
        this.bounds.grow(-5, -5);
        widgets.add(Widgets.createRecipeBase(bounds));
    }

    @Override
    public SlotConfig addSlot(TlaIngredient ingredient, int x, int y) {
        var widget = Widgets.createSlot(getPoint(x + 1, y + 1));
        widget.entries(ReiUtil.convertIngredient(ingredient));
        return addWidget(widget, ReiSlotConfig::new);
    }

    @Override
    public WidgetConfig addTexture(TextureConfig config, int x, int y) {
        return addWidget(new TextureWidget(x + bounds.x, y + bounds.y, config), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addAnimatedTexture(TextureConfig config, int x, int y, int duration, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return addWidget(new AnimatedTextureWidget.Builder(x + bounds.x, y + bounds.y, config.width(), config.height())
                .animationDuration(duration)
                .texture(config.darkTexture(), config.lightTexture())
                .flags(horizontal, endToStart, fullToEmpty)
                .uv(config.u(), config.v())
                .regionSize(config.regionWidth(), config.regionHeight())
                .textureSize(config.textureWidth(), config.textureHeight())
                .build(), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addProgressingTexture(TextureConfig config, int x, int y, DoubleSupplier progress, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return addWidget(new AnimatedTextureWidget.Builder(x + bounds.x, y + bounds.y, config.width(), config.height())
                .progress(progress)
                .texture(config.darkTexture(), config.lightTexture())
                .flags(horizontal, endToStart, fullToEmpty)
                .uv(config.u(), config.v())
                .regionSize(config.regionWidth(), config.regionHeight())
                .textureSize(config.textureWidth(), config.textureHeight())
                .build(), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addArrow(int x, int y, boolean full) {
        return addWidget(TlaWidgets.reiTexture(getPoint(x, y), full ? 82 : 106, 91, 24, 17), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addAnimatedArrow(int x, int y, int duration) {
        return addWidget(Widgets.createArrow(getPoint(x, y)).animationDurationMS(duration), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addFlame(int x, int y, boolean full) {
        return addWidget(TlaWidgets.reiTexture(getPoint(x, y), full ? 82 : 1, full ? 77 : 74, 14, 14), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addAnimatedFlame(int x, int y, int duration) {
        var point = getPoint(x, y);
        widgets.add(TlaWidgets.reiTexture(point, 1, 74, 14, 14));
        return addWidget(TlaWidgets.animatedFlame(point, 1000), ReiWidgetConfig::new);
    }

    @Override
    public WidgetConfig addPlusSign(int x, int y) {
        return addWidget(TlaWidgets.plusSign(getPoint(x, y)), ReiWidgetConfig::new);
    }

    @Override
    public TextConfig addText(Text text, int x, int y, boolean shadow) {
        return addWidget(Widgets.createLabel(getPoint(x, y), text).shadow(shadow), ReiTextConfig::new);
    }

    @Override
    public TextConfig addText(Text text, int x, int y, int color, boolean shadow) {
        return addWidget(Widgets.createLabel(getPoint(x, y), text).shadow(shadow).color(color), ReiTextConfig::new);
    }

    @Override
    public WidgetConfig addCustomWidget(CustomTlaWidget widget) {
        return addWidget(new ReiCustomWidget(widget, bounds), ReiWidgetConfig::new);
    }

    @Override
    public void addTooltip(int x, int y, int width, int height, List<Text> tooltip) {
        widgets.add(Widgets.createTooltip(new Rectangle(getPoint(x, y), new Dimension(width, height)), tooltip));
    }

    private <T extends WidgetConfig, W extends WidgetWithBounds> T addWidget(W widget, Function<W, ? extends T> config) {
        widgets.add(widget);
        return config.apply(widget);
    }

    @Override
    public TlaBounds getBounds() {
        return new TlaBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    private Point getPoint(int x, int y) {
        return new Point(bounds.x + x, bounds.y + y);
    }

    private class ReiWidgetConfig<T extends ReiWidgetConfig<T, W>, W extends WidgetWithBounds> implements WidgetConfig {
        protected W widget;

        private ReiWidgetConfig(W widget) {
            this.widget = widget;
        }

        @Override
        public T addTooltip(List<Text> tooltip) {
            widgets.add(Widgets.createTooltip(widget::getBounds, tooltip));
            return getThis();
        }

        @Override
        public TlaBounds getBounds() {
            var reiBounds = widget.getBounds();
            return new TlaBounds(reiBounds.x + bounds.x, reiBounds.y + bounds.y, reiBounds.width, reiBounds.height);
        }

        protected T getThis() {
            return Untyped.cast(this);
        }
    }

    private class ReiTextConfig extends ReiWidgetConfig<ReiTextConfig, Label> implements TextConfig {
        private ReiTextConfig(Label label) {
            super(label);
            alignLeft();
        }

        @Override
        public TextConfig alignLeft() {
            widget.leftAligned();
            return this;
        }

        @Override
        public TextConfig alignRight() {
            widget.rightAligned();
            return this;
        }

        @Override
        public TextConfig alignCenter() {
            widget.centered();
            return this;
        }

        @Override
        public ReiTextConfig addTooltip(List<Text> tooltip) {
            widget.setTooltip(tooltip.toArray(Text[]::new));
            return this;
        }
    }

    private class ReiSlotConfig extends ReiWidgetConfig<ReiSlotConfig, Slot> implements SlotConfig {
        public ReiSlotConfig(Slot widget) {
            super(widget);
        }

        @Override
        public SlotConfig markOutput() {
            widget.markOutput();
            return this;
        }

        @Override
        public SlotConfig markInput() {
            widget.markInput();
            return this;
        }

        @Override
        public SlotConfig markCatalyst() {
            return this;
        }

        @Override
        public SlotConfig disableBackground() {
            widget.disableBackground();
            return this;
        }

        @Override
        public SlotConfig makeLarge() {
            disableBackground();
            widget.getBounds().translate(4, 4);
            var backgroundLocation = widget.getBounds().getLocation();
            backgroundLocation.translate(1, 1);
            widgets.add(1, Widgets.createResultSlotBackground(backgroundLocation));
            return this;
        }

        @Override
        public ReiSlotConfig addTooltip(List<Text> tooltip) {
            widget.getEntries().forEach(entry -> entry.tooltip(tooltip));
            return this;
        }
    }
}
