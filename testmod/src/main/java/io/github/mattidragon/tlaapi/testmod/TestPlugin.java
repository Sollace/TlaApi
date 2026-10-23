package io.github.mattidragon.tlaapi.testmod;

import io.github.mattidragon.tlaapi.api.plugin.PluginContext;
import io.github.mattidragon.tlaapi.api.plugin.TlaApiPlugin;
import io.github.mattidragon.tlaapi.api.recipe.StableRecipeIdentifier;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack.TlaItemStack;
import io.github.mattidragon.tlaapi.testmod.content.TestMod;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.stream.Stream;

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
        context.addGenerator(client -> Stream.of(new TestCustomRecipe(), new TestTextDisplay(), new ProgressBarTestRecipe()));

        // Remove a vanilla recipe
        context.removeRecipes(StableRecipeIdentifier.of(RegistryKey.of(RegistryKeys.RECIPE, Identifier.of("crafting_table"))));
        // Remove a vanilla recipe (by result)
        context.removeRecipes(recipe -> {
            return recipe.getOutputs().stream().anyMatch(i -> i instanceof TlaItemStack stack && stack.getItem() == Items.TORCH);
        });
        // Remove custom recipe by id
        context.removeRecipes(Identifier.of("testmod", "test_removed"));

        // Remove stack
        context.removeStacks(i -> i instanceof TlaItemStack stack && stack.getItem() == Items.BAT_SPAWN_EGG);
    }
}
