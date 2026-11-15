package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;

public interface Look {
    String id();

    String displayName();

    Palette colors();

    Component styled(Component text, boolean emphasis);

    default Component styled(Component text, boolean emphasis, float sizeFactor) {
        return styled(text, emphasis);
    }

    boolean textShadow();

    boolean blurBackdrop();

    void panel(Canvas canvas, int x, int y, int width, int height);

    default void shape(Canvas canvas, int x, int y, int width, int height, float radius, int argb) {
        canvas.smoothRect(x, y, width, height, radius, argb);
    }

    default void outline(Canvas canvas, int x, int y, int width, int height, float radius, int thickness, int argb) {
        canvas.smoothFrame(x, y, width, height, radius, thickness, argb);
    }

    void button(Canvas canvas, int x, int y, int width, int height, boolean primary, float highlight,
                boolean enabled, boolean focused);

    int buttonTextColor(boolean primary, float highlight, boolean enabled);

    int wellSize();

    void well(Canvas canvas, int x, int y, int size);

    int barHeight();

    void barTrack(Canvas canvas, int x, int y, int width, int height);

    void barFill(Canvas canvas, int x, int y, int width, int height, int argb);

    int scrollbarWidth();

    void scrollbar(Canvas canvas, int x, int y, int width, int height, int handleY, int handleHeight);
}
