package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;

public final class MinecraftLook implements Look {
    public static final MinecraftLook INSTANCE = new MinecraftLook();

    private static final Asset PANEL = Asset.minecraft("popup/background");
    private static final Asset BUTTON = Asset.minecraft("widget/button");
    private static final Asset BUTTON_HIGHLIGHTED = Asset.minecraft("widget/button_highlighted");
    private static final Asset BUTTON_DISABLED = Asset.minecraft("widget/button_disabled");
    private static final Asset SLOT = Asset.minecraft("container/slot");
    private static final Asset SCROLLER = Asset.minecraft("widget/scroller");
    private static final Asset SCROLLER_TRACK = Asset.minecraft("widget/scroller_background");

    private static final Palette COLORS = new Palette(
            0xFF101010, 0xFF1D1D21, 0xFFFFFFFF, 0xFFA0A0A0, 0xFF707070, 0xFFFFFFFF,
            0xFFD2C0FF, 0xFFD2C0FF, 0xFFD2C0FF, 0xFFE6DCFF,
            0xFF55FF55, 0xFFFFAA00, 0xFFFF5555,
            0xA0101010, 0xFF1D1D21, 0xFF000000, 0xFF1D1D21, 0xFF1D1D21, 0xFF2A2A30);

    private MinecraftLook() {
    }

    @Override
    public String id() {
        return "minecraft";
    }

    @Override
    public String displayName() {
        return "Minecraft";
    }

    @Override
    public Palette colors() {
        return COLORS;
    }

    @Override
    public Component styled(Component text, boolean emphasis) {
        return text;
    }

    @Override
    public boolean textShadow() {
        return true;
    }

    @Override
    public boolean blurBackdrop() {
        return false;
    }

    @Override
    public void panel(Canvas canvas, int x, int y, int width, int height) {
        canvas.sprite(PANEL, x, y, width, height);
    }

    @Override
    public void shape(Canvas canvas, int x, int y, int width, int height, float radius, int argb) {
        canvas.fill(x, y, width, height, argb);
    }

    @Override
    public void outline(Canvas canvas, int x, int y, int width, int height, float radius, int thickness, int argb) {
        canvas.frame(x, y, width, height, argb);
    }

    @Override
    public void button(Canvas canvas, int x, int y, int width, int height, boolean primary, float highlight,
                       boolean enabled, boolean focused) {
        if (!enabled) {
            canvas.sprite(BUTTON_DISABLED, x, y, width, height);
            return;
        }
        canvas.sprite(BUTTON, x, y, width, height);
        float amount = Math.max(highlight, focused ? 1 : 0);
        if (amount > 0) {
            canvas.pushAlpha(amount);
            canvas.sprite(BUTTON_HIGHLIGHTED, x, y, width, height);
            canvas.popAlpha();
        }
    }

    @Override
    public int buttonTextColor(boolean primary, float highlight, boolean enabled) {
        if (!enabled) {
            return COLORS.textMuted();
        }
        return primary ? COLORS.accent() : COLORS.text();
    }

    @Override
    public int wellSize() {
        return 18;
    }

    @Override
    public void well(Canvas canvas, int x, int y, int size) {
        canvas.sprite(SLOT, x, y, size, size);
    }

    @Override
    public int barHeight() {
        return 6;
    }

    @Override
    public void barTrack(Canvas canvas, int x, int y, int width, int height) {
        canvas.fill(x, y, width, height, 0xFF000000);
        canvas.fill(x + 1, y + 1, width - 2, height - 2, COLORS.inset());
    }

    @Override
    public void barFill(Canvas canvas, int x, int y, int width, int height, int argb) {
        if (width > 2) {
            canvas.fill(x + 1, y + 1, width - 2, height - 2, argb);
        }
    }

    @Override
    public int scrollbarWidth() {
        return 6;
    }

    @Override
    public void scrollbar(Canvas canvas, int x, int y, int width, int height, int handleY, int handleHeight) {
        canvas.sprite(SCROLLER_TRACK, x, y, width, height);
        canvas.sprite(SCROLLER, x, handleY, width, handleHeight);
    }
}
