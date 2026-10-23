package io.github.mattidragon.tlaapi.impl.rei;

import dev.architectury.fluid.FluidStack;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReiUtil {
    public static final Logger LOGGER = LoggerFactory.getLogger("TLA-Api/REI");

    public static EntryStack<?> convertStack(TlaStack stack) {
        return switch (stack) {
            case TlaStack.TlaEmptyStack empty -> EntryStack.empty();
            case TlaStack.TlaFluidStack fluidStack -> EntryStacks.of(FluidStack.create(
                    fluidStack.getFluid(),
                    fluidStack.getAmount(),
                    fluidStack.getComponents()
                ));
            case TlaStack.TlaItemStack itemStack -> EntryStacks.of(itemStack.toStack());
        };
    }

    public static EntryIngredient convertIngredient(TlaIngredient ingredient) {
        var stacks = ingredient.stacks();
        return switch (stacks.size()) {
            case 0 -> EntryIngredient.empty();
            case 1 -> EntryIngredient.of(convertStack(stacks.iterator().next()));
            default -> EntryIngredient.of(stacks.stream().map(ReiUtil::convertStack).toList());
        };
    }

    public static TlaStack convertStack(EntryStack<?> stack) {
        return switch (stack.getValue()) {
            case FluidStack fluidStack -> convertStack(fluidStack);
            case ItemStack itemStack -> TlaStack.of(itemStack);
            default -> TlaStack.empty();
        };
    }

    public static TlaStack convertStack(FluidStack fluidStack) {
        return TlaStack.of(fluidStack.getFluid(), fluidStack.getPatch(), fluidStack.getAmount());
    }

    public static TlaIngredient convertIngredient(EntryIngredient ingredient) {
        return switch (ingredient.size()) {
            case 0 -> TlaIngredient.EMPTY;
            case 1 -> TlaIngredient.ofStacks(convertStack(ingredient.getFirst()));
            default -> TlaIngredient.ofStacks(ingredient.stream().map(ReiUtil::convertStack));
        };
    }
}
