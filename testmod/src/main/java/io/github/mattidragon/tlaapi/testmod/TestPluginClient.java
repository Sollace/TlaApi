package io.github.mattidragon.tlaapi.testmod;

import java.util.List;
import java.util.function.DoubleSupplier;

import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.api.client.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.client.plugin.ClientPluginContext;
import io.github.mattidragon.tlaapi.api.client.plugin.TlaApiClientPlugin;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.testmod.content.TestScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TestPluginClient implements TlaApiClientPlugin {
    @Override
    public void register(ClientPluginContext context) {
        context.addStackDragHandler(TestScreen.class, screen -> {
            var bounds = new TlaBounds(screen.width / 2, screen.height / 2, 18, 18);
            return List.of(new StackDragHandler.DropTarget(bounds, stack -> {
                if (stack instanceof TlaStack.TlaItemStack itemStack) {
                    screen.filterA = itemStack.toStack();
                    return true;
                }
                return false;
            }));
        });
        context.addScreenSizeProvider(TestScreen.class, screen -> new TlaBounds(screen.getX(), screen.getY(), screen.getWidth(), screen.getHeight()));
        context.addExclusionZoneProvider(TestScreen.class, screen -> List.of(new TlaBounds(screen.getX() + screen.getWidth(), screen.getY(), 100, screen.getHeight())));
        context.addScreenHandlerClickArea(TestScreen.class, TestPlugin.TEST_CATEGORY, screen -> new TlaBounds(10, 10, 18, 18));
        context.registerGui(TestPlugin.TEST_PROGRESS_BAR_SERIALIZER, (recipe, builder) -> {
            var texture = TextureConfig.builder()
                    .texture(Identifier.ofVanilla("textures/block/dirt.png"), Identifier.ofVanilla("textures/block/stone.png"))
                    .size(35, 10)
                    .regionSize(16, 16)
                    .textureSize(16, 16)
                    .build();
            DoubleSupplier manualProgress = () -> {
                var time = System.currentTimeMillis() % 1000;
                return time > 500 ? 1 - (time - 500) / 500.0 : time / 500.0;
            };

            builder.addAnimatedTexture(texture, 0, 0, 1000, true, false, false)
                    .addTooltip(Text.literal("1000ms, horizontal, start to end, empty to full"));
            builder.addAnimatedTexture(texture, 0, 10, 1000, true, true, false)
                    .addTooltip(Text.literal("1000ms, horizontal, end to start, empty to full"));
            builder.addAnimatedTexture(texture, 0, 20, 1000, true, false, true)
                    .addTooltip(Text.literal("1000ms, horizontal, start to end, full to empty"));
            builder.addAnimatedTexture(texture, 0, 30, 1000, true, true, true)
                    .addTooltip(Text.literal("1000ms, horizontal, end to start, full to empty"));

            builder.addProgressingTexture(texture, 36, 0, manualProgress, true, false, false)
                    .addTooltip(Text.literal("custom, horizontal, start to end, empty to full"));
            builder.addProgressingTexture(texture, 36, 10, 0.25, true, true, false)
                    .addTooltip(Text.literal("fixed, horizontal, end to start, empty to full"));
            builder.addProgressingTexture(texture, 36, 20, manualProgress, true, false, true)
                    .addTooltip(Text.literal("custom, horizontal, start to end, full to empty"));
            builder.addProgressingTexture(texture, 36, 30, 0.25, true, true, true)
                    .addTooltip(Text.literal("fixed, horizontal, end to start, full to empty"));
        });
        context.registerGui(TestPlugin.TEST_CUSTOM_SERIALIZER, (recipe, builder) -> {
            builder.addSlot(recipe.getInputs().getFirst(), 0, 0).markInput();
            builder.addSlot(recipe.getOutputs().getFirst(), 54, 0).markOutput();
            builder.addAnimatedArrow(24, 0, 1000).addTooltip(Text.literal("1s"));
            builder.addSlot(recipe.getCatalysts().getFirst(), 27, 20).markCatalyst();
            builder.addFlame(0, 18);
            builder.addAnimatedFlame(54, 18, 1000);
        });
        context.registerGui(TestPlugin.TEST_TLA_RECIPE_SERIALIZER, (recipe, builder) -> {
            var dirtTexture = TextureConfig.builder()
                    .texture(Identifier.ofVanilla("textures/block/dirt.png"))
                    .fullSize(16, 16)
                    .build();

            builder.addSlot(TlaIngredient.ofSlotDisplay(recipe.recipe().input()), 0, 4).addTooltip(Text.literal("hi")).markInput();
            builder.addTexture(dirtTexture, 18, 0);
            builder.addArrow(24, 4, false).addTooltip(Text.literal("am arrow"));
            builder.addArrow(24, 24, true).addTooltip(Text.literal("am full arrow"));
            builder.addSlot(TlaStack.of(recipe.recipe().result()).withChance(0.5), 46, 0).markOutput().makeLarge().addTooltip(Text.literal("am large"));
            builder.addCustomWidget(0, 22, 16, 16, (ctx, mouseX, mouseY, delta) -> {
                ctx.drawTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("textures/block/cobblestone.png"), 0, 0, 0, 0, 16, 16, 16, 16);
            }).addTooltip(Text.literal("am custom widget"));
        });
        context.registerGui(TestPlugin.TEST_TEXT_SERIALIZER, (recipe, builder) -> {
            var center = builder.getBounds().centerX();
            builder.addText(Text.literal("default"), center, 0, false);
            builder.addText(Text.literal("left"), center, 9, false).alignLeft();
            builder.addText(Text.literal("center"), center, 18, false).alignCenter().addTooltip(Text.literal("Text tooltip"));
            builder.addText(Text.literal("right"), center, 27, false).alignRight();
        });
    }

}
