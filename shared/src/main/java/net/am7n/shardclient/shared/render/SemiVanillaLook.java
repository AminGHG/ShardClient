package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.anim.Lerp;
import net.minecraft.network.chat.Component;

public final class SemiVanillaLook implements Look {
    public static final SemiVanillaLook DARK = new SemiVanillaLook("semi-vanilla", "Semi Vanilla", Palette.DARK);
    public static final SemiVanillaLook LIGHT =
            new SemiVanillaLook("semi-vanilla-light", "Semi Vanilla Light", Palette.LIGHT);

    public static final SemiVanillaLook INSTANCE = DARK;

    private final String id;
    private final String displayName;
    private final Palette colors;

    private SemiVanillaLook(String id, String displayName, Palette colors) {
        this.id = id;
        this.displayName = displayName;
        this.colors = colors;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Palette colors() {
        return colors;
    }

    @Override
    public Component styled(Component text, boolean emphasis) {
        return Fonts.inter(text, emphasis);
    }

    @Override
    public Component styled(Component text, boolean emphasis, float sizeFactor) {
        return Fonts.inter(text, emphasis, sizeFactor);
    }

    @Override
    public boolean textShadow() {
        return false;
    }

    @Override
    public boolean blurBackdrop() {
        return true;
    }

    @Override
    public void panel(Canvas canvas, int x, int y, int width, int height) {
        canvas.roundRect(x, y, width, height, Theme.RADIUS_PANEL, colors.surface());
        canvas.roundFrame(x, y, width, height, Theme.RADIUS_PANEL, colors.hairline());
    }

    private static int gridRadius(float radius) {
        return Maths.clamp(Math.round(radius / 2f), 0, Theme.RADIUS_PANEL);
    }

    @Override
    public void shape(Canvas canvas, int x, int y, int width, int height, float radius, int argb) {
        canvas.roundRect(x, y, width, height, gridRadius(radius), argb);
    }

    @Override
    public void outline(Canvas canvas, int x, int y, int width, int height, float radius, int thickness, int argb) {
        canvas.roundFrame(x, y, width, height, gridRadius(radius), argb);
    }

    @Override
    public void button(Canvas canvas, int x, int y, int width, int height, boolean primary, float highlight,
                       boolean enabled, boolean focused) {
        int radius = Theme.RADIUS_CONTROL;
        if (!enabled) {
            canvas.roundRect(x, y, width, height, radius, colors.inset());
            canvas.roundFrame(x, y, width, height, radius, colors.hairline());
            return;
        }
        if (primary) {
            switch (Theme.primaryStyle()) {
                case LILAC -> canvas.roundRect(x, y, width, height, radius,
                        Lerp.color(colors.primary(), colors.primaryHovered(), highlight));
                case WHITE -> canvas.roundRect(x, y, width, height, radius,
                        Lerp.color(0xFFF5F2FA, 0xFFFFFFFF, highlight));
                case OUTLINE -> {
                    canvas.roundRect(x, y, width, height, radius,
                            Lerp.color(colors.ghost(), colors.ghostHovered(), highlight));
                    canvas.roundFrame(x, y, width, height, radius, colors.accentStrong());
                }
                case SOFT -> canvas.roundRect(x, y, width, height, radius,
                        Lerp.color(0x3DA266EB, 0x5CA266EB, highlight));
            }
        } else {
            canvas.roundRect(x, y, width, height, radius, Lerp.color(colors.ghost(), colors.ghostHovered(), highlight));
            canvas.roundFrame(x, y, width, height, radius, colors.hairline());
        }
        if (focused) {
            canvas.roundFrame(x - 1, y - 1, width + 2, height + 2, radius + 1, colors.accentStrong());
        }
    }

    @Override
    public int buttonTextColor(boolean primary, float highlight, boolean enabled) {
        if (!enabled) {
            return colors.textDisabled();
        }
        if (!primary) {
            return colors.text();
        }
        return switch (Theme.primaryStyle()) {
            case LILAC -> colors.textOnPrimary();
            case WHITE -> 0xFF161024;
            case OUTLINE -> colors.accent();
            case SOFT -> colors.text();
        };
    }

    @Override
    public int wellSize() {
        return 20;
    }

    @Override
    public void well(Canvas canvas, int x, int y, int size) {
        canvas.roundRect(x, y, size, size, Theme.RADIUS_SMALL + 1, colors.inset());
        canvas.roundFrame(x, y, size, size, Theme.RADIUS_SMALL + 1, colors.hairline());
    }

    @Override
    public int barHeight() {
        return 4;
    }

    @Override
    public void barTrack(Canvas canvas, int x, int y, int width, int height) {
        canvas.roundRect(x, y, width, height, height / 2, colors.ghost());
    }

    @Override
    public void barFill(Canvas canvas, int x, int y, int width, int height, int argb) {
        if (width >= height) {
            canvas.roundRect(x, y, width, height, height / 2, argb);
        }
    }

    @Override
    public int scrollbarWidth() {
        return 3;
    }

    @Override
    public void scrollbar(Canvas canvas, int x, int y, int width, int height, int handleY, int handleHeight) {
        canvas.roundRect(x, y, width, height, 1, colors.ghost());
        canvas.roundRect(x, handleY, width, handleHeight, 1, colors.accentStrong());
    }
}
