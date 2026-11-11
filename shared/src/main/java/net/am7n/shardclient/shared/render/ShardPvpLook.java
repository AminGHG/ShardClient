package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Lerp;
import net.minecraft.network.chat.Component;

/**
 * The ShardPvP website's design (shardpvp.xyz), as exactly as Minecraft allows: smooth anti-aliased rounded
 * corners at full screen resolution, screen-pixel hairlines, pill-shaped bars, soft card shadows, the purple
 * primary button and the Inter Tight typeface. Comes in the site's dark and light mode.
 */
public final class ShardPvpLook implements Look {

    public static final ShardPvpLook DARK = new ShardPvpLook("shardpvp-dark", "ShardPvP Dark", Palette.DARK, 0x8C000000);
    public static final ShardPvpLook LIGHT = new ShardPvpLook("shardpvp-light", "ShardPvP Light", Palette.LIGHT, 0x2E161024);

    // Radii in GUI pixels, scaled from the website's CSS (cards 20px, buttons 12px) to Minecraft's text size.
    private static final float RADIUS_CARD = 10f;
    private static final float RADIUS_BUTTON = 6f;
    private static final float RADIUS_WELL = 5f;
    private static final float SHADOW_BLUR = 10f;
    private static final float SHADOW_OFFSET = 3f;
    /** Gap between a control and its focus ring, in GUI pixels. */
    private static final int FOCUS_GAP = 2;

    private final String id;
    private final String displayName;
    private final Palette colors;
    private final int shadow;

    private ShardPvpLook(String id, String displayName, Palette colors, int shadow) {
        this.id = id;
        this.displayName = displayName;
        this.colors = colors;
        this.shadow = shadow;
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

    /** The website's 1 CSS-pixel hairline: about half a GUI pixel, never thinner than one screen pixel. */
    private static int hairline() {
        return Math.max(1, Math.round(SmoothShapes.guiScale() * 0.5f));
    }

    @Override
    public void panel(Canvas canvas, int x, int y, int width, int height) {
        canvas.smoothShadow(x, y, width, height, RADIUS_CARD, SHADOW_BLUR, SHADOW_OFFSET, shadow);
        canvas.smoothRect(x, y, width, height, RADIUS_CARD, colors.surface());
        canvas.smoothFrame(x, y, width, height, RADIUS_CARD, hairline(), colors.hairline());
    }

    @Override
    public void button(Canvas canvas, int x, int y, int width, int height, boolean primary, float highlight,
                       boolean enabled, boolean focused) {
        if (!enabled) {
            canvas.smoothRect(x, y, width, height, RADIUS_BUTTON, colors.inset());
            canvas.smoothFrame(x, y, width, height, RADIUS_BUTTON, hairline(), colors.hairline());
            return;
        }
        if (primary) {
            switch (Theme.primaryStyle()) {
                case LILAC -> canvas.smoothRect(x, y, width, height, RADIUS_BUTTON,
                        Lerp.color(colors.primary(), colors.primaryHovered(), highlight));
                case WHITE -> canvas.smoothRect(x, y, width, height, RADIUS_BUTTON,
                        Lerp.color(0xFFF5F2FA, 0xFFFFFFFF, highlight));
                case OUTLINE -> {
                    canvas.smoothRect(x, y, width, height, RADIUS_BUTTON,
                            Lerp.color(colors.ghost(), colors.ghostHovered(), highlight));
                    canvas.smoothFrame(x, y, width, height, RADIUS_BUTTON, Math.max(1, SmoothShapes.guiScale()),
                            colors.accentStrong());
                }
                case SOFT -> canvas.smoothRect(x, y, width, height, RADIUS_BUTTON,
                        Lerp.color(0x3DA266EB, 0x5CA266EB, highlight));
            }
        } else {
            canvas.smoothRect(x, y, width, height, RADIUS_BUTTON,
                    Lerp.color(colors.ghost(), colors.ghostHovered(), highlight));
            canvas.smoothFrame(x, y, width, height, RADIUS_BUTTON, hairline(), colors.hairline());
        }
        if (focused) {
            canvas.smoothFrame(x - FOCUS_GAP, y - FOCUS_GAP, width + FOCUS_GAP * 2, height + FOCUS_GAP * 2,
                    RADIUS_BUTTON + FOCUS_GAP, Math.max(2, SmoothShapes.guiScale()), colors.accentStrong());
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
        canvas.smoothRect(x, y, size, size, RADIUS_WELL, colors.inset());
        canvas.smoothFrame(x, y, size, size, RADIUS_WELL, hairline(), colors.hairline());
    }

    @Override
    public int barHeight() {
        return 4;
    }

    @Override
    public void barTrack(Canvas canvas, int x, int y, int width, int height) {
        canvas.smoothRect(x, y, width, height, height / 2f, colors.ghost());
    }

    @Override
    public void barFill(Canvas canvas, int x, int y, int width, int height, int argb) {
        if (width > 0) {
            canvas.smoothRect(x, y, Math.max(width, height), height, height / 2f, argb);
        }
    }

    @Override
    public int scrollbarWidth() {
        return 3;
    }

    @Override
    public void scrollbar(Canvas canvas, int x, int y, int width, int height, int handleY, int handleHeight) {
        canvas.smoothRect(x, y, width, height, width / 2f, colors.ghost());
        canvas.smoothRect(x, handleY, width, handleHeight, width / 2f, colors.accentStrong());
    }
}
