package net.am7n.shardclient.full.gui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Hover;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

final class NameField extends Element {
    private static final int HEIGHT = 20;

    private final String placeholder;
    private final int maxLength;
    private final Consumer<String> onSubmit;
    private final Runnable onCancel;
    private final Tween focusAmount = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private final Tween hoverAmount = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private final Hover hover = new Hover();
    private String text = "";
    private boolean focused;

    NameField(String placeholder, int maxLength, Consumer<String> onSubmit, Runnable onCancel) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
    }

    String text() {
        return text;
    }

    NameField text(String value) {
        text = value.length() > maxLength ? value.substring(0, maxLength) : value;
        return this;
    }

    boolean focused() {
        return focused;
    }

    NameField focus(boolean value) {
        focused = value;
        return this;
    }

    @Override
    public Size measure() {
        return new Size(60, HEIGHT);
    }

    @Override
    public void render(Canvas canvas) {
        Palette colors = Theme.colors();
        boolean pointed = contains(canvas.mouseX(), canvas.mouseY());
        hover.update(pointed && !focused);
        hoverAmount.set(pointed ? 1 : 0);
        focusAmount.set(focused ? 1 : 0);
        float focus = focusAmount.get();
        int fill = Lerp.color(colors.inset(), colors.ghostHovered(), Math.max(hoverAmount.get() * 0.5f, focus));
        Theme.look().shape(canvas, x, y, width, height, 6f, fill);
        Theme.look().outline(canvas, x, y, width, height, 6f, 1,
                Lerp.color(colors.hairline(), colors.accentStrong(), focus));

        int textX = x + 7;
        int textY = y + (height - 8) / 2;
        int room = width - 14;
        canvas.clip(textX, y, room, height);
        if (text.isEmpty()) {
            canvas.text(Theme.text(Component.literal(placeholder), false), textX, textY, colors.textDisabled(), false);
        } else {
            Component shown = Theme.text(Component.literal(text), false);
            int overflow = Math.max(0, canvas.textWidth(shown) + 2 - room);
            canvas.text(shown, textX - overflow, textY, colors.text(), false);
        }
        if (focused && System.currentTimeMillis() / 530 % 2 == 0) {
            int textWidth = text.isEmpty() ? 0 : canvas.textWidth(Theme.text(Component.literal(text), false));
            int caretX = textX + Math.min(textWidth, room - 2);
            canvas.fill(caretX, textY - 1, 1, 10, colors.accentStrong());
        }
        canvas.unclip();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = contains(mouseX, mouseY);
        if (focused && !inside) {
            focused = false;
            onCancel.run();
            return false;
        }
        focused = inside;
        return inside;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!focused) {
            return false;
        }
        boolean ctrl = GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                focused = false;
                onCancel.run();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!text.isEmpty()) {
                    text = ctrl ? "" : text.substring(0, text.length() - 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onSubmit.accept(text);
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (clip != null) {
                        for (int i = 0; i < clip.length(); i++) {
                            char c = clip.charAt(i);
                            if (!Character.isISOControl(c) && text.length() < maxLength) {
                                text += c;
                            }
                        }
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    @Override
    public boolean charTyped(String typed) {
        if (!focused || typed.isEmpty() || Character.isISOControl(typed.codePointAt(0))) {
            return false;
        }
        if (text.length() + typed.length() <= maxLength) {
            text += typed;
        }
        return true;
    }
}
