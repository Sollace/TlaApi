package io.github.mattidragon.tlaapi.impl.jei;

import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

class TlaRecipeCategory implements IRecipeCategory<PreparedRecipe> {
    final TlaCategory category;
    private final IRecipeType<PreparedRecipe> type;
    private final IDrawable icon;

    public TlaRecipeCategory(IJeiHelpers helpers, TlaCategory category) {
        this.category = category;
        this.type = IRecipeType.create(category.getId().getNamespace(), category.getId().getPath(), PreparedRecipe.class);
        this.icon = JeiUtils.iconToDrawable(helpers, category.getIcon());
    }

    @Override
    public IRecipeType<PreparedRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Text getTitle() {
        return category.getName();
    }

    @Override
    public int getWidth() {
        return category.getDisplayWidth();
    }

    @Override
    public int getHeight() {
        return category.getDisplayHeight();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(PreparedRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext guiGraphics, double mouseX, double mouseY) {
        for (PreparedRecipe.DrawableEntry drawable : recipe.getDrawables()) {
            drawable.drawable().draw(guiGraphics, drawable.x(), drawable.y());
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, PreparedRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        tooltip.addAll(recipe.getTooltips((int) mouseX, (int) mouseY));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PreparedRecipe recipe, IFocusGroup focuses) {
        recipe.addSlots(builder);
        // Make JEI think there are slots even when there aren't to force it to display the recipe
        builder.addInvisibleIngredients(RecipeIngredientRole.RENDER_ONLY);
    }

    @Override
    public @Nullable Identifier getRegistryName(PreparedRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public Codec<PreparedRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return PreparedRecipe.CODEC;
    }
}
