package io.github.mattidragon.tlaapi.testmod.content;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.testmod.TestRecipeDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay.ItemSlotDisplay;
import net.minecraft.recipe.display.SlotDisplay.StackSlotDisplay;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public record TestRecipe(Ingredient input, ItemStack output) implements Recipe<CraftingRecipeInput> {
    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        return input.test(inventory.getStackInSlot(0));
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup lookup) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<TestRecipe> getSerializer() {
        return TestMod.RECIPE_SERIALIZER;
    }

    @Override
    public RecipeType<TestRecipe> getType() {
        return TestMod.RECIPE_TYPE;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.forSingleSlot(input);
    }

    @Override
    public List<RecipeDisplay> getDisplays() {
        return List.of(new TestRecipeDisplay(
                input.toDisplay(), new StackSlotDisplay(output), new ItemSlotDisplay(Items.CRAFTING_TABLE))
        );
    }

    @Override
    public RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.BLAST_FURNACE_BLOCKS;
    }

    public static class Serializer implements RecipeSerializer<TestRecipe> {
        private static final MapCodec<TestRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(TestRecipe::input),
            ItemStack.CODEC.fieldOf("output").forGetter(TestRecipe::output)
        ).apply(instance, TestRecipe::new));
        @Deprecated
        private static final PacketCodec<RegistryByteBuf, TestRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC, TestRecipe::input,
                ItemStack.PACKET_CODEC, TestRecipe::output,
                TestRecipe::new);

        @Override
        public MapCodec<TestRecipe> codec() {
            return CODEC;
        }

        @Deprecated
        @Override
        public PacketCodec<RegistryByteBuf, TestRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}
