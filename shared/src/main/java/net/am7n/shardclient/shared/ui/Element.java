package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

import java.util.List;

public abstract class Element {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private boolean visible = true;
    private float grow;

    public abstract Size measure();

    public abstract void render(Canvas canvas);

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

    public boolean keyPressed(int key) {
        return false;
    }

    public boolean charTyped(String text) {
        return false;
    }

    public void collectFocusable(List<Focusable> into) {
    }
}
