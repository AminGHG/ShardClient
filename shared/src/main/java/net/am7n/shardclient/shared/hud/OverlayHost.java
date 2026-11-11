package net.am7n.shardclient.shared.hud;

import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The ShardClient layer of the in-game HUD: draws the active {@link Overlay}s on top of Minecraft's own HUD.
 * With nothing active it draws nothing and costs one empty list check per frame.
 */
public final class OverlayHost {

    private final List<Overlay> overlays = new ArrayList<>();

    /** Adds the layer to the HUD under {@code id}. Call once, from the mod initializer. */
    public void register(Identifier id) {
        HudElementRegistry.addLast(id, this::render);
    }

    /** Client thread only. */
    public void add(Overlay overlay) {
        overlays.add(overlay);
    }

    public void remove(Overlay overlay) {
        overlays.remove(overlay);
    }

    public void clear() {
        overlays.clear();
    }

    public boolean isEmpty() {
        return overlays.isEmpty();
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (overlays.isEmpty()) {
            return;
        }
        Canvas canvas = new Canvas(graphics, Minecraft.getInstance().font, -1, -1);
        // Iterate over a copy: an overlay may add or remove overlays while drawing.
        for (Overlay overlay : List.copyOf(overlays)) {
            boolean alive;
            try {
                alive = overlay.render(canvas);
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.error("Overlay {} failed and was removed", overlay.getClass().getName(), ex);
                alive = false;
            }
            if (!alive) {
                overlays.remove(overlay);
            }
        }
    }
}
