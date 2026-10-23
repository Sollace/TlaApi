package io.github.mattidragon.tlaapi.impl.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

public class Collections {
    public static <T, A extends V, V> V reduce(Collection<? extends T> collection,
            V unit,
            Function<? super T, A> unary,
            Function<Stream<A>, V> composite
    ) {
        return switch (collection.size()) {
            case 0 -> unit;
            case 1 -> unary.apply(collection.iterator().next());
            default -> composite.apply(collection.stream().map(unary));
        };
    }

    public static <T, A extends V, V> V reduce(T[] collection,
            V unit,
            Function<? super T, A> unary,
            Function<Stream<A>, V> composite
    ) {
        return switch (collection.length) {
            case 0 -> unit;
            case 1 -> unary.apply(collection[0]);
            default -> composite.apply(Arrays.stream(collection).map(unary));
        };
    }
}
