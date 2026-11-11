package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;

/**
 * A visual style: how every ShardClient surface is drawn. Components only say what they are (a panel, a button,
 * a bar); the active look decides how that appears. Switching the look restyles everything at once.
 */
public interface Look {

    /** Stable id, for the config file. */
    String id();

    /** Name shown in settings. */
    String displayName();

    Palette colors();

    /** {@code text} in this look's font; {@code emphasis} for headings and button labels. */
    Component styled(Component text, boolean emphasis);

    boolean textShadow();

    /** Whether screens blur the world behind them (honours Minecraft's own blur setting). */
    boolean blurBackdrop();

    void panel(Canvas canvas, int x, int y, int width, int height);

    /**
     * A button background.
     *
     * @param highlight 0 at rest, 1 hovered or focused, animated in between
     */
    void button(Canvas canvas, int x, int y, int width, int height, boolean primary, float highlight,
                boolean enabled, boolean focused);

    int buttonTextColor(boolean primary, float highlight, boolean enabled);

    /** Side length of the well an item icon sits in. */
    int wellSize();

    void well(Canvas canvas, int x, int y, int size);

    int barHeight();

    void barTrack(Canvas canvas, int x, int y, int width, int height);

    void barFill(Canvas canvas, int x, int y, int width, int height, int argb);

    int scrollbarWidth();

    void scrollbar(Canvas canvas, int x, int y, int width, int height, int handleY, int handleHeight);
}
