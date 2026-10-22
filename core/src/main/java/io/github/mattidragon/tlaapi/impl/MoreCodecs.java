package io.github.mattidragon.tlaapi.impl;

import java.util.OptionalDouble;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public final class MoreCodecs {
    public static final Codec<OptionalDouble> OPTIONAL_DOUBLE_CODEC = Codec.DOUBLE.xmap(
            d -> d == null ? OptionalDouble.empty() : OptionalDouble.of(d),
            d -> d.isPresent() ? d.getAsDouble() : null
    );

    public static final PacketCodec<ByteBuf, OptionalDouble> OPTIONAL_DOUBLE_PACKET_CODEC = PacketCodecs.optional(PacketCodecs.DOUBLE).xmap(
            d -> d.stream().mapToDouble(Double::doubleValue).findFirst(),
            d -> d.stream().mapToObj(Double::valueOf).findFirst()
    );

    public static <T> Codec<Supplier<T>> supplierOf(Codec<T> elementCodec) {
        return elementCodec.xmap(Untyped::unit, Supplier::get);
    }

    public static <B, T> PacketCodec<B, Supplier<T>> supplierOf(PacketCodec<B, T> elementCodec) {
        return elementCodec.xmap(Untyped::unit, Supplier::get);
    }
}
