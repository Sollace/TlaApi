package io.github.mattidragon.tlaapi.api.recipe;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.github.mattidragon.tlaapi.impl.Untyped;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;

/**
 * Serializer for reading and writing a TlaRecipe to and from packets
 *
 * @param <T> The type of recipe to serialize
 */
public record TlaRecipeSerializer<T extends TlaRecipe>(
        Identifier id,
        MapCodec<T> mapCodec,
        PacketCodec<RegistryByteBuf, T> packetCodec
) {
    private static final Map<Identifier, TlaRecipeSerializer<?>> REGISTRY = new HashMap<>();
    public static final Codec<TlaRecipeSerializer<?>> CODEC = Identifier.CODEC.xmap(REGISTRY::get, TlaRecipeSerializer::id);
    public static final PacketCodec<RegistryByteBuf, TlaRecipeSerializer<?>> PACKET_CODEC = Identifier.PACKET_CODEC.<RegistryByteBuf>cast().xmap(REGISTRY::get, TlaRecipeSerializer::id);

    public static <T extends TlaRecipe> TlaRecipeSerializer<T> register(Identifier id, MapCodec<T> mapCodec, PacketCodec<RegistryByteBuf, T> packetCodec) {
        return Untyped.cast(REGISTRY.compute(id, (_id, existing) -> new TlaRecipeSerializer<>(id, mapCodec, packetCodec)));
    }
}
