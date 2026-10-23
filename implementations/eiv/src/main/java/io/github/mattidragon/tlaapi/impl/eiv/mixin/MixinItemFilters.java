package io.github.mattidragon.tlaapi.impl.eiv.mixin;

import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import de.crafty.eiv.common.overlay.itemlist.view.ItemFilters;
import io.github.mattidragon.tlaapi.api.recipe.TlaStack;
import io.github.mattidragon.tlaapi.impl.eiv.EivImplementation;
import net.minecraft.item.ItemStack;

@Mixin(value = ItemFilters.class, remap = false)
abstract class MixinItemFilters {
    @ModifyReturnValue(method = "fullStackList", at = @At("RETURN"))
    private static List<ItemStack> tla$filterFullStackList(List<ItemStack> stacks) {
        if (EivImplementation.INSTANCE == null) {
            return stacks;
        }
        return stacks.stream().filter(stack -> !EivImplementation.INSTANCE.stackHideFilter.test(TlaStack.of(stack))).collect(Collectors.toList());
    }
}
