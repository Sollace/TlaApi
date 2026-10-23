package io.github.mattidragon.tlaapi.api.recipe;

import net.minecraft.component.ComponentChanges;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.display.DisplayedItemFactory;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplay.EmptySlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.dynamic.Codecs;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.api.plugin.ModdingPlatform;
import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.impl.MoreCodecs;
import io.github.mattidragon.tlaapi.impl.Untyped;

/**
 * Represents a stack of items or fluids for use with recipe viewers.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public sealed abstract class TlaStack permits TlaStack.TlaFluidStack, TlaStack.TlaItemStack, TlaStack.TlaEmptyStack {
    static <X, T extends TlaStack> MapCodec<T> createCodec(MapCodec<RegistryEntry<X>> variantCodec, Function4<RegistryEntry<X>, ComponentChanges, Long, OptionalDouble, T> constructor) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                variantCodec.forGetter(o -> Untyped.cast(o.variant)),
                ComponentChanges.CODEC.optionalFieldOf("components", ComponentChanges.EMPTY).forGetter(TlaStack::getComponents),
                Codec.LONG.fieldOf("amount").forGetter(TlaStack::getAmount),
                MoreCodecs.OPTIONAL_DOUBLE_CODEC.optionalFieldOf("chance", OptionalDouble.empty()).forGetter(TlaStack::getChance)
        ).apply(i, constructor));
    }

    static <X, T extends TlaStack> PacketCodec<RegistryByteBuf, T> createPacketCodec(PacketCodec<RegistryByteBuf, RegistryEntry<X>> variantCodec, Function4<RegistryEntry<X>, ComponentChanges, Long, OptionalDouble, T> constructor) {
        return PacketCodec.tuple(
                variantCodec, o -> Untyped.cast(o.variant),
                ComponentChanges.PACKET_CODEC, TlaStack::getComponents,
                PacketCodecs.LONG, TlaStack::getAmount,
                MoreCodecs.OPTIONAL_DOUBLE_PACKET_CODEC, TlaStack::getChance,
                constructor
        );
    }

    private static final Supplier<BiMap<Identifier, Type>> REGISTRY = Untyped.memoize(() -> HashBiMap.create(Map.of(
            Identifier.ofVanilla("empty"), TlaEmptyStack.TYPE.get(),
            Identifier.ofVanilla("fluid"), TlaFluidStack.TYPE,
            Identifier.ofVanilla("item"), TlaItemStack.TYPE
    )));
    public static final Codec<TlaStack> CODEC = Codecs.idChecked(Identifier.CODEC,
                id -> REGISTRY.get().get(id),
                type -> REGISTRY.get().inverse().get(type)
            ).dispatch("type", TlaStack::type, Type::codec);
    public static final PacketCodec<RegistryByteBuf, TlaStack> PACKET_CODEC = Identifier.PACKET_CODEC.xmap(
                id -> REGISTRY.get().get(id),
                type -> REGISTRY.get().inverse().get(type)
            ).<RegistryByteBuf>cast().dispatch(TlaStack::type, Type::packetCodec);

    protected final long amount;
    protected final OptionalDouble chance;
    protected final RegistryEntry<?> variant;

    protected final ComponentChanges components;

    protected final Type type;

    record Type(MapCodec<? extends TlaStack> codec, PacketCodec<RegistryByteBuf, ? extends TlaStack> packetCodec) {}

    static final DisplayedItemFactory.FromStack<TlaStack> DISPLAYED_ITEM_FACTORY = TlaStack::of;

    protected TlaStack(Type type, long amount, OptionalDouble chance, RegistryEntry<?> variant, ComponentChanges components) {
        this.type = type;
        this.amount = amount;
        this.chance = chance;
        this.variant = variant;
        this.components = components;
    }

    protected Type type() {
        return type;
    }

    /**
     * Creates a stack of 0 air items.
     */
    public static TlaStack empty() {
        return TlaEmptyStack.INSTANCE;
    }

    public static TlaStack of(Fluid fluid, long amount) {
        return amount == 0 ? empty() : of(fluid, ComponentChanges.EMPTY, amount);
    }

    @SuppressWarnings("deprecation")
    public static TlaStack of(Fluid fluid, ComponentChanges components, long amount) {
        return amount == 0 ? empty() : new TlaFluidStack(fluid.getRegistryEntry(), components, amount, OptionalDouble.empty());
    }

    public static TlaStack bucketOf(Fluid fluid) {
        return bucketOf(fluid, ComponentChanges.EMPTY);
    }

    public static TlaStack bucketOf(Fluid fluid, ComponentChanges components) {
        return fluid == Fluids.EMPTY ? empty() : of(fluid, components, ModdingPlatform.getInstance().getBucketAmount());
    }

    public static TlaStack of(ItemConvertible item, long amount) {
        return of(item, ComponentChanges.EMPTY, amount);
    }

    @SuppressWarnings("deprecation")
    public static TlaStack of(ItemConvertible item, ComponentChanges components, long amount) {
        return item.asItem() == Items.AIR || amount <= 0 ? empty() : new TlaItemStack(item.asItem().getRegistryEntry(), components, amount, OptionalDouble.empty());
    }

    public static TlaStack of(ItemConvertible item) {
        return item.asItem() == Items.AIR ? empty() : of(item, 1);
    }

    @SuppressWarnings("deprecation")
    public static TlaStack of(RegistryEntry<Item> item) {
        return item.matchesId(Items.AIR.getRegistryEntry().registryKey().getValue()) ? empty() : of(item.value(), 1);
    }

    public static TlaStack of(SlotDisplay display) {
        if (display instanceof EmptySlotDisplay) {
            return empty();
        }
        ContextParameterMap parameters = SlotDisplayContexts.createParameters(Objects.requireNonNull(PluginLoader.getWorld()));
        return of(display.getFirst(parameters));
    }

    public static TlaStack of(ItemStack stack) {
        return stack.isEmpty() ? empty() : of(stack.getItem(), stack.getComponentChanges(), stack.getCount());
    }

    public long getAmount() {
        return amount;
    }

    public OptionalDouble getChance() {
        return chance;
    }

    public boolean isEmpty() {
        return amount == 0;
    }

    /**
     * Returns a copy of this stack with the provided amount.
     */
    public abstract TlaStack withAmount(long amount);

    /**
     * Returns a copy of this stack with the provided chance.
     * @implNote The chances for each stack in an ingredient are averaged for display in EMI.
     */
    public abstract TlaStack withChance(double chance);

    /**
     * Returns a copy of this stack without a chance.
     */
    public abstract TlaStack withoutChance();

    /**
     * Gets the registry entry of the backing object for this stack.
     */
    public RegistryEntry<?> getRegistryEntry() {
        return variant;
    }

    public abstract Either<Fluid, Item> getVariant();

    public TlaIngredient asIngredient() {
        return TlaIngredient.ofStacks(this);
    }

    public ComponentChanges getComponents() {
        return components;
    }

    public static final class TlaEmptyStack extends TlaStack {
        static final Supplier<Type> TYPE = Untyped.memoize(() -> new Type(MapCodec.unit(TlaEmptyStack.INSTANCE), PacketCodec.unit(TlaEmptyStack.INSTANCE)));
        static final TlaEmptyStack INSTANCE = new TlaEmptyStack();

        @SuppressWarnings("deprecation")
        private TlaEmptyStack() {
            super(TYPE.get(), 0, OptionalDouble.empty(), Items.AIR.getRegistryEntry(), ComponentChanges.EMPTY);
        }

        @Override
        public TlaStack withAmount(long amount) {
            return this;
        }

        @Override
        public TlaStack withChance(double chance) {
            return this;
        }

        @Override
        public TlaStack withoutChance() {
            return this;
        }

        @Override
        public ComponentChanges getComponents() {
            return ComponentChanges.EMPTY;
        }

        @SuppressWarnings("deprecation")
        @Override
        public RegistryEntry<?> getRegistryEntry() {
            return Items.AIR.getRegistryEntry();
        }

        @Override
        public Either<Fluid, Item> getVariant() {
            return Either.right(Items.AIR);
        }

        @Override
        public String toString() {
            return "TlaEmptyStack[]";
        }
    }

    public static final class TlaFluidStack extends TlaStack {
        static final Type TYPE = new Type(
                createCodec(Registries.FLUID.getEntryCodec().fieldOf("fluid"), TlaFluidStack::new),
                createPacketCodec(PacketCodecs.registryEntry(RegistryKeys.FLUID), TlaFluidStack::new)
        );

        private TlaFluidStack(RegistryEntry<Fluid> fluid, ComponentChanges components, long amount, OptionalDouble chance) {
            super(TYPE, amount, chance, fluid, components);
        }

        @Override
        public TlaFluidStack withAmount(long amount) {
            return new TlaFluidStack(getRegistryEntry(), components, amount, chance);
        }

        @Override
        public TlaFluidStack withChance(double chance) {
            return chance >= 1 ? withoutChance() : new TlaFluidStack(getRegistryEntry(), components, amount, OptionalDouble.of(chance));
        }

        @Override
        public TlaFluidStack withoutChance() {
            return chance.isEmpty() ? this : new TlaFluidStack(getRegistryEntry(), components, amount, OptionalDouble.empty());
        }

        @Override
        public RegistryEntry<Fluid> getRegistryEntry() {
            return Untyped.cast(variant);
        }

        public Fluid getFluid() {
            return getRegistryEntry().value();
        }

        @Override
        public Either<Fluid, Item> getVariant() {
            return Either.left(getFluid());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            return obj instanceof TlaFluidStack that
                    && Objects.equals(this.variant, that.variant)
                    && Objects.equals(this.components, that.components)
                    && this.amount == that.amount
                    && Objects.equals(this.chance, that.chance);
        }

        @Override
        public int hashCode() {
            return Objects.hash(variant, components, amount, chance);
        }

        @Override
        public String toString() {
            return "TlaFluidStack[" +
                   "fluid=" + variant + ", " +
                   "component=" + components + ", " +
                   "amount=" + amount + ", " +
                   "chance=" + chance + ']';
        }
    }

    public static final class TlaItemStack extends TlaStack {
        static final Type TYPE = new Type(
            createCodec(Registries.ITEM.getEntryCodec().fieldOf("item"), TlaItemStack::new),
            createPacketCodec(PacketCodecs.registryEntry(RegistryKeys.ITEM), TlaItemStack::new)
        );

        private TlaItemStack(RegistryEntry<Item> item, ComponentChanges components, long amount, OptionalDouble chance) {
            super(TYPE, amount, chance, item, components);
        }

        @Override
        public TlaItemStack withAmount(long amount) {
            return new TlaItemStack(getRegistryEntry(), components, amount, chance);
        }

        @Override
        public TlaItemStack withChance(double chance) {
            return chance >= 1 ? withoutChance() : new TlaItemStack(getRegistryEntry(), components, amount, OptionalDouble.of(chance));
        }

        @Override
        public TlaItemStack withoutChance() {
            return chance.isEmpty() ? this : new TlaItemStack(getRegistryEntry(), components, amount, OptionalDouble.empty());
        }

        public Item getItem() {
            return getRegistryEntry().value();
        }
        @Override
        public Either<Fluid, Item> getVariant() {
            return Either.right(getItem());
        }

        @Override
        public RegistryEntry<Item> getRegistryEntry() {
            return Untyped.cast(variant);
        }

        public ItemStack toStack() {
            return new ItemStack(getRegistryEntry(), (int) amount);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            return obj instanceof TlaItemStack that
                   && Objects.equals(this.variant, that.variant)
                   && Objects.equals(this.components, that.components)
                   && this.amount == that.amount
                   && Objects.equals(this.chance, that.chance);
        }

        @Override
        public int hashCode() {
            return Objects.hash(variant, components, amount, chance);
        }

        @Override
        public String toString() {
            return "TlaItemStack[" +
                   "item=" + variant + ", " +
                   "component=" + components + ", " +
                   "amount=" + amount + ", " +
                   "chance=" + chance + ']';
        }
    }
}
