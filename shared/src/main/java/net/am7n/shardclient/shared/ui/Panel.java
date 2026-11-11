package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;

/** A surface around one child, drawn by the active look. */
public final class Panel extends Container {

    private int padding = Theme.PANEL_PADDING;
    private int minWidth;

    public Panel(Element content) {
        children.add(content);
    }

    public Panel padding(int padding) {
        this.padding = padding;
        return this;
    }

    public Panel minWidth(int minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    @Override
    public Size measure() {
        Size content = children.get(0).measure();
        return new Size(Math.max(minWidth, content.width() + padding * 2), content.height() + padding * 2);
    }

    @Override
    protected void layout() {
        children.get(0).place(x + padding, y + padding, width - padding * 2, height - padding * 2);
    }

    @Override
    public void render(Canvas canvas) {
        Theme.look().panel(canvas, x, y, width, height);
        super.render(canvas);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A panel is solid: a click on it never reaches what is behind.
        return super.mouseClicked(mouseX, mouseY, button) || contains(mouseX, mouseY);
    }
}
