package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

import java.util.List;

/**
 * Anything on a ShardClient screen: it knows how big it wants to be, is given a rectangle, draws itself and may
 * react to input. Layout is two steps, top down: a parent asks {@link #measure()}, then calls {@link #place}.
 */
public abstract class Element {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private boolean visible = true;
    private float grow;

    /** The size this element would like, before any parent stretches or squeezes it. */
    public abstract Size measure();

    public abstract void render(Canvas canvas);

    /** Gives the element its rectangle; containers lay out their children from it. */
    public final void place(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        layout();
    }

    protected void layout() {
    }

    public boolean isVisible() {
        return visible;
    }

    public Element visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /** Share of a row's or column's spare space this element takes (0: keeps its measured size). */
    public float grow() {
        return grow;
    }

    public Element grow(float weight) {
        this.grow = weight;
        return this;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }

    /** @return true when the click was used and nothing behind this element should see it */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /** Adds every element inside this one that can take keyboard focus, in reading order. */
    public void collectFocusable(List<Focusable> into) {
    }
}
