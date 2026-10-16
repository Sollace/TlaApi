package io.github.mattidragon.tlaapi.api.plugin;

import io.github.mattidragon.tlaapi.api.recipe.TlaStackComparison;
import io.github.mattidragon.tlaapi.impl.ImplementationsExtend;

@ImplementationsExtend
public interface Comparisons<T> extends RecipeViewerOwned {
    static Comparisons<?> noop(RecipeViewer viewer) {
        return new Comparisons<>() {
            @Override
            public RecipeViewer getRecipeViewer() {
                return viewer;
            }

            @Override
            public void register(Object key, TlaStackComparison comparison) {

            }
        };
    }

    /**
     * Adds a default comparison method for a stack for its key.
     * @param item The key for the object to compare.
     * @param comparison The comparison and hash function
     */
    void register(T key, TlaStackComparison comparison);

    /**
     * Adds a default comparison method for a stack for its key.
     * @param keys The keys for the object to compare.
     * @param comparison The comparison and hash function
     */
    default void register(TlaStackComparison comparison, @SuppressWarnings("unchecked") T... keys) {
        for (T key : keys) {
            register(key, comparison);
        }
    }

    /**
     * Adds a default comparison method for a stack for its key.
     * @param keys The keys for the object to compare.
     * @param comparison The comparison and hash function
     */
    default void register(TlaStackComparison comparison, Iterable<T> keys) {
        for (T key : keys) {
            register(key, comparison);
        }
    }
}
