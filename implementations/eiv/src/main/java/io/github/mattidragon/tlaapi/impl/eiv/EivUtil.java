package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.List;
import java.util.stream.Stream;

import de.crafty.eiv.common.extra.FluidStack;
import de.crafty.eiv.common.recipe.inventory.SlotContent;
import io.github.mattidragon.tlaapi.api.gui.TextureConfig;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.TlaApi;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

class EivUtil {
    public static final Identifier BLANK_TEXTURE = TlaApi.id("textures/gui/blank.png");
    public static final Identifier WIDGETS_TEXTURE = Identifier.of("eiv", "textures/gui/eiv_widgets.png");
    public static final TextureConfig EMPTY_ARROW = widgetSprite(14, 14, 14, 0);
    public static final TextureConfig FULL_ARROW = widgetSprite(14, 14, 14, 0);
    public static final TextureConfig EMPTY_FLAME = widgetSprite(24, 16, 0, 0);
    public static final TextureConfig FULL_FLAME = widgetSprite(24, 16, 0, 0);
    public static final TextureConfig PLUS = TextureConfig.builder()
            .texture(TlaApi.id("textures/gui/recipe_plus_sign.png"))
            .size(13, 13)
            .textureSize(13, 13)
            .build();

    public static TextureConfig widgetSprite(int width, int height, int u, int v) {
        return TextureConfig.builder()
                .texture(EivUtil.WIDGETS_TEXTURE)
                .size(width, height)
                .textureSize(128, 128)
                .uv(u, v)
                .build();
    }

    public static SlotContent convertIngredient(TlaIngredient ingredient) {
        List<ItemStack> itemStacks = ingredient.stacks().stream()
                .filter(stack -> stack instanceof TlaStack.TlaItemStack)
                .map(stack -> ((TlaStack.TlaItemStack)stack).toStack())
                .toList();
        if (!itemStacks.isEmpty()) {
            return SlotContent.of(itemStacks);
        }
        List<FluidStack> fluidStacks = ingredient.stacks().stream()
                .filter(stack -> stack instanceof TlaStack.TlaFluidStack)
                .map(stack -> (TlaStack.TlaFluidStack)stack)
                .map(fluid -> new FluidStack(fluid.getFluid(), (int)fluid.getAmount()))
                .toList();
        if (!fluidStacks.isEmpty()) {
            return SlotContent.ofFluidList(fluidStacks);
        }
        return SlotContent.of();
    }

    public static SlotContent convertStack(TlaStack stack) {
        return switch (stack) {
            case TlaStack.TlaEmptyStack empty -> SlotContent.of();
            case TlaStack.TlaItemStack itemStack -> SlotContent.of(itemStack.toStack());
            case TlaStack.TlaFluidStack fluidStack -> SlotContent.of(new FluidStack(fluidStack.getFluid(), (int)fluidStack.getAmount()));
        };
    }

    public static ItemStack getAsItemStack(TlaStack stack) {
        return convertStack(stack).next();
    }

    public static Stream<ItemStack> getAsItemStacks(TlaIngredient ingredient) {
        return ingredient.stacks().stream().map(EivUtil::getAsItemStack).distinct().filter(stack -> !stack.isEmpty());
    }
}
