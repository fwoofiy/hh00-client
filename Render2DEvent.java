package dev.hh00.event.events;

import dev.hh00.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Posted when the vanilla HUD is rendered. Use it to draw overlays, watermark, etc.
 */
public class Render2DEvent extends Event {

    private final DrawContext context;
    private final RenderTickCounter tickCounter;

    public Render2DEvent(DrawContext context, RenderTickCounter tickCounter) {
        this.context = context;
        this.tickCounter = tickCounter;
    }

    public DrawContext getContext() {
        return context;
    }

    public RenderTickCounter getTickCounter() {
        return tickCounter;
    }
}
