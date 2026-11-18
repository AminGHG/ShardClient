package net.am7n.shardclient.shared.hud;

import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.platform.Hud;
import net.am7n.shardclient.shared.render.Canvas;

import java.util.ArrayList;
import java.util.List;

public final class OverlayHost {
    private final List<Overlay> overlays = new ArrayList<>();

    public void register(String name) {
        Hud.addLayer(name, this::render);
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
