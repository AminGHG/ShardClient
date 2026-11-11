package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.anim.Motion;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Transition;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of every ShardClient screen. A subclass only says what is on it ({@link #build()}); this class centres
 * it, animates it in and out, routes mouse and keyboard to it, and never pauses the game. Closing plays the
 * exit animation first and can be interrupted by reopening.
 */
public abstract class ShardScreen extends Screen {

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

    /** Creates the content. Called once per screen; the result is laid out again on every resize. */
    protected abstract Element build();

    /**
     * Positions the content. By default it gets its measured size, limited to the screen with a margin, and is
     * centred.
     */
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
                // A screen that cannot be built must never trap the player: log it and go back.
                ShardClientRuntime.LOGGER.error("Could not build {}", getClass().getName(), ex);
                minecraft.gui.setScreen(parent);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            extractPanorama(graphics, partialTick);
        }
        if (Theme.look().blurBackdrop()) {
            // Minecraft's own menu blur, at the strength set in its accessibility options (0 turns it off).
            extractBlurredBackground(graphics);
        }
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        canvas.pushAlpha(Math.clamp(transition.progress(), 0f, 1f));
        canvas.fill(0, 0, width, height, Theme.colors().backdrop());
        canvas.popAlpha();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (root == null) {
            return;
        }
        float progress = transition.progress();
        Canvas canvas = new Canvas(graphics, font, mouseX, mouseY);
        canvas.push();
        canvas.scale(Lerp.of(Motion.POPOVER_START_SCALE, 1f, progress), width / 2f, height / 2f);
        canvas.pushAlpha(Math.clamp(progress, 0f, 1f));
        root.render(canvas);
        canvas.popAlpha();
        canvas.pop();
    }

    /** Leaves once the exit animation is over. Done from the tick, never from the middle of a frame. */
    @Override
    public void tick() {
        if (closing && !transition.isVisible()) {
            minecraft.gui.setScreen(parent);
        }
    }

    /** Starts the exit animation; the screen goes away when it has finished. */
    @Override
    public void onClose() {
        closing = true;
        transition.hide();
    }

    /** How far the screen has opened, 0 (hidden) to 1, for screens that draw their own backdrop. */
    protected float openProgress() {
        return Math.clamp(transition.progress(), 0f, 1f);
    }

    /** Cancels a close in progress: the screen animates back in from wherever it was. */
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || root == null) {
            return false;
        }
        focus(-1);
        return root.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return root != null && root.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return !closing && root != null && root.mouseDragged(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return !closing && root != null && root.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && shouldCloseOnEsc()) {
            onClose();
            return true;
        }
        if (closing) {
            return false;
        }
        if (event.isCycleFocus()) {
            moveFocus(event.hasShiftDown() ? -1 : 1);
            return true;
        }
        if ((event.isConfirmation() || event.isSelection()) && focusIndex >= 0) {
            focusable.get(focusIndex).activate();
            return true;
        }
        return false;
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
