package io.github.mattidragon.tlaapi.impl;

import java.util.OptionalDouble;
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

}
