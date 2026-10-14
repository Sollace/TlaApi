package io.github.mattidragon.tlaapi.impl.emi;

import java.util.function.Function;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import io.github.mattidragon.tlaapi.api.recipe.BasicTlaRecipe;
import io.github.mattidragon.tlaapi.api.recipe.TlaCategory;
import io.github.mattidragon.tlaapi.api.recipe.TlaRecipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

final class EmiTlaRecipe {

    static TlaRecipe of(Function<EmiRecipeCategory, TlaCategory> context, EmiRecipe recipe) {
        if (recipe instanceof TlaEmiRecipe r) {
            return r.getUnderlying();
        }
        return new BasicTlaRecipe(
                context.apply(recipe.getCategory()),
                recipe.getId() == null ? null : RegistryKey.of(RegistryKeys.RECIPE, recipe.getId()),
                () -> recipe.getInputs().stream().map(EmiUtils::convertIngredient).toList(),
                () -> recipe.getOutputs().stream().map(EmiUtils::convertStack).toList(),
                () -> recipe.getCatalysts().stream().map(EmiUtils::convertIngredient).toList()
        );
    }
}
