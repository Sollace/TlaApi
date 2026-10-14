package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.StackDragHandler;
import io.github.mattidragon.tlaapi.api.gui.TlaBounds;
import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.TlaApiPlugin;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.testmod.content.TestMod;
import io.github.mattidragon.tlaapi.testmod.content.TestScreen;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

import java.util.List;

import com.mojang.serialization.MapCodec;

public class TestPlugin implements TlaApiPlugin {
    public static final TestCategory TEST_CATEGORY = new TestCategory();

    public static final TlaRecipeSerializer<TestTextDisplay> TEST_TEXT_SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla_test", "text_display"), MapCodec.unit(TestTextDisplay::new), PacketCodec.unit(new TestTextDisplay()));
    public static final TlaRecipeSerializer<TestCustomRecipe> TEST_CUSTOM_SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla_test", "custom_recipe"), MapCodec.unit(TestCustomRecipe::new), PacketCodec.unit(new TestCustomRecipe()));
    public static final TlaRecipeSerializer<ProgressBarTestRecipe> TEST_PROGRESS_BAR_SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla_test", "progress_bar"), MapCodec.unit(ProgressBarTestRecipe::new), PacketCodec.unit(new ProgressBarTestRecipe()));
    public static final TlaRecipeSerializer<TestTlaRecipe> TEST_TLA_RECIPE_SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla_test", "tla_recipe"),
            TestMod.RECIPE_DISPLAY_SERIALIZER.codec().xmap(TestTlaRecipe::new, TestTlaRecipe::recipe),
            TestMod.RECIPE_DISPLAY_SERIALIZER.streamCodec().xmap(TestTlaRecipe::new, TestTlaRecipe::recipe)
    );

    @Override
    public void register(PluginContext context) {
        context.addCategory(TEST_CATEGORY);
        context.addWorkstation(TEST_CATEGORY, TlaStack.of(Items.DIRT).asIngredient());
        context.addRecipeGenerator(TestMod.RECIPE_DISPLAY_SERIALIZER, TestTlaRecipe::new);
        context.addGenerator(client -> List.of(new TestCustomRecipe(), new TestTextDisplay(), new ProgressBarTestRecipe()));
        context.addStackDragHandler(TestScreen.class, screen -> {
            var bounds = new TlaBounds(screen.width / 2, screen.height / 2, 18, 18);
            return List.of(new StackDragHandler.DropTarget(bounds, stack -> {
                if (stack instanceof TlaStack.TlaItemStack itemStack) {
                    screen.filterA = itemStack.getItemVariant();
                    return true;
                }
                return false;
            }));
        });
        context.addScreenSizeProvider(TestScreen.class, screen -> new TlaBounds(screen.getX(), screen.getY(), screen.getWidth(), screen.getHeight()));
        context.addExclusionZoneProvider(TestScreen.class, screen -> List.of(new TlaBounds(screen.getX() + screen.getWidth(), screen.getY(), 100, screen.getHeight())));
        context.addScreenHandlerClickArea(TestScreen.class, TEST_CATEGORY, screen -> new TlaBounds(10, 10, 18, 18));
    }
}
