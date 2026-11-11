package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Lerp;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * The ShardPvP website's design (shardpvp.xyz): flat rounded surfaces with a hairline border, a purple primary
 * button, slim rounded bars and the Inter Tight typeface. Comes in the site's dark and light mode.
 */
public final class WebsiteLook implements Look {

    public static final WebsiteLook DARK = new WebsiteLook("website-dark", "ShardPvP Dark", Palette.DARK);
    public static final WebsiteLook LIGHT = new WebsiteLook("website-light", "ShardPvP Light", Palette.LIGHT);

    private static final FontDescription REGULAR =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "inter"));
    private static final FontDescription SEMIBOLD =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "inter_semibold"));

    private final String id;
    private final String displayName;
    private final Palette colors;

    private WebsiteLook(String id, String displayName, Palette colors) {
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
        FontDescription font = emphasis ? SEMIBOLD : REGULAR;
        return text.copy().withStyle(style -> style.withFont(font));
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
