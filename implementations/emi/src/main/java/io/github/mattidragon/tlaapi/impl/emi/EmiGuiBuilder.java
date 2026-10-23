package io.github.mattidragon.tlaapi.impl.emi;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.mattidragon.tlaapi.api.client.gui.*;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.DoubleSupplier;

class EmiGuiBuilder implements GuiBuilder {
    private final TlaEmiRecipe recipe;
    private final WidgetHolder widgetHolder;

    public EmiGuiBuilder(TlaEmiRecipe recipe, WidgetHolder widgetHolder) {
        this.recipe = recipe;
        this.widgetHolder = widgetHolder;
    }

    @Override
    public SlotConfig addSlot(TlaIngredient ingredient, int x, int y) {
        return new EmiSlotConfig(widgetHolder.addSlot(EmiUtils.convertIngredient(ingredient), x, y));
    }

    @Override
    public WidgetConfig addTexture(TextureConfig config, int x, int y) {
        return new EmiWidgetConfig<>(widgetHolder.addTexture(config.lightTexture(),
                x, y,
                config.width(), config.height(),
                config.u(), config.v(),
                config.regionWidth(), config.regionHeight(),
                config.textureWidth(), config.textureHeight()));
    }

    @Override
    public WidgetConfig addAnimatedTexture(TextureConfig config, int x, int y, int duration, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return new EmiWidgetConfig<>(widgetHolder.addAnimatedTexture(config.lightTexture(),
                x, y,
                config.width(), config.height(),
                config.u(), config.v(),
                config.regionWidth(), config.regionHeight(),
                config.textureWidth(), config.textureHeight(),
                duration,
                horizontal,
                endToStart,
                fullToEmpty));
    }

    @Override
    public WidgetConfig addProgressingTexture(TextureConfig config, int x, int y, DoubleSupplier progress, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return new EmiWidgetConfig<>(widgetHolder.add(new ProgressingTextureWidget(config.lightTexture(),
                x, y,
                config.width(), config.height(),
                config.u(), config.v(),
                config.regionWidth(), config.regionHeight(),
                config.textureWidth(), config.textureHeight(),
                progress,
                horizontal,
                endToStart,
                fullToEmpty)));
    }

    @Override
    public WidgetConfig addArrow(int x, int y, boolean full) {
        return new EmiWidgetConfig<>(widgetHolder.addTexture(full ? EmiTexture.FULL_ARROW : EmiTexture.EMPTY_ARROW, x, y));
    }

    @Override
    public WidgetConfig addAnimatedArrow(int x, int y, int duration) {
        return new EmiWidgetConfig<>(widgetHolder.addFillingArrow(x, y, duration));
    }

    @Override
    public WidgetConfig addFlame(int x, int y, boolean full) {
        return new EmiWidgetConfig<>(widgetHolder.addTexture(full ? EmiTexture.FULL_FLAME : EmiTexture.EMPTY_FLAME, x, y));
    }

    @Override
    public WidgetConfig addAnimatedFlame(int x, int y, int duration) {
        widgetHolder.addTexture(EmiTexture.EMPTY_FLAME, x, y);
        return new EmiWidgetConfig<>(widgetHolder.addAnimatedTexture(EmiTexture.FULL_FLAME, x, y, duration, false, true, true));
    }

    @Override
    public WidgetConfig addPlusSign(int x, int y) {
        return new EmiWidgetConfig<>(widgetHolder.addTexture(EmiTexture.PLUS, x, y));
    }

    @Override
    public TextConfig addText(Text text, int x, int y, int color, boolean shadow) {
        return new EmiTextConfig(widgetHolder.addText(text, x, y, color, shadow));
    }

    @Override
    public WidgetConfig addCustomWidget(CustomTlaWidget widget) {
        return new EmiWidgetConfig<>(widgetHolder.add(new EmiCustomWidget(widget)));
    }

    @Override
    public void addTooltip(int x, int y, int width, int height, List<Text> tooltip) {
        widgetHolder.addTooltipText(tooltip, x, y, width, height);
    }

    @Override
    public TlaBounds getBounds() {
        return new TlaBounds(0, 0, widgetHolder.getWidth(), widgetHolder.getHeight());
    }

    private class EmiWidgetConfig<T extends EmiWidgetConfig<T, W>, W extends Widget> implements WidgetConfig {
        protected final W widget;

        private EmiWidgetConfig(W widget) {
            this.widget = widget;
        }

        @Override
        public T addTooltip(List<Text> tooltip) {
            var bounds = widget.getBounds();
            widgetHolder.addTooltipText(tooltip, bounds.x(), bounds.y(), bounds.width(), bounds.height());
            return getThis();
        }

        @Override
        public TlaBounds getBounds() {
            var emiBounds = widget.getBounds();
            return new TlaBounds(emiBounds.x(), emiBounds.y(), emiBounds.width(), emiBounds.height());
        }

        @SuppressWarnings("unchecked")
        protected T getThis() {
            return (T) this;
        }
    }

    private class EmiTextConfig extends EmiWidgetConfig<EmiTextConfig, TextWidget> implements TextConfig {
        public EmiTextConfig(TextWidget widget) {
            super(widget);
        }

        @Override
        public TextConfig alignLeft() {
            widget.horizontalAlign(TextWidget.Alignment.START);
            return this;
        }

        @Override
        public TextConfig alignRight() {
            widget.horizontalAlign(TextWidget.Alignment.END);
            return this;
        }

        @Override
        public TextConfig alignCenter() {
            widget.horizontalAlign(TextWidget.Alignment.CENTER);
            return this;
        }
    }

    private class EmiSlotConfig extends EmiWidgetConfig<EmiSlotConfig, SlotWidget> implements SlotConfig {
        private EmiSlotConfig(SlotWidget widget) {
            super(widget);
        }

        @Override
        public SlotConfig markOutput() {
            widget.recipeContext(recipe);
            return this;
        }

        @Override
        public SlotConfig markInput() {
            return this;
        }

        @Override
        public SlotConfig markCatalyst() {
            widget.catalyst(true);
            return this;
        }

        @Override
        public SlotConfig disableBackground() {
            widget.drawBack(false);
            return this;
        }

        @Override
        public SlotConfig makeLarge() {
            widget.large(true);
            return this;
        }

        @Override
        public EmiSlotConfig addTooltip(List<Text> tooltip) {
            tooltip.forEach(widget::appendTooltip);
            return this;
        }
    }
}
