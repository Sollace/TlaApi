package io.github.mattidragon.tlaapi.impl.jei;

import io.github.mattidragon.tlaapi.api.recipe.BasicTlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaIngredient;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.Untyped;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientSupplier;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.platform.IPlatformFluidHelperInternal;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class JeiUtils {
    public static final Identifier ARROW_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/atlas/gui/recipe_arrow.png");
    public static final Identifier FILLED_ARROW_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/atlas/gui/recipe_arrow_filled.png");

    public static final Identifier FLAME_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/atlas/gui/icons/flame_empty.png");
    public static final Identifier FILLED_FLAME_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/atlas/gui/icons/flame.png");

    public static final Identifier OUTPUT_SLOT_TEXTURE = Identifier.of(ModIds.JEI_ID, "textures/jei/atlas/gui/output_slot.png");

    public static Optional<? extends ITypedIngredient<?>> convertStack(IJeiHelpers helpers, TlaStack stack) {
        return switch (stack) {
            case TlaStack.TlaEmptyStack empty -> Optional.empty();
            case TlaStack.TlaFluidStack fluidStack -> createFluidIngredient(helpers.getIngredientManager(), helpers.getPlatformFluidHelper(), fluidStack);
            case TlaStack.TlaItemStack itemStack -> helpers.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack.toStack(), false);
        };
    }


    public static <T> TlaRecipe convertRecipe(IRecipeCategory<T> category, TlaCategory convertedCategory, T recipe, IPlatformFluidHelper<?> helper, IRecipeManager recipeManager) {
        var ingredientSupplier = recipeManager.getRecipeIngredients(category, recipe);
        var id = category.getRegistryName(recipe);
        return new BasicTlaRecipe(convertedCategory,
                id == null ? null : RegistryKey.of(RegistryKeys.RECIPE, id),
                () -> getIngredients(helper, ingredientSupplier, RecipeIngredientRole.INPUT).map(TlaStack::asIngredient).toList(),
                () -> getIngredients(helper, ingredientSupplier, RecipeIngredientRole.OUTPUT).toList(),
                () -> getIngredients(helper, ingredientSupplier, RecipeIngredientRole.CRAFTING_STATION).map(TlaStack::asIngredient).toList()
        );
    }

    public static <T> Stream<TlaStack> getIngredients(IPlatformFluidHelper<?> helper, IIngredientSupplier ingredientSupplier, RecipeIngredientRole role) {
        return ingredientSupplier.getIngredients(role)
                .stream()
                .map(input -> JeiUtils.convertStack(helper, input));
    }


    public static List<ITypedIngredient<?>> convertIngredient(IJeiHelpers helpers, TlaIngredient ingredient) {
        return ingredient.stacks()
                .stream()
                .map(stack -> convertStack(helpers, stack))
                .<ITypedIngredient<?>>flatMap(Optional::stream)
                .toList();
    }

    public static TlaStack convertStack(IPlatformFluidHelper<?> helper, ITypedIngredient<?> stack) {
        if (stack.getType() == VanillaTypes.ITEM_STACK) {
            return TlaStack.of(((ItemStack) stack.getIngredient()));
        } else if (stack.getType() == helper.getFluidIngredientType()) {
            return convertFluid(Untyped.cast(stack), Untyped.cast(helper));
        } else {
            return TlaStack.empty();
        }
    }

    private static <T> TlaStack convertFluid(ITypedIngredient<T> stack, IPlatformFluidHelperInternal<T> helper) {
        return TlaStack.of(
                helper.getFluidIngredientType().getBase(stack.getIngredient()),
                helper.getComponentsPatch(stack.getIngredient()),
                helper.getAmount(stack.getIngredient())
        );
    }

    public static TlaIngredient convertIngredient(List<ITypedIngredient<?>> stacks, IPlatformFluidHelper<?> helpers) {
        return TlaIngredient.ofStacks(stacks.stream().map(stack -> convertStack(helpers, stack)));
    }

    public static IDrawable iconToDrawable(IJeiHelpers helpers, CategoryIcon icon) {
        return switch (icon) {
            case CategoryIcon.StackIcon stackIcon ->
                    convertStack(helpers, stackIcon.stack())
                            .map(it -> getDrawableIngredient(helpers.getGuiHelper(), it))
                            .orElseGet(() -> helpers.getGuiHelper().createBlankDrawable(16, 16));
            case CategoryIcon.TextureIcon textureIcon -> new TextureDrawable(textureIcon.texture());
            case CategoryIcon.EmptyIcon empty -> helpers.getGuiHelper().createBlankDrawable(0, 0);
        };
    }

    private static <T> IDrawable getDrawableIngredient(IGuiHelper guiHelper, ITypedIngredient<T> converted) {
        return guiHelper.createDrawableIngredient(converted.getType(), converted.getIngredient());
    }

    private static <F> Optional<ITypedIngredient<F>> createFluidIngredient(IIngredientManager manager, IPlatformFluidHelper<F> helper, TlaStack.TlaFluidStack stack) {
        return manager.createTypedIngredient(
                helper.getFluidIngredientType(),
                helper.create(stack.getRegistryEntry(), stack.getAmount(), stack.getComponents()),
                false
        );
    }
}
