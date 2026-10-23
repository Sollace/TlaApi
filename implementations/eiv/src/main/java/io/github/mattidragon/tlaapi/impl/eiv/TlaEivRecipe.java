package io.github.mattidragon.tlaapi.impl.eiv;

import java.util.List;

import de.crafty.eiv.common.api.recipe.EivRecipeType;
import de.crafty.eiv.common.api.recipe.IEivRecipeViewType;
import de.crafty.eiv.common.api.recipe.IEivServerRecipe;
import de.crafty.eiv.common.api.recipe.IEivViewRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

class TlaEivRecipe implements IEivServerRecipe {
    public static final EivRecipeType<TlaEivRecipe> TYPE = EivRecipeType.register(Identifier.of("tla", "recipe"), TlaEivRecipe::new);

    private IEivRecipeViewType viewType;
    TlaRecipe recipe;

    private TlaEivRecipe() {
        this(null, null);
    }

    public TlaEivRecipe(TlaRecipe recipe, IEivRecipeViewType viewType) {
        this.recipe = recipe;
        this.viewType = viewType;
    }

    public void setRecipe(TlaRecipe recipe) {
        this.recipe = recipe;
    }

    public TlaRecipe getRecipe() {
        return recipe;
    }

    @Override
    public EivRecipeType<TlaEivRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void loadFromTag(NbtCompound nbt) {
        recipe = nbt.get("recipe", TlaRecipe.CODEC).orElse(null);
        viewType = nbt.get("type", EivImplementation.RECIPE_TYPE_CODEC).orElseThrow();
    }

    @Override
    public void writeToTag(NbtCompound nbt) {
        nbt.putNullable("recipe", TlaRecipe.CODEC, recipe);
        nbt.put("type", EivImplementation.RECIPE_TYPE_CODEC, viewType);
    }

    public List<IEivViewRecipe> getRecipeViews() {
        return List.of(new TlaEivViewRecipe(recipe, viewType));
    }
}
