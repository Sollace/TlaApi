package io.github.mattidragon.tlaapi.impl.rei;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipeSerializer;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public record ReiTlaRecipe(
        Display display,
        TlaCategory category,
        Supplier<List<TlaIngredient>> inputs,
        Supplier<List<TlaStack>> outputs
) implements TlaRecipe {
    private static final MapCodec<ReiTlaRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Display.codec().fieldOf("display").forGetter(ReiTlaRecipe::display)
    ).apply(i, ReiTlaRecipe::new));
    private static final PacketCodec<RegistryByteBuf, ReiTlaRecipe> PACKET_CODEC = Display.streamCodec().xmap(ReiTlaRecipe::new, ReiTlaRecipe::display);
    private static final TlaRecipeSerializer<ReiTlaRecipe> SERIALIZER = TlaRecipeSerializer.register(Identifier.of("tla_rei", "recipe"), MAP_CODEC, PACKET_CODEC);
    public static TlaRecipe of(Display display) {
        if (display instanceof TlaDisplay tla) {
            return tla.getRecipe();
        }
        return new ReiTlaRecipe(display);
    }

    private ReiTlaRecipe(Display display) {
        this(display,
                ReiImplementation.INSTANCE.lookupCategory(display.getCategoryIdentifier()),
                Suppliers.memoize(() -> display.getInputEntries().stream().map(ReiUtil::convertIngredient).toList()),
                Suppliers.memoize(() -> display.getOutputEntries().stream().flatMap(List::stream).map(ReiUtil::convertStack).toList())
        );
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
    public @Nullable RegistryKey<Recipe<?>> getId() {
        return display.getDisplayLocation().map(id -> RegistryKey.of(RegistryKeys.RECIPE, id)).orElse(null);
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
        return List.of();
    }
}
