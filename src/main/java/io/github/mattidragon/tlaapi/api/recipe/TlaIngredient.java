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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;

import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;

/**
 * Represents a list of values that can be held by a single slot in a recipe.
 */
public record TlaIngredient (Collection<? extends TlaStack> stacks) {
    /**
     * An empty ingredient, containing no stacks.
     */
    public static final TlaIngredient EMPTY = new TlaIngredient(List.of());

    public static final Codec<TlaIngredient> CODEC = TlaStack.CODEC.codec().listOf().xmap(TlaIngredient::new, i -> List.copyOf(i.stacks));
    public static final PacketCodec<RegistryByteBuf, TlaIngredient> PACKET_CODEC = TlaStack.PACKET_CODEC.collect(PacketCodecs.toList()).xmap(TlaIngredient::new, i -> List.copyOf(i.stacks));

    public TlaIngredient {
        stacks = List.copyOf(stacks);
    }

    /**
     * Creates a new ingredient from the provided stacks.
     */
    public static TlaIngredient ofStacks(Collection<? extends TlaStack> stacks) {
        return stacks.isEmpty() ? EMPTY : new TlaIngredient(stacks);
    }

    /**
     * Creates a new ingredient from the provided stacks.
     */
    public static TlaIngredient ofStacks(TlaStack... stacks) {
        return new TlaIngredient(List.of(stacks));
    }

    /**
     * Creates a new ingredient from the provided vanilla ingredient.
     */
    @SuppressWarnings("deprecation")
    public static TlaIngredient ofIngredient(Ingredient ingredient) {
        return ingredient.isEmpty() ? EMPTY : ofStacks(ingredient.getMatchingItems().<TlaStack>map(TlaStack::of).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided vanilla ingredient.
     */
    public static TlaIngredient ofSlotDisplay(SlotDisplay display) {
        ContextParameterMap parameters = SlotDisplayContexts.createParameters(Objects.requireNonNull(PluginLoader.getServer()).getOverworld());
        if (display instanceof EmptySlotDisplay) {
            return EMPTY;
        }
        return ofStacks(display.appendStacks(parameters, TlaStack.DISPLAYED_ITEM_FACTORY).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided items.
     */
    public static TlaIngredient ofItems(Collection<? extends ItemConvertible> items) {
        return items.isEmpty() ? EMPTY : ofStacks(items.stream().map(TlaStack::of).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided item tag.
     * Note that any tags used here or in recipes should be translated so that they appear nicely in EMI.
     */
    public static TlaIngredient ofItemTag(TagKey<Item> tag) {
        return ofItems(Registries.ITEM.getOptional(tag)
                .map(RegistryEntryList.Named::stream)
                .orElse(Stream.empty())
                .map(RegistryEntry::value)
                .toList());
    }

    /**
     * Creates a new ingredient from the provided fluids.
     */
    public static TlaIngredient ofFluids(Collection<? extends Fluid> fluids) {
        return fluids.isEmpty() ? EMPTY : ofStacks(fluids.stream().map(TlaStack::bucketOf).filter(i -> !i.isEmpty()).toList());
    }

    /**
     * Creates a new ingredient from the provided fluid tag.
     * Note that any tags used here or in recipes should be translated so that they appear nicely in EMI.
     */
    public static TlaIngredient ofFluidTag(TagKey<Fluid> tag) {
        return ofFluids(Registries.FLUID.getOptional(tag)
                .map(RegistryEntryList.Named::stream)
                .orElse(Stream.empty())
                .map(RegistryEntry::value)
                .toList());
    }

    /**
     * Combines multiple ingredients into a single ingredient.
     * Does not play well with EMIs tag names so should be avoided if possible.
     */
    public static TlaIngredient join(TlaIngredient... ingredients) {
        return join(List.of(ingredients));
    }

    /**
     * Combines multiple ingredients into a single ingredient.
     * Does not play well with EMIs tag names so should be avoided if possible.
     */
    public static TlaIngredient join(Collection<? extends TlaIngredient> ingredients) {
        return new TlaIngredient(ingredients.stream().flatMap(ingredient -> ingredient.stacks().stream()).toList());
    }

    /**
     * Create a new ingredient where every stack has the provided count.
     * Note that this applies equally to fluids and items.
     */
    public TlaIngredient withAmount(long amount) {
        return new TlaIngredient(stacks.stream().map(stack -> stack.withAmount(amount)).toList());
    }

    /**
     * Create a new ingredient where every stack has the provided chance.
     */
    public TlaIngredient withChance(double chance) {
        return new TlaIngredient(stacks.stream().map(stack -> stack.withChance(chance)).toList());
    }
}
