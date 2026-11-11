package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Lerp;
import net.minecraft.network.chat.Component;

/**
 * Between the website and the game: the website's dark colours and typeface, but shapes snapped to Minecraft's
 * GUI pixel grid, so rounded corners show their pixel steps like the rest of the game.
 */
public final class SemiVanillaLook implements Look {

    public static final SemiVanillaLook INSTANCE = new SemiVanillaLook();

    private final Palette colors = Palette.DARK;

    private SemiVanillaLook() {
    }

    @Override
    public String id() {
        return "semi-vanilla";
    }

    @Override
    public String displayName() {
        return "Semi Vanilla";
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
            canvas.roundRect(x, y, width, height, radius, Lerp.color(colors.primary(), colors.primaryHovered(), highlight));
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
        return primary ? colors.textOnPrimary() : colors.text();
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
