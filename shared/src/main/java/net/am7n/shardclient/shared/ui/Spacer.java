package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

public final class Spacer extends Element {
    private final Size size;

    public Spacer(int width, int height) {
        this.size = new Size(width, height);
    }

    public static Spacer flexible() {
        Spacer spacer = new Spacer(0, 0);
        spacer.grow(1);
        return spacer;
    }

    @Override
    public Size measure() {
        return size;
    }

    @Override
    public void render(Canvas canvas) {
    }
}
