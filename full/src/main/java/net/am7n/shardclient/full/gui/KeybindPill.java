package net.am7n.shardclient.full.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Pill;
import net.am7n.shardclient.shared.ui.Size;
import org.lwjgl.glfw.GLFW;

/** A module's key binding: click, then press a key (Esc or Backspace removes the binding). */
final class KeybindPill extends Element {

    private final Module module;
    private final Pill pill;
    private boolean listening;

    KeybindPill(Module module) {
        this.module = module;
        this.pill = new Pill(this::text, direction -> {
            if (direction < 0) {
                module.setKey(Module.NO_KEY);
                listening = false;
            } else {
                listening = !listening;
            }
        });
    }

    private String text() {
        if (listening) {
            return "Press a key";
        }
        return module.key() == Module.NO_KEY ? "None"
                : InputConstants.Type.KEYSYM.getOrCreate(module.key()).getDisplayName().getString();
    }

    @Override
    public Size measure() {
        return pill.measure();
    }

    @Override
    protected void layout() {
        pill.place(x, y, width, height);
    }

    @Override
    public void render(Canvas canvas) {
        pill.setActive(listening);
        pill.render(canvas);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean used = pill.mouseClicked(mouseX, mouseY, button);
        if (!used && listening) {
            listening = false;
        }
        return used;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!listening) {
            return false;
        }
        listening = false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            module.setKey(Module.NO_KEY);
        } else {
            module.setKey(key);
        }
        return true;
    }
}
