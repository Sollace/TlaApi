package io.github.mattidragon.tlaapi.testmod;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.testmod.content.TestMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;

public record TestRecipeDisplay(SlotDisplay input, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {
    public static final MapCodec<TestRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            SlotDisplay.CODEC.fieldOf("input").forGetter(TestRecipeDisplay::input),
            SlotDisplay.CODEC.fieldOf("result").forGetter(TestRecipeDisplay::result),
            SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(TestRecipeDisplay::craftingStation)
    ).apply(i, TestRecipeDisplay::new));
    public static final PacketCodec<RegistryByteBuf, TestRecipeDisplay> PACKET_CODEC = PacketCodec.tuple(
            SlotDisplay.PACKET_CODEC, TestRecipeDisplay::input,
            SlotDisplay.PACKET_CODEC, TestRecipeDisplay::result,
            SlotDisplay.PACKET_CODEC, TestRecipeDisplay::craftingStation,
            TestRecipeDisplay::new
    );

    @Override
    public Serializer<? extends RecipeDisplay> serializer() {
        return TestMod.RECIPE_DISPLAY_SERIALIZER;
    }
}
