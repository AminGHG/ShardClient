package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.shared.platform.ScreenBase;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class ShardScreen extends ScreenBase {
    private final Screen parent;
    private final Transition transition = Motion.transition(Motion.POPOVER_MS);
    private final List<Focusable> focusable = new ArrayList<>();
    private Element root;
    private int focusIndex = -1;
    private boolean closing;

    protected ShardScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected abstract Element build();

    protected void arrange(Element root, int screenWidth, int screenHeight) {
        Size size = root.measure();
        int rootWidth = Math.min(size.width(), screenWidth - Theme.GAP_LARGE * 2);
        int rootHeight = Math.min(size.height(), screenHeight - Theme.GAP_LARGE * 2);
        root.place((screenWidth - rootWidth) / 2, (screenHeight - rootHeight) / 2, rootWidth, rootHeight);
    }

    @Override
    protected void init() {
        if (root == null) {
            try {
                root = build();
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.error("Could not build {}", getClass().getName(), ex);
                Game.setScreen(parent);
                return;
            }
            transition.show();
        }
        arrange(root, width, height);
        focusable.clear();
        root.collectFocusable(focusable);
        focus(-1);
    }

    @Override
    protected boolean blurBehind() {
        return Theme.look().blurBackdrop();
    }

    @Override
    protected void drawBackdrop(Canvas canvas) {
        canvas.pushAlpha(Maths.clamp(transition.progress(), 0f, 1f));
        canvas.fill(0, 0, width, height, Theme.colors().backdrop());
        canvas.popAlpha();
    }

    @Override
    protected void draw(Canvas canvas) {
        if (root == null) {
            return;
        }
        float progress = transition.progress();
        canvas.push();
        canvas.scale(Lerp.of(Motion.POPOVER_START_SCALE, 1f, progress), width / 2f, height / 2f);
        canvas.pushAlpha(Maths.clamp(progress, 0f, 1f));
        root.render(canvas);
        canvas.popAlpha();
        canvas.pop();
    }

    @Override
    public void tick() {
        if (closing && !transition.isVisible()) {
            Game.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        closing = true;
        transition.hide();
    }

    protected float openProgress() {
        return Maths.clamp(transition.progress(), 0f, 1f);
    }

    public void reopen() {
        closing = false;
        transition.show();
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected boolean onMouseClicked(double x, double y, int button) {
        if (closing || root == null) {
            return false;
        }
        focus(-1);
        return root.mouseClicked(x, y, button);
    }

    @Override
    protected boolean onMouseReleased(double x, double y, int button) {
        return root != null && root.mouseReleased(x, y, button);
    }

    @Override
    protected boolean onMouseDragged(double x, double y, int button) {
        return !closing && root != null && root.mouseDragged(x, y, button);
    }

    @Override
    protected boolean onMouseScrolled(double x, double y, double amount) {
        return !closing && root != null && root.mouseScrolled(x, y, amount);
    }

    @Override
    protected boolean onKey(Key key) {
        if (!closing && root != null && root.keyPressed(key.code())) {
            return true;
        }
        if (key.escape() && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        if (closing) {
            return false;
        }
        if (key.cycleFocus()) {
            moveFocus(key.shift() ? -1 : 1);
            return true;
        }
        if (key.confirm() && focusIndex >= 0) {
            focusable.get(focusIndex).activate();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onChar(String text) {
        return !closing && root != null && root.charTyped(text);
    }

    private void moveFocus(int step) {
        int count = focusable.size();
        for (int i = 1; i <= count; i++) {
            int index = Math.floorMod((focusIndex < 0 && step < 0 ? 0 : focusIndex) + step * i, count);
            if (focusable.get(index).canFocus()) {
                focus(index);
                return;
            }
        }
    }

    private void focus(int index) {
        if (focusIndex >= 0 && focusIndex < focusable.size()) {
            focusable.get(focusIndex).setFocused(false);
        }
        focusIndex = index;
        if (index >= 0) {
            focusable.get(index).setFocused(true);
        }
    }
}
