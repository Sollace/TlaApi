package io.github.mattidragon.tlaapi.impl;

import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.spongepowered.include.com.google.common.base.Preconditions;

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

    public static <B, V> PacketCodec<B, V> unit(Supplier<V> value) {
        return new PacketCodec<>() {
            @Override
            public V decode(B b) {
                return value.get();
            }

            @Override
            public void encode(B b, V v) {
                Preconditions.checkState(v.equals(value.get()), "Can't encode '" + v + "', expected '" + value.get() + "'");
            }
        };
    }

    public static <B, V> PacketCodec<B, V> lazy(Supplier<PacketCodec<B, V>> codecSupplier) {
        return new PacketCodec<>() {
            final Supplier<PacketCodec<B, V>> codec = Untyped.memoize(codecSupplier);
            @Override
            public V decode(B buffer) {
                return codec.get().decode(buffer);
            }

            @Override
            public void encode(B buffer, V value) {
                codec.get().encode(buffer, value);
            }
        };
    }
}
