package io.github.mattidragon.tlaapi.api.recipe;

import io.github.mattidragon.tlaapi.impl.PluginsExtend;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import java.util.List;

/**
 * Represents a single entry in a category.
 * This usually a represents a recipe in minecraft's recipe system,
 * but a recipe can represent anything.
 */
@PluginsExtend
public interface TlaRecipe {
    Codec<TlaRecipe> CODEC = TlaRecipeSerializer.CODEC.dispatch(TlaRecipe::getSerializer, TlaRecipeSerializer::mapCodec);
    PacketCodec<RegistryByteBuf, TlaRecipe> PACKET_CODEC = TlaRecipeSerializer.PACKET_CODEC.dispatch(TlaRecipe::getSerializer, TlaRecipeSerializer::packetCodec);

    /**
     * The serializer used for serializing this recipe over the network.
     */
    @NotNull
    TlaRecipeSerializer<?> getSerializer();

    /**
     * Provides the category this recipe belongs to.
     */
    TlaCategory getCategory();

    /**
     * Provides the identifier of this recipe.
     * This may return {@code null} if the recipe doesn't have an identifier and one can't be generated,
     * but that will result in certain features not working properly.
     * For recipes that aren't vanilla recipes it's recommended to provide an identifier with a single slash at the start of the path to distinguish it.
     */
    @Nullable
    default RegistryKey<Recipe<?>> getId() {
        return null;
    }

    default StableRecipeIdentifier getStableId() {
        return StableRecipeIdentifier.of(this);
    }

    /**
     * Returns all the input ingredients of this recipe.
     */
    default List<TlaIngredient> getInputs() {
        return List.of();
    }

    /**
     * Returns all the output stacks of this recipe.
     */
    default List<TlaStack> getOutputs() {
        return List.of();
    }

    /**
     * Returns all the catalysts of this recipe.
     * Catalysts are ingredients that aren't consumed by the recipe but still need to be present.
     * Usually corresponds to the catalyst slot in the recipe gui.
     * @implNote Only used by the EMI implementation.
     */
    default List<TlaIngredient> getCatalysts() {
        return List.of();
    }
}
