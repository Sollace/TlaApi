package io.github.mattidragon.tlaapi.impl.emi;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.util.Collections;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;

class EmiUtils {
    private static final EmiRenderable EMPTY_RENDERABLE = (draw, x, y, delta) -> {};

    public static EmiStack convertStack(TlaStack stack) {
        return (switch(stack) {
            case TlaStack.TlaEmptyStack empty -> EmiStack.EMPTY;
            case TlaStack.TlaFluidStack fluidStack -> EmiStack.of(fluidStack.getFluid(), fluidStack.getComponents(), fluidStack.getAmount());
            case TlaStack.TlaItemStack itemStack -> EmiStack.of(itemStack.toStack(), itemStack.getAmount());
        }).setChance((float)stack.getChance().orElse(1D));
    }

    public static EmiIngredient convertIngredient(TlaIngredient ingredient) {
        return Collections.reduce(ingredient.stacks(), EmiStack.EMPTY, EmiUtils::convertStack, stacks -> {
            // Emi ingredients store the chance in the ingredient itself, not in the stacks
            // We calculate the average chance of all stacks and set it in the ingredient
            // As emi doesn't differentiate between 100% and a lack of chance, we set it to 1 if there is no chance
            var list = stacks.toList();
            float chance = (float) list.stream().mapToDouble(EmiStack::getChance).average().orElse(1);
            return EmiIngredient.of(list).setChance(chance);
        });
    }

    public static TlaStack convertStack(EmiStack stack) {
        return (switch(stack.getKey()) {
            case Fluid fluid -> TlaStack.of(fluid, stack.getComponentChanges(), stack.getAmount());
            case Item item -> TlaStack.of(item, stack.getComponentChanges(), stack.getAmount()).withChance(stack.getChance());
            default -> TlaStack.empty();
        }).withChance(stack.getChance());
    }

    public static TlaIngredient convertIngredient(EmiIngredient stack) {
        if (stack instanceof EmiStack emiStack) {
            return TlaIngredient.ofStacks(convertStack(emiStack));
        }
        return TlaIngredient.ofStacks(stack.getEmiStacks().stream().map(EmiUtils::convertStack));
    }

    public static EmiRenderable iconToRenderable(CategoryIcon icon) {
        return switch (icon) {
            case CategoryIcon.StackIcon stackIcon -> convertStack(stackIcon.stack());
            case CategoryIcon.TextureIcon textureIcon -> textureIcon.texture().apply((TextureConfig.UnitRemappingFunction<EmiTexture>)EmiTexture::new);
            case CategoryIcon.EmptyIcon empty -> EMPTY_RENDERABLE;
        };
    }
}
