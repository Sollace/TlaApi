package io.github.mattidragon.tlaapi.api.recipe;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplay.EmptySlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.context.ContextParameterMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;

import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.impl.MoreCodecs;
import io.github.mattidragon.tlaapi.impl.util.Collections;

/**
 * Represents a list of values that can be held by a single slot in a recipe.
 */
public sealed interface TlaIngredient permits TlaIngredient.Many, TlaStack {
    /**
     * An empty ingredient, containing no stacks.
     */
    TlaIngredient EMPTY = new Many(List.of());

    Codec<TlaIngredient> CODEC = Codec.lazyInitialized(() -> TlaStack.CODEC.listOf().xmap(TlaIngredient::ofStacks, i -> List.copyOf(i.stacks())));
    PacketCodec<RegistryByteBuf, TlaIngredient> PACKET_CODEC = MoreCodecs.lazy(() -> TlaStack.PACKET_CODEC.collect(PacketCodecs.toList()).xmap(TlaIngredient::ofStacks, i -> List.copyOf(i.stacks())));

    Collection<? extends TlaStack> stacks();

    default Optional<TagKey<?>> tag() {
        return Optional.empty();
    }

    /**
     * Creates a new ingredient from the provided stacks.
     */
    static TlaIngredient ofStacks(Stream<? extends TlaStack> stacks) {
        return Collections.reduce(stacks.filter(i -> !i.isEmpty()).toList(), EMPTY, Function.identity(), s -> new Many(s.toList()));
    }

    /**
     * Creates a new ingredient from the provided stacks.
     */
    static TlaIngredient ofStacks(Collection<? extends TlaStack> stacks) {
        return ofStacks(stacks.stream());
    }

    /**
     * Creates a new ingredient from the provided stacks.
     */
    static TlaIngredient ofStacks(TlaStack... stacks) {
        return ofStacks(Arrays.stream(stacks));
    }

    /**
     * Creates a new ingredient from the provided vanilla ingredient.
     */
    @SuppressWarnings("deprecation")
    static TlaIngredient ofIngredient(Ingredient ingredient) {
        return ingredient.isEmpty() ? EMPTY : ofStacks(ingredient.getMatchingItems().map(TlaStack::of).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided vanilla ingredient.
     */
    static TlaIngredient ofSlotDisplay(SlotDisplay display) {
        if (display instanceof EmptySlotDisplay) {
            return EMPTY;
        }
        ContextParameterMap parameters = SlotDisplayContexts.createParameters(Objects.requireNonNull(PluginLoader.getWorld()));
        return ofStacks(display.appendStacks(parameters, TlaStack.DISPLAYED_ITEM_FACTORY).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided items.
     */
    static TlaIngredient ofItems(Collection<? extends ItemConvertible> items) {
        return ofItems(items.stream());
    }

    /**
     * Creates a new ingredient from the provided items.
     */
    static TlaIngredient ofItems(Stream<? extends ItemConvertible> items) {
        return ofStacks(items.map(TlaStack::of));
    }

    /**
     * Creates a new ingredient from the provided item tag.
     * Note that any tags used here or in recipes should be translated so that they appear nicely in EMI.
     */
    static TlaIngredient ofItemTag(TagKey<Item> tag) {
        return ofStacks(Registries.ITEM.getOptional(tag)
                .map(RegistryEntryList.Named::stream)
                .orElse(Stream.empty())
                .map(RegistryEntry::value)
                .map(TlaStack::of));
    }

    /**
     * Creates a new ingredient from the provided fluids.
     */
    static TlaIngredient ofFluids(Collection<? extends Fluid> fluids) {
        return ofFluids(fluids.stream());
    }

    /**
     * Creates a new ingredient from the provided fluids.
     */
    static TlaIngredient ofFluids(Stream<? extends Fluid> fluids) {
        return ofStacks(fluids.map(TlaStack::bucketOf));
    }

    /**
     * Creates a new ingredient from the provided fluid tag.
     * Note that any tags used here or in recipes should be translated so that they appear nicely in EMI.
     */
    static TlaIngredient ofFluidTag(TagKey<Fluid> tag) {
        return ofStacks(Registries.FLUID.getOptional(tag)
                .map(RegistryEntryList.Named::stream)
                .orElse(Stream.empty())
                .map(RegistryEntry::value)
                .map(TlaStack::bucketOf));
    }

    /**
     * Combines multiple ingredients into a single ingredient.
     * Does not play well with EMIs tag names so should be avoided if possible.
     */
    static TlaIngredient join(TlaIngredient... ingredients) {
        return join(Stream.of(ingredients));
    }

    /**
     * Combines multiple ingredients into a single ingredient.
     * Does not play well with EMIs tag names so should be avoided if possible.
     */
    static TlaIngredient join(Collection<? extends TlaIngredient> ingredients) {
        return join(ingredients.stream());
    }

    /**
     * Combines multiple ingredients into a single ingredient.
     * Does not play well with EMIs tag names so should be avoided if possible.
     */
    static TlaIngredient join(Stream<? extends TlaIngredient> ingredients) {
        return ofStacks(ingredients.flatMap(ingredient -> ingredient.stacks().stream()));
    }

    /**
     * Create a new ingredient where every stack has the provided count.
     * Note that this applies equally to fluids and items.
     */
    default TlaIngredient withAmount(long amount) {
        return ofStacks(stacks().stream().map(stack -> stack.withAmount(amount)));
    }

    /**
     * Create a new ingredient where every stack has the provided chance.
     */
    default TlaIngredient withChance(double chance) {
        return ofStacks(stacks().stream().map(stack -> stack.withChance(chance)));
    }

    public record Many(Collection<? extends TlaStack> stacks) implements TlaIngredient {
        public Many {
            stacks = List.copyOf(stacks);
        }
    }
}
