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

    public static <A> Resettable<A> memoize(Supplier<A> supplier) {
        return switch (supplier) {
            case Memoize<A> memoize -> memoize;
            default -> new Memoize<>(new AtomicReference<>(null), supplier);
        };
    }

    private record Memoize<A>(AtomicReference<Optional<A>> ref, Supplier<A> supplier) implements Resettable<A> {
        @Override
        public A get() {
            return ref.updateAndGet(a -> a == null ? Optional.ofNullable(supplier.get()) : a).orElse(null);
        }

        @Override
        public void reset() {
            ref.set(null);
        }
    }

    public interface Resettable<A> extends Supplier<A> {
        void reset();
    }
}
