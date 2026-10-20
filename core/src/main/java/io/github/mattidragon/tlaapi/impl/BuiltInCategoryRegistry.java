package io.github.mattidragon.tlaapi.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import io.github.mattidragon.tlaapi.api.BuiltInRecipeCategory;

public abstract class BuiltInCategoryRegistry<T> {
    private static final BuiltInRecipeCategory[] KEYS = BuiltInRecipeCategory.values();

    // We use a memoize so the values are only computed once needed
    private final Supplier<BiMap<BuiltInRecipeCategory, T>> entries = Untyped.memoize(() -> ImmutableBiMap.ofEntries(Untyped.cast(Arrays.stream(KEYS)
            .map(this::getDistinctKey)
            .distinct()
            .map(category -> {
                var t = getValue(category);
                return t == null ? null : Map.entry(category, t);
            })
            .filter(Objects::nonNull)
            .toArray(Map.Entry[]::new))));

    public @Nullable T get(BuiltInRecipeCategory category) {
        return entries.get().get(getDistinctKey(category));
    }

    public @Nullable BuiltInRecipeCategory get(T t) {
        return entries.get().inverse().get(t);
    }

    public Optional<T> getOptional(BuiltInRecipeCategory category) {
        return Optional.ofNullable(get(category));
    }

    public Optional<BuiltInRecipeCategory> getOptional(T t) {
        return Optional.ofNullable(get(t));
    }

    protected abstract @Nullable T getValue(BuiltInRecipeCategory category);

    protected BuiltInRecipeCategory getDistinctKey(BuiltInRecipeCategory category) {
        return category;
    }
}
