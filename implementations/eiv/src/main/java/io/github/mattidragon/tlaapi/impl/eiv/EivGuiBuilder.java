package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

import de.crafty.eiv.common.recipe.inventory.RecipeViewMenu.OptionalSlotRenderer;
import io.github.mattidragon.tlaapi.api.client.gui.CustomTlaWidget;
import io.github.mattidragon.tlaapi.api.client.gui.GuiBuilder;
import io.github.mattidragon.tlaapi.api.client.gui.SlotConfig;
import io.github.mattidragon.tlaapi.api.client.gui.TextConfig;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.gui.WidgetConfig;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

class EivGuiBuilder implements GuiBuilder {
    private final TlaBounds bounds;

    List<SlotBuilder> slots = new ArrayList<>();
    List<Drawable> elements = new ArrayList<>();
    List<Runnable> tickers = new ArrayList<>();

    public EivGuiBuilder(int width, int height) {
        this.bounds = new TlaBounds(0, 0, width, height);
    }

    @Override
    public TlaBounds getBounds() {
        return bounds;
    }

    @Override
    public SlotConfig addSlot(TlaIngredient ingredient, int x, int y) {
        var slot = new SlotBuilder(ingredient, x, y);
        slots.add(slot);
        return slot;
    }

    @Override
    public WidgetConfig addTexture(TextureConfig config, int x, int y) {
        var widget = new WidgetBuilder() {
            @Override
            public TlaBounds getBounds() {
                return new TlaBounds(x, y, config.width(), config.height());
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
                context.drawTexture(RenderLayer::getGuiTextured, config.lightTexture(), x, y, config.u(), config.v(), config.width(), config.height(), config.textureWidth(), config.textureHeight());
                super.render(context, mouseX, mouseY, tickDelta);
            }
        };
        elements.add(widget);
        return widget;
    }

    @Override
    public WidgetConfig addAnimatedTexture(TextureConfig config, int x, int y, int duration, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        return addProgressingTexture(config, x, y, new DoubleSupplier() {
            private int frame;

            @Override
            public double getAsDouble() {
                frame = (frame + 1) % duration;
                return (double)frame / duration;
            }

        }, horizontal, endToStart, fullToEmpty);
    }

    @Override
    public WidgetConfig addProgressingTexture(TextureConfig config, int x, int y, DoubleSupplier progressSupplier, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
        var widget = new WidgetBuilder() {
            private float lastProgress;
            private float progress;
            @Override
            public TlaBounds getBounds() {
                return new TlaBounds(x, y, config.width(), config.height());
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
                float progress = MathHelper.lerp(tickDelta, lastProgress, this.progress);
                if (progress < 0) {
                    progress += 1;
                }
                if (fullToEmpty) {
                    progress = 1 - progress;
                }
                int xx = x, yy = y;
                int u = config.u(), v = config.v();
                int width = config.width(), height = config.height();

                if (horizontal) {
                    if (endToStart) {
                        width *= progress;
                    } else {
                        xx += (width * progress);
                        width *= (1 - progress);
                    }
                } else {
                    if (endToStart) {
                        width *= progress;
                    } else {
                        yy += (height * progress);
                        height *= (1 - progress);
                    }
                }

                context.drawTexture(RenderLayer::getGuiTextured, config.lightTexture(),
                        xx, yy,
                        u, v,
                        width, height,
                        config.textureWidth(), config.textureHeight());
                super.render(context, mouseX, mouseY, tickDelta);
            }

            void tick() {
                lastProgress = progress;
                progress = (float)progressSupplier.getAsDouble();
            }
        };
        elements.add(widget);
        tickers.add(widget::tick);
        return widget;
    }

    @Override
    public WidgetConfig addArrow(int x, int y, boolean full) {
        return addTexture(full ? EivUtil.FULL_ARROW : EivUtil.EMPTY_ARROW, x, y);
    }

    @Override
    public WidgetConfig addAnimatedArrow(int x, int y, int duration) {
        addArrow(x, y, false);
        return addAnimatedTexture(EivUtil.FULL_ARROW, x, y, duration, true, false, false);
    }

    @Override
    public WidgetConfig addFlame(int x, int y, boolean full) {
        return addTexture(full ? EivUtil.FULL_FLAME : EivUtil.EMPTY_FLAME, x, y);
    }

    @Override
    public WidgetConfig addAnimatedFlame(int x, int y, int duration) {
        addFlame(x, y, false);
        return addAnimatedTexture(EivUtil.FULL_FLAME, x, y, duration, false, false, true);
    }

    @Override
    public WidgetConfig addPlusSign(int x, int y) {
        return addTexture(EivUtil.PLUS, x, y);
    }

    @Override
    public TextConfig addText(Text text, int x, int y, int color, boolean shadow) {
        var widget = new TextBuilder(text, x, y, color, shadow);
        elements.add(widget);
        return widget;
    }

    @Override
    public WidgetConfig addCustomWidget(CustomTlaWidget custom) {
        var widget = new WidgetBuilder() {
            @Override
            public TlaBounds getBounds() {
                return custom.getBounds();
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
                custom.render(context, mouseX, mouseY, tickDelta);
                super.render(context, mouseX, mouseY, tickDelta);
            }
        };
        elements.add(widget);
        return widget;
    }

    @Override
    public void addTooltip(int x, int y, int width, int height, List<Text> tooltip) {

    }

    static abstract class WidgetBuilder implements WidgetConfig, Drawable {
        protected List<Text> tooltip = List.of();

        @Override
        public WidgetConfig addTooltip(List<Text> tooltip) {
            this.tooltip = List.copyOf(tooltip);
            return this;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            if (!tooltip.isEmpty()) {
                context.drawTooltip(null, tooltip, mouseX, mouseY);
            }
        }
    }

    static class TextBuilder extends WidgetBuilder implements TextConfig {
        Alignment alignment = Alignment.LEFT;

        private final Text text;
        private final int x;
        private final int y;
        private final int color;
        private final boolean shadow;

        public TextBuilder(Text text, int x, int y, int color, boolean shadow) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.shadow = shadow;
        }

        @Override
        public TextConfig alignLeft() {
            alignment = Alignment.LEFT;
            return this;
        }

        @Override
        public TextConfig alignRight() {
            alignment = Alignment.RIGHT;
            return this;
        }

        @Override
        public TextConfig alignCenter() {
            alignment = Alignment.CENTER;
            return this;
        }

        @Override
        public TextConfig addTooltip(List<Text> tooltip) {
            super.addTooltip(tooltip);
            return this;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            context.drawText(MinecraftClient.getInstance().textRenderer, text.asOrderedText(), x, y, color, shadow);
            super.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public TlaBounds getBounds() {
            var font = MinecraftClient.getInstance().textRenderer;
            int width = font.getWidth(text);

            int x = alignment.getX(this.x, width);
            return new TlaBounds(x, y, x + width, y + font.fontHeight);
        }

        enum Alignment {
            LEFT {
                @Override
                int getX(int x, int width) {
                    return x;
                }
            },
            RIGHT {
                @Override
                int getX(int x, int width) {
                    return x - width;
                }
            },
            CENTER {
                @Override
                int getX(int x, int width) {
                    return x - width / 2;
                }
            };

            abstract int getX(int x, int width);
        }

    }

    static class SlotBuilder extends WidgetBuilder implements SlotConfig, OptionalSlotRenderer {
        final TlaIngredient ingredient;
        private final TlaBounds bounds;

        public SlotBuilder(TlaIngredient ingredient, int x, int y) {
            this.bounds = new TlaBounds(x, y, 20, 20);
            this.ingredient = ingredient;
        }

        @Override
        public SlotConfig markOutput() {
            return this;
        }

        @Override
        public SlotConfig markInput() {
            return this;
        }

        @Override
        public SlotConfig markCatalyst() {
            return this;
        }

        @Override
        public SlotConfig disableBackground() {
            return this;
        }

        @Override
        public SlotConfig makeLarge() {
            return this;
        }

        @Override
        public SlotConfig addTooltip(List<Text> tooltip) {
            super.addTooltip(tooltip);
            return this;
        }

        @Override
        public TlaBounds getBounds() {
            return bounds;
        }
    }
}