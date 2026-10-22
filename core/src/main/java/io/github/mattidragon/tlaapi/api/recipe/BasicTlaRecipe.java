package io.github.mattidragon.tlaapi.api.recipe;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.impl.MoreCodecs;
import io.github.mattidragon.tlaapi.impl.Untyped;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public record BasicTlaRecipe (
        TlaCategory category,
        @Nullable RegistryKey<Recipe<?>> id,
        Supplier<List<TlaIngredient>> inputs,
        Supplier<List<TlaStack>> outputs,
        Supplier<List<TlaIngredient>> catalysts
) implements TlaRecipe {
    private static final MapCodec<BasicTlaRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TlaCategory.CODEC.fieldOf("category").forGetter(BasicTlaRecipe::category),
            RegistryKey.createCodec(RegistryKeys.RECIPE).fieldOf("id").forGetter(BasicTlaRecipe::id),
            MoreCodecs.supplierOf(TlaIngredient.CODEC.listOf()).fieldOf("inputs").forGetter(BasicTlaRecipe::inputs),
            MoreCodecs.supplierOf(TlaStack.CODEC.listOf()).fieldOf("outputs").forGetter(BasicTlaRecipe::outputs),
            MoreCodecs.supplierOf(TlaIngredient.CODEC.listOf()).fieldOf("inputs").forGetter(BasicTlaRecipe::catalysts)
    ).apply(i, BasicTlaRecipe::new));
    private static final PacketCodec<RegistryByteBuf, BasicTlaRecipe> PACKET_CODEC = PacketCodec.tuple(
            TlaCategory.PACKET_CODEC, BasicTlaRecipe::category,
            RegistryKey.createPacketCodec(RegistryKeys.RECIPE), BasicTlaRecipe::id,
            MoreCodecs.supplierOf(TlaIngredient.PACKET_CODEC.collect(PacketCodecs.toList())), BasicTlaRecipe::inputs,
            MoreCodecs.supplierOf(TlaStack.PACKET_CODEC.collect(PacketCodecs.toList())), BasicTlaRecipe::outputs,
            MoreCodecs.supplierOf(TlaIngredient.PACKET_CODEC.collect(PacketCodecs.toList())), BasicTlaRecipe::catalysts,
            BasicTlaRecipe::new
    );
    public static final TlaRecipeSerializer<BasicTlaRecipe> SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla", "basic_recipe"), MAP_CODEC, PACKET_CODEC);

    public BasicTlaRecipe {
        inputs = Untyped.memoize(inputs);
        outputs = Untyped.memoize(outputs);
        catalysts = Untyped.memoize(catalysts);
    }

    @Override
    public @Nullable RegistryKey<Recipe<?>> getId() {
        return id;
    }

    @Override
    public @NotNull TlaRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public TlaCategory getCategory() {
        return category;
    }

    @Override
    public List<TlaIngredient> getInputs() {
        return inputs.get();
    }

    @Override
    public List<TlaStack> getOutputs() {
        return outputs.get();
    }

    @Override
    public List<TlaIngredient> getCatalysts() {
        return catalysts.get();
    }
}
