package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class SearchField extends Element {
    private static final int HEIGHT = 18;
    private static final int MAX_LENGTH = 40;

    private final String placeholder;
    private final Consumer<String> onChange;
    private final Tween focusAmount = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private final Tween hoverAmount = new Tween(0, Theme.FAST_MS, Easing.OUT_CUBIC);
    private final Hover hover = new Hover();
    private String text = "";
    private boolean focused;

    public SearchField(String placeholder, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.onChange = onChange;
    }

    public String text() {
        return text;
    }

    public boolean focused() {
        return focused;
    }

    public void focus(boolean value) {
        focused = value;
    }

    public void clear() {
        focused = false;
        set("");
    }

    private void set(String next) {
        if (!next.equals(text)) {
            text = next;
            onChange.accept(text);
        }
    }

    @Override
    public Size measure() {
        return new Size(120, HEIGHT);
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

        int iconX = x + 6;
        int iconY = y + (height - 9) / 2;
        int iconColor = Lerp.color(colors.textMuted(), colors.text(), focus);
        Theme.look().outline(canvas, iconX, iconY, 7, 7, 3.5f, 1, iconColor);
        canvas.fill(iconX + 6, iconY + 6, 2, 2, iconColor);

        int textX = x + 18;
        int textY = y + (height - 8) / 2;
        canvas.clip(textX, y, width - 22, height);
        if (text.isEmpty()) {
            canvas.text(Theme.text(Component.literal(placeholder), false), textX, textY, colors.textDisabled(), false);
        } else {
            canvas.text(Theme.text(Component.literal(text), false), textX, textY, colors.text(), false);
        }
        if (focused && System.currentTimeMillis() / 530 % 2 == 0) {
            int caretX = textX + (text.isEmpty() ? 0 : canvas.textWidth(Theme.text(Component.literal(text), false)));
            canvas.fill(caretX, textY - 1, 1, 10, colors.accentStrong());
        }
        canvas.unclip();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = contains(mouseX, mouseY);
        focused = inside;
        return inside;
    }

    @Override
    public boolean keyPressed(int key) {
        if (!focused) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (text.isEmpty()) {
                    focused = false;
                } else {
                    set("");
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!text.isEmpty()) {
                    boolean ctrl = GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                            || GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
                    set(ctrl ? "" : text.substring(0, text.length() - 1));
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                focused = false;
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
        if (text.length() + typed.length() <= MAX_LENGTH) {
            set(text + typed);
        }
        return true;
    }
}
