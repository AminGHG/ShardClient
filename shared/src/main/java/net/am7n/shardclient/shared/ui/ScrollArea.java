package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;

public final class ScrollArea extends Container {
    private static final int WHEEL_STEP = 24;

    private static final int INSET = 3;

    private final int viewHeight;
    private final Tween scroll = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private int contentHeight;

    public void scrollToTop() {
        scroll.snap(0);
    }

    public ScrollArea(Element content, int viewHeight) {
        children.add(content);
        this.viewHeight = viewHeight;
    }

    @Override
    public Size measure() {
        Size content = children.get(0).measure();
        return new Size(content.width() + Theme.look().scrollbarWidth() + Theme.GAP_SMALL, viewHeight);
    }

    @Override
    protected void layout() {
        contentHeight = children.get(0).measure().height() + INSET * 2;

        float clamped = Maths.clamp(scroll.target(), 0, maxScroll());
        if (clamped != scroll.target()) {
            scroll.set(clamped);
        }
        placeContent();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    private void placeContent() {
        int contentWidth = width - Theme.look().scrollbarWidth() - Theme.GAP_SMALL - INSET * 2;
        children.get(0).place(x + INSET, y + INSET - Math.round(scroll.get()), contentWidth, contentHeight - INSET * 2);
    }

    @Override
    public void render(Canvas canvas) {
        if (!scroll.isSettled()) {
            placeContent();
        }
        canvas.clip(x, y, width, height);
        super.render(canvas);
        canvas.unclip();

        int max = maxScroll();
        if (max > 0) {
            int barWidth = Theme.look().scrollbarWidth();
            int handle = Math.max(12, height * height / contentHeight);
            int handleY = y + Math.round((height - handle) * (scroll.get() / max));
            Theme.look().scrollbar(canvas, x + width - barWidth, y, barWidth, height, handleY, handle);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        int max = maxScroll();
        if (max == 0) {
            return false;
        }
        scroll.set(Maths.clamp(scroll.target() - (float) amount * WHEEL_STEP, 0, max));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return contains(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
    }
}
