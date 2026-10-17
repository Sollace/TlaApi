package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import de.crafty.eiv.common.api.recipe.IEivRecipeViewType;
import de.crafty.eiv.common.recipe.inventory.RecipeViewMenu.SlotDefinition;
import io.github.mattidragon.tlaapi.api.recipe.CategoryIcon;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

class TlaEivRecipeViewType implements IEivRecipeViewType {
    final TlaCategory category;
    final List<ItemStack> craftReferences;

    @Nullable
    private final TlaEivViewRecipe currentRecipe;

    public TlaEivRecipeViewType(TlaCategory category) {
        this.category = category;
        craftReferences = new ArrayList<>();
        currentRecipe = null;
    }

    private TlaEivRecipeViewType(TlaEivRecipeViewType prime, @Nullable TlaEivViewRecipe currentRecipe) {
        this.category = prime.category;
        this.craftReferences = prime.craftReferences;
        this.currentRecipe = currentRecipe;
    }

    public IEivRecipeViewType bindTo(TlaEivViewRecipe currentRecipe) {
        return new TlaEivRecipeViewType(this, currentRecipe);
    }

    @Override
    public int getDisplayHeight() {
        return category.getDisplayHeight();
    }

    @Override
    public Text getDisplayName() {
        return category.getName();
    }

    @Override
    public int getDisplayWidth() {
        return category.getDisplayWidth();
    }

    @Override
    public Identifier getGuiTexture() {
        return EivUtil.BLANK_TEXTURE;
    }

    @Override
    public ItemStack getIcon() {
        return switch (category.getIcon()) {
            case CategoryIcon.EmptyIcon empty -> ItemStack.EMPTY;
            case CategoryIcon.StackIcon stack -> EivUtil.convertStack(stack.stack()).next();
            case CategoryIcon.TextureIcon texture -> ItemStack.EMPTY;
        };
    }

    @Override
    public List<ItemStack> getCraftReferences() {
        return craftReferences;
    }

    @Override
    public Identifier getId() {
        return category.getId();
    }

    @Override
    public int getSlotCount() {
        return currentRecipe == null ? 0 : currentRecipe.gui.slots.size();
    }

    @Override
    public void placeSlots(SlotDefinition slots) {
        if (currentRecipe != null) {
            for (int i = 0; i < currentRecipe.gui.slots.size(); i++) {
                var slot = currentRecipe.gui.slots.get(i).getBounds();
                slots.addItemSlot(i, slot.x(), slot.y());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof TlaEivRecipeViewType t && t.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, getId());
    }
}
