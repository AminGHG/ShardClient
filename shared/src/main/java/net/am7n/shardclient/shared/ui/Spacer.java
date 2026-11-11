package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

/** Empty space of a fixed size, or (with {@link #grow(float)}) whatever is left over. */
public final class Spacer extends Element {

    private final Size size;

    public Spacer(int width, int height) {
        this.size = new Size(width, height);
    }

    /** A spacer that takes all spare space in its row or column. */
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
