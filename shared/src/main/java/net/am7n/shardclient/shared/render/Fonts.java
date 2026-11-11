package net.am7n.shardclient.shared.render;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Inter Tight, the website's typeface (bundled; Minecraft's font fills in characters it lacks).
 *
 * <p>Minecraft rasterises a TrueType font at one fixed resolution (its "oversample"). When that differs from the
 * GUI scale, every glyph is resampled on screen and comes out soft. So there is one definition per GUI scale, each
 * rasterised at exactly that scale: one glyph pixel per screen pixel, as sharp as the screen allows.
 */
final class Fonts {

    /** Highest rasterisation scale: GUI scale times the largest heading size. */
    private static final int MAX_SCALE = 16;
    private static final FontDescription[] REGULAR = load("inter");
    private static final FontDescription[] SEMIBOLD = load("inter_semibold");

    private Fonts() {
    }

    private static FontDescription[] load(String name) {
        FontDescription[] fonts = new FontDescription[MAX_SCALE];
        for (int scale = 1; scale <= MAX_SCALE; scale++) {
            fonts[scale - 1] = new FontDescription.Resource(
                    Identifier.fromNamespaceAndPath("shardclient-shared", name + "_x" + scale));
        }
        return fonts;
    }

    static Component inter(Component text, boolean emphasis) {
        return inter(text, emphasis, 1f);
    }

    /** {@code text} for drawing {@code sizeFactor} times its normal size (a heading drawn scaled up). */
    static Component inter(Component text, boolean emphasis, float sizeFactor) {
        int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int scale = Math.clamp(Math.round(guiScale * Math.max(1f, sizeFactor)), 1, MAX_SCALE);
        FontDescription font = (emphasis ? SEMIBOLD : REGULAR)[scale - 1];
        return text.copy().withStyle(style -> style.withFont(font));
    }
}
