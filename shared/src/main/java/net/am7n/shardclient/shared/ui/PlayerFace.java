package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

import java.util.UUID;

public final class PlayerFace extends Element {
    private final UUID playerId;
    private final int size;

    public PlayerFace(UUID playerId, int size) {
        this.playerId = playerId;
        this.size = size;
    }

    @Override
    public Size measure() {
        return new Size(size, size);
    }

    @Override
    public void render(Canvas canvas) {
        canvas.face(playerId, x + (width - size) / 2, y + (height - size) / 2, size);
    }
}
