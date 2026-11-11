package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;

import java.util.ArrayList;
import java.util.List;

/** An element made of other elements. Draws them in order and offers input to the topmost one first. */
public abstract class Container extends Element {

    protected final List<Element> children = new ArrayList<>();

    public Container add(Element child) {
        children.add(child);
        return this;
    }

    public List<Element> children() {
        return children;
    }

    @Override
    public void render(Canvas canvas) {
        for (Element child : children) {
            if (child.isVisible()) {
                child.render(canvas);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.isVisible() && child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean used = false;
        for (Element child : children) {
            used |= child.isVisible() && child.mouseReleased(mouseX, mouseY, button);
        }
        return used;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.isVisible() && child.mouseDragged(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (int i = children.size() - 1; i >= 0; i--) {
            Element child = children.get(i);
            if (child.isVisible() && child.contains(mouseX, mouseY) && child.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void collectFocusable(List<Focusable> into) {
        for (Element child : children) {
            if (child.isVisible()) {
                child.collectFocusable(into);
            }
        }
    }
}
