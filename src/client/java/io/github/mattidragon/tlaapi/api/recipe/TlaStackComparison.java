package io.github.mattidragon.tlaapi.api.recipe;

import java.util.Objects;
import java.util.function.Function;

/**
 * A combined equality and hashing function for TlaStacks.
 * <p>
 * @see PluginContext#setDefaultComparison
 */
public record TlaStackComparison(Predicate predicate, HashFunction hashFunction) {
    private static final TlaStackComparison COMPARE_COMPONENTS = compareData(TlaStack::getComponents);
    public static final TlaStackComparison DEFAULT_COMPARISON = of((a, b) -> true, a -> 0);

    public static TlaStackComparison of() {
        return DEFAULT_COMPARISON;
    }

    public static TlaStackComparison compareComponents() {
        return COMPARE_COMPONENTS;
    }


    public static TlaStackComparison of(Predicate comparator, HashFunction hashFunction) {
        return new TlaStackComparison(comparator, hashFunction);
    }

    public static <T> TlaStackComparison compareData(Function<TlaStack, T> function) {
        return of((a, b) -> Objects.equals(function.apply(a), function.apply(b)), stack -> Objects.hashCode(function.apply(stack)));
    }

    public interface Predicate {
        boolean compare(TlaStack a, TlaStack b);
    }

    public interface HashFunction {
        int hash(TlaStack stack);
    }
}
