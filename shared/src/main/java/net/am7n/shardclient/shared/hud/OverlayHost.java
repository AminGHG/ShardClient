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

public final class OverlayHost {
    private final List<Overlay> overlays = new ArrayList<>();

    public void register(Identifier id) {
        HudElementRegistry.addLast(id, this::render);
    }

    public void add(Overlay overlay) {
        if (!overlays.contains(overlay)) {
            overlays.add(overlay);
        }
    }

    public void addOnTop(Overlay overlay) {
        overlays.remove(overlay);
        overlays.add(overlay);
    }

    public boolean contains(Overlay overlay) {
        return overlays.contains(overlay);
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

    public void render(Canvas canvas) {
        if (overlays.isEmpty()) {
            return;
        }
        draw(canvas);
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (overlays.isEmpty()) {
            return;
        }
        draw(new Canvas(graphics, Minecraft.getInstance().font, -1, -1));
    }

    private void draw(Canvas canvas) {
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
