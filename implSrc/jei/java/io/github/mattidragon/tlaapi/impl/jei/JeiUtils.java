package io.github.mattidragon.tlaapi.impl.jei;

import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class JeiUtils {
    public static final Identifier VANILLA_GUI_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/gui/gui_vanilla.png");
    
    public static Optional<? extends ITypedIngredient<?>> convertStack(IJeiHelpers helpers, TlaStack stack) {
        var manager = helpers.getIngredientManager();
        return switch (stack) {
            case TlaStack.TlaFluidStack fluidStack -> createFluidIngredient(manager, helpers.getPlatformFluidHelper(), fluidStack);
            case TlaStack.TlaItemStack itemStack -> manager.createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack.toStack());
        };
    }

    public static List<ITypedIngredient<?>> convertIngredient(IJeiHelpers helpers, TlaIngredient ingredient) {
        return ingredient.getStacks()
                .stream()
                .map(stack -> convertStack(helpers, stack))
                .<ITypedIngredient<?>>flatMap(Optional::stream)
                .toList();
    }

    public static TlaStack convertStack(ITypedIngredient<?> stack) {
        if (stack.getType() == VanillaTypes.ITEM_STACK) {
            return TlaStack.of(((ItemStack) stack.getIngredient()));
        } else if (stack.getType() == FabricTypes.FLUID_STACK) {
            var ingredient = (IJeiFluidIngredient) stack.getIngredient();
            return TlaStack.of(ingredient.getFluidVariant(), ingredient.getAmount());
        } else {
            return TlaStack.empty();
        }
    }

    public static TlaIngredient convertIngredient(List<ITypedIngredient<?>> stacks) {
        return TlaIngredient.ofStacks(stacks.stream().map(JeiUtils::convertStack).toList());
    }

    public static IDrawable iconToDrawable(IJeiHelpers helpers, CategoryIcon icon) {
        return switch (icon) {
            case CategoryIcon.StackIcon stackIcon ->
                    convertStack(helpers, stackIcon.stack())
                            .map(it -> getDrawableIngredient(helpers.getGuiHelper(), it))
                            .orElseGet(() -> helpers.getGuiHelper().createBlankDrawable(16, 16));
            case CategoryIcon.TextureIcon textureIcon -> new TextureDrawable(textureIcon.texture());
        };
    }

    private static <T> IDrawable getDrawableIngredient(IGuiHelper guiHelper, ITypedIngredient<T> converted) {
        return guiHelper.createDrawableIngredient(converted.getType(), converted.getIngredient());
    }

    private static <F> Optional<ITypedIngredient<F>> createFluidIngredient(IIngredientManager manager, IPlatformFluidHelper<F> helper, TlaStack.TlaFluidStack stack) {
        var variant = stack.getFluidVariant();
        return manager.createTypedIngredient(
                helper.getFluidIngredientType(),
                helper.create(variant.getRegistryEntry(), stack.getAmount(), variant.getComponents())
        );
    }
}
