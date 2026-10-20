package io.github.mattidragon.tlaapi.impl;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class Untyped {
    @SuppressWarnings("unchecked")
    public static <A, B> B cast(A a) {
        return (B)a;
    }

    public static <A> Supplier<A> unit(A a) {
        return () -> a;
    }

    public static <A> Supplier<A> memoize(Supplier<A> supplier) {
        return switch (supplier) {
            case Memoize<A> memoize -> memoize;
            default -> new Memoize<>(new AtomicReference<>(null), supplier);
        };
    }

    private record Memoize<A>(AtomicReference<Optional<A>> ref, Supplier<A> supplier) implements Supplier<A> {
        @Override
        public A get() {
            return ref.updateAndGet(a -> {
                return a == null ? Optional.ofNullable(supplier.get()) : a;
            }).orElse(null);
        }
    }
}
