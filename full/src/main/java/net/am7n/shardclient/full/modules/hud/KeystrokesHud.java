package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.shared.anim.Easing;
import net.am7n.shardclient.shared.anim.Lerp;
import net.am7n.shardclient.shared.anim.Tween;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public final class KeystrokesHud extends HudModule {
    private static final int KEY = 20;
    private static final int GAP = 2;
    private static final int WIDTH = KEY * 3 + GAP * 2;

    private final BooleanSetting mouse = bool("mouse", "Mouse buttons", true);
    private final BooleanSetting space = bool("space", "Space bar", true);

    private final Key w = new Key(() -> down(mc.options.keyUp));
    private final Key a = new Key(() -> down(mc.options.keyLeft));
    private final Key s = new Key(() -> down(mc.options.keyDown));
    private final Key d = new Key(() -> down(mc.options.keyRight));
    private final Key lmb = new Key(() -> Game.screen() == null && mc.mouseHandler.isLeftPressed());
    private final Key rmb = new Key(() -> Game.screen() == null && mc.mouseHandler.isRightPressed());
    private final Key jump = new Key(() -> down(mc.options.keyJump));

    public KeystrokesHud() {
        super("keystrokes", "Keystrokes", "Shows the movement keys and mouse buttons you press.", 0.0f, 0.62f);
        background.set(true);
    }

    private static boolean down(KeyMapping key) {
        return key.isDown();
    }

    @Override
    public Size size(Canvas canvas) {
        int height = KEY * 2 + GAP;
        if (mouse.on()) {
            height += GAP + KEY;
        }
        if (space.on()) {
            height += GAP + 12;
        }
        return new Size(WIDTH, height);
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        w.draw(canvas, x + KEY + GAP, y, KEY, KEY, label(mc.options.keyUp, "W"));
        int row = y + KEY + GAP;
        a.draw(canvas, x, row, KEY, KEY, label(mc.options.keyLeft, "A"));
        s.draw(canvas, x + KEY + GAP, row, KEY, KEY, label(mc.options.keyDown, "S"));
        d.draw(canvas, x + (KEY + GAP) * 2, row, KEY, KEY, label(mc.options.keyRight, "D"));
        row += KEY + GAP;
        if (mouse.on()) {
            int half = (WIDTH - GAP) / 2;
            lmb.draw(canvas, x, row, half, KEY, ClickCounter.left() + " CPS");
            rmb.draw(canvas, x + half + GAP, row, WIDTH - half - GAP, KEY, ClickCounter.right() + " CPS");
            row += KEY + GAP;
        }
        if (space.on()) {
            jump.draw(canvas, x, row, WIDTH, 12, "");
            int barWidth = WIDTH / 3;
            canvas.fill(x + (WIDTH - barWidth) / 2, row + 5, barWidth, 1, Theme.colors().text());
        }
    }

    private static String label(KeyMapping key, String fallback) {
        String name = key.getTranslatedKeyMessage().getString();
        return name.length() <= 2 ? name.toUpperCase(java.util.Locale.ROOT) : fallback;
    }

    private final class Key {
        private final BooleanSupplier held;
        private final Tween press = new Tween(0, 70, Easing.OUT_CUBIC);

        Key(BooleanSupplier held) {
            this.held = held;
        }

        void draw(Canvas canvas, int x, int y, int width, int height, String text) {
            Palette colors = Theme.colors();
            press.set(held.getAsBoolean() ? 1 : 0);
            float amount = press.get();
            int idle = background.on() ? colors.surface() : 0x00000000;
            Theme.look().shape(canvas, x, y, width, height, 4f, Lerp.color(idle, colors.text(), amount));
            if (!text.isEmpty()) {
                int color = Lerp.color(colors.text(), colors.bg0() | 0xFF000000, amount);
                canvas.textCentered(Component.literal(text), x + width / 2, y + (height - 8) / 2 + 1, color,
                        amount < 0.5f && Theme.mcShadow());
            }
        }
    }
}
