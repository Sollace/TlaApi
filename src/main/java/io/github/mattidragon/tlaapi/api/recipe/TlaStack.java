package io.github.mattidragon.tlaapi.api.recipe;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.component.ComponentChanges;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.display.DisplayedItemFactory;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameterMap;
import java.util.Objects;
import java.util.OptionalDouble;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.mattidragon.tlaapi.api.plugin.PluginLoader;
import io.github.mattidragon.tlaapi.impl.MoreCodecs;

/**
 * Represents a stack of items or fluids for use with recipe viewers.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public sealed abstract class TlaStack permits TlaStack.TlaFluidStack, TlaStack.TlaItemStack {
    private static final Codec<Either<FluidVariant, ItemVariant>> VARIANT_CODEC = Codec.either(FluidVariant.CODEC, ItemVariant.CODEC);
    private static final PacketCodec<RegistryByteBuf, Either<FluidVariant, ItemVariant>> VARIANT_PACKET_CODEC = PacketCodecs.either(FluidVariant.PACKET_CODEC, ItemVariant.PACKET_CODEC);

    public static final MapCodec<TlaStack> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            VARIANT_CODEC.fieldOf("item").forGetter(TlaStack::getVariant),
            Codec.LONG.fieldOf("amount").forGetter(o -> o.amount),
            MoreCodecs.OPTIONAL_DOUBLE_CODEC.optionalFieldOf("chance", OptionalDouble.empty()).forGetter(o -> o.chance)
    ).apply(i, TlaStack::of));
    public static final PacketCodec<RegistryByteBuf, TlaStack> PACKET_CODEC = PacketCodec.tuple(
            VARIANT_PACKET_CODEC, TlaStack::getVariant,
            PacketCodecs.LONG, o -> o.amount,
            MoreCodecs.OPTIONAL_DOUBLE_PACKET_CODEC, o -> o.chance,
            TlaStack::of
    );

    protected final long amount;
    protected final OptionalDouble chance;

    static final DisplayedItemFactory.FromStack<TlaStack> DISPLAYED_ITEM_FACTORY = TlaStack::of;

    protected TlaStack(long amount, OptionalDouble chance) {
        this.amount = amount;
        this.chance = chance;
    }

    private static TlaStack of(Either<FluidVariant, ItemVariant> variant, long amount, OptionalDouble chance) {
        return Either.unwrap(variant.mapBoth(fluid -> new TlaFluidStack(fluid, amount, chance), item -> new TlaItemStack(item, amount, chance)));
    }

    /**
     * Creates a stack of 0 air items.
     */
    public static TlaStack empty() {
        return of(Items.AIR, 0);
    }

    public static TlaFluidStack of(FluidVariant fluid, long amount) {
        return new TlaFluidStack(fluid, amount, OptionalDouble.empty());
    }

    public static TlaFluidStack of(Fluid fluid, long amount) {
        return of(FluidVariant.of(fluid), amount);
    }

    public static TlaFluidStack bucketOf(FluidVariant fluid) {
        return of(fluid, FluidConstants.BUCKET);
    }

    public static TlaFluidStack bucketOf(Fluid fluid) {
        return bucketOf(FluidVariant.of(fluid));
    }

    public static TlaItemStack of(ItemVariant item, long amount) {
        return new TlaItemStack(item, amount, OptionalDouble.empty());
    }

    public static TlaItemStack of(ItemVariant item) {
        return of(item, 1);
    }

    public static TlaItemStack of(ItemConvertible item, long amount) {
        return of(ItemVariant.of(item), amount);
    }

    public static TlaItemStack of(ItemConvertible item) {
        return of(item, 1);
    }

    public static TlaItemStack of(RegistryEntry<Item> item) {
        return of(item.value(), 1);
    }

    public static TlaItemStack of(SlotDisplay display) {
        ContextParameterMap parameters = SlotDisplayContexts.createParameters(Objects.requireNonNull(PluginLoader.getServer()).getOverworld());
        return of(display.getFirst(parameters));
    }

    public static TlaItemStack of(ItemStack stack) {
        return of(ItemVariant.of(stack), stack.getCount());
    }

    public long getAmount() {
        return amount;
    }

    public OptionalDouble getChance() {
        return chance;
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

    public abstract Either<FluidVariant, ItemVariant> getVariant();

    public TlaIngredient asIngredient() {
        return TlaIngredient.ofStacks(this);
    }

    public abstract ComponentChanges getComponents();

    public static final class TlaFluidStack extends TlaStack {
        private final FluidVariant fluid;

        private TlaFluidStack(FluidVariant fluid, long amount, OptionalDouble chance) {
            super(amount, chance);
            this.fluid = fluid;
        }

        @Override
        public TlaFluidStack withAmount(long amount) {
            return new TlaFluidStack(fluid, amount, chance);
        }

        @Override
        public TlaFluidStack withChance(double chance) {
            return new TlaFluidStack(fluid, amount, OptionalDouble.of(chance));
        }

        @Override
        public TlaFluidStack withoutChance() {
            return new TlaFluidStack(fluid, amount, OptionalDouble.empty());
        }

        public FluidVariant getFluidVariant() {
            return fluid;
        }

        public Fluid getFluid() {
            return fluid.getFluid();
        }

        @Override
        public Either<FluidVariant, ItemVariant> getVariant() {
            return Either.left(fluid);
        }

        @Override
        public ComponentChanges getComponents() {
            return fluid.getComponents();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TlaFluidStack) obj;
            return Objects.equals(this.fluid, that.fluid) &&
                   this.amount == that.amount &&
                   Objects.equals(this.chance, that.chance);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fluid, amount, chance);
        }

        @Override
        public String toString() {
            return "TlaFluidStack[" +
                   "fluid=" + fluid + ", " +
                   "amount=" + amount + ", " +
                   "chance=" + chance + ']';
        }
    }

    public static final class TlaItemStack extends TlaStack {
        private final ItemVariant item;

        private TlaItemStack(ItemVariant item, long amount, OptionalDouble chance) {
            super(amount, chance);
            this.item = item;
        }

        @Override
        public TlaItemStack withAmount(long amount) {
            return new TlaItemStack(item, amount, chance);
        }

        @Override
        public TlaItemStack withChance(double chance) {
            return new TlaItemStack(item, amount, OptionalDouble.of(chance));
        }

        @Override
        public TlaItemStack withoutChance() {
            return new TlaItemStack(item, amount, OptionalDouble.empty());
        }

        public Item getItem() {
            return item.getItem();
        }

        public ItemVariant getItemVariant() {
            return item;
        }

        public ItemStack toStack() {
            return item.toStack((int) amount);
        }

        @Override
        public ComponentChanges getComponents() {
            return item.getComponents();
        }

        @Override
        public Either<FluidVariant, ItemVariant> getVariant() {
            return Either.right(item);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TlaItemStack) obj;
            return Objects.equals(this.item, that.item) &&
                   this.amount == that.amount &&
                   Objects.equals(this.chance, that.chance);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, amount, chance);
        }

        @Override
        public String toString() {
            return "TlaItemStack[" +
                   "item=" + item + ", " +
                   "amount=" + amount + ", " +
                   "chance=" + chance + ']';
        }
    }
}
