package io.github.mattidragon.tlaapi.impl.client.rei.util;

import io.github.mattidragon.tlaapi.api.client.StackDragHandler;
import io.github.mattidragon.tlaapi.impl.rei.ReiUtil;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.stream.Stream;

public record TlaDragHandler<T extends Screen>(StackDragHandler<T> handler, Class<T> clazz) implements DraggableStackVisitor<T> {
    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return clazz.isAssignableFrom(screen.getClass()) && handler.appliesTo(unsafeCast(screen));
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<T> context, DraggableStack stack) {
        var targets = handler.getDropTargets(context.getScreen());
        var pos = context.getCurrentPosition();
        if (pos == null) return DraggedAcceptorResult.PASS;

        for (var target : targets) {
            if (target.bounds().contains(pos.x, pos.y)) {
                var reiStack = ReiUtil.convertStack(stack.getStack());
                var accepted = target.ingredientConsumer().apply(reiStack);
                if (accepted) return DraggedAcceptorResult.ACCEPTED;
            }
        }

        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<T> context, DraggableStack stack) {
        return handler.getDropTargets(context.getScreen())
                .stream()
                .map(StackDragHandler.DropTarget::bounds)
                .map(tlaBounds -> new Rectangle(tlaBounds.x(), tlaBounds.y(), tlaBounds.width(), tlaBounds.height()))
                .map(BoundsProvider::ofRectangle);
    }

    @SuppressWarnings("unchecked")
    private <U> U unsafeCast(Object o) {
        return (U) o;
    }
}
