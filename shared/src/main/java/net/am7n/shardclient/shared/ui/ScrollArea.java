package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;

/**
 * A window onto content taller than itself. The mouse wheel scrolls it smoothly; a slim bar shows the position
 * when there is something to scroll.
 */
public final class ScrollArea extends Container {

    private static final int WHEEL_STEP = 24;

    private final int viewHeight;
    private final Tween scroll = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private int contentHeight;

    /** @param viewHeight the height this area asks for; its content may be any height */
    /** Jumps back to the top, e.g. when the content is replaced. */
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
        contentHeight = children.get(0).measure().height();
        // Keep a running scroll animation; only pull the target back if the content got shorter.
        float clamped = Math.clamp(scroll.target(), 0, maxScroll());
        if (clamped != scroll.target()) {
            scroll.set(clamped);
        }
        placeContent();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    private void placeContent() {
        int contentWidth = width - Theme.look().scrollbarWidth() - Theme.GAP_SMALL;
        children.get(0).place(x, y - Math.round(scroll.get()), contentWidth, contentHeight);
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
        scroll.set(Math.clamp(scroll.target() - (float) amount * WHEEL_STEP, 0, max));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Content scrolled out of view must not be clickable.
        return contains(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
    }
}
