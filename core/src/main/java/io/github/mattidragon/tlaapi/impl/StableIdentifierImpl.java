package io.github.mattidragon.tlaapi.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.datafixers.util.Pair;

import io.github.mattidragon.tlaapi.api.recipe.StableRecipeIdentifier;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public record StableIdentifierImpl(
        @Nullable RegistryKey<Recipe<?>> key,
        TlaCategory category,
        Collection<? extends TlaStack> ingredients,
        Collection<? extends TlaStack> results
    ) implements StableRecipeIdentifier {
    private static final Identifier NONE = Identifier.ofVanilla("none");
    private static final Interner<StableIdentifierImpl> INTERNER = Interners.newWeakInterner();
    private static final Map<StableIdentifierImpl, Identifier> IDS = new WeakHashMap<>();

    @Override
    public Pair<Optional<RegistryKey<Recipe<?>>>, Supplier<Optional<Identifier>>> getKey() {
        return new Pair<>(Optional.ofNullable(key), this::lookupId);
    }

    private Optional<Identifier> lookupId() {
        return Optional.of(IDS.computeIfAbsent(this, self -> {
            return self.category().getId().withPath(p -> {
                var outId = getOutId(self.results());
                var inId = getOutId(self.ingredients());
                return String.join("/", p, inId.getNamespace(), inId.getPath(), outId.getNamespace(), outId.getPath());
            });
        }));
    }

    public static StableRecipeIdentifier get(@NotNull RegistryKey<Recipe<?>> key) {
        Objects.requireNonNull(key, "Registry key is required");
        return () -> new Pair<>(Optional.of(key), Optional::empty);
    }

    public static StableRecipeIdentifier get(@Nullable RegistryKey<Recipe<?>> key, TlaCategory category, Collection<TlaIngredient> ingredients, Collection<TlaStack> results) {
        return INTERNER.intern(new StableIdentifierImpl(
                key,
                category,
                ingredients.stream().flatMap(i -> i.stacks().stream()).distinct().sorted().toList(),
                results.stream().distinct().sorted().toList()
        ));
    }

    private static Identifier getOutId(Collection<? extends TlaStack> results) {
        return switch(results.size()) {
            case 0 -> NONE;
            case 1 -> results.iterator().next().getRegistryEntry().getKey().map(StableIdentifierImpl::getId).orElse(NONE);
            default -> TlaApi.id(results.size() + "/" + results.hashCode());
        };
    }

    private static Identifier getId(RegistryKey<?> key) {
        return key.getRegistry().withSuffixedPath(key.getValue().getNamespace() + "/" + key.getValue().getNamespace());
    }

    public static boolean matches(StableRecipeIdentifier a, StableRecipeIdentifier b) {
        var keyA = a.getKey();
        var keyB = b.getKey();
        return matchKeyPart(keyA.getFirst(), keyB.getFirst()) || matchKeyPart(keyA.getSecond().get(), keyB.getSecond().get());
    }

    public static boolean matches(StableRecipeIdentifier a, Identifier id) {
        var keyA = a.getKey();
        return keyA.getFirst().filter(key -> key.getValue().equals(id)).isPresent()
            || keyA.getSecond().get().filter(id::equals).isPresent();
    }

    private static boolean matchKeyPart(Optional<?> a, Optional<?> b) {
        return a.isPresent() && a.equals(b);
    }
}
