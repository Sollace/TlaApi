package io.github.mattidragon.tlaapi.impl.client.rei.util;

import java.util.Objects;

import io.github.mattidragon.tlaapi.impl.Untyped;
import io.github.mattidragon.tlaapi.impl.rei.ReiImplementation;
import io.github.mattidragon.tlaapi.impl.rei.ReiTlaRecipe;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.minecraft.screen.ScreenHandler;

public record ReiPlacementHandler<C extends ScreenHandler>(
        ReiImplementation.PlacementHandler<C> handler
    ) implements SimpleTransferHandler {

    @Override
    public ApplicabilityResult checkApplicable(Context context) {
        if (context.getMenu().getType() == handler.screenType() && handler.placementHandler().isApplicable(handler.category(), ReiTlaRecipe.of(context.getDisplay()))) {
            return ApplicabilityResult.createApplicable();
        }

        return ApplicabilityResult.createNotApplicable();
    }

    @Override
    public Iterable<SlotAccessor> getInputSlots(Context context) {
        return handler.placementHandler().getCraftingSlots(ReiTlaRecipe.of(context.getDisplay()), Untyped.cast(context.getMenu()))
                .filter(Objects::nonNull)
                .map(SlotAccessor::fromSlot)
                .toList();
    }

    @Override
    public Iterable<SlotAccessor> getInventorySlots(Context context) {
        return handler.placementHandler().getInputSlots(Untyped.cast(context.getMenu()))
                .filter(Objects::nonNull)
                .map(SlotAccessor::fromSlot)
                .toList();
    }

}
