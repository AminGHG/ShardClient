package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.platform.Game;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public final class Fonts {
    private static final int MAX_SCALE = 16;
    private static final FontDescription[] REGULAR = load("inter");
    private static final FontDescription[] SEMIBOLD = load("inter_semibold");
    private static final FontDescription BADGE =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "badge"));

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

    public static net.minecraft.network.chat.Style badge(net.minecraft.network.chat.Style style) {
        return style.withFont(BADGE);
    }

    static Component inter(Component text, boolean emphasis) {
        return inter(text, emphasis, 1f);
    }

    static Component inter(Component text, boolean emphasis, float sizeFactor) {
        int guiScale = Game.guiScale();
        int scale = Math.clamp(Math.round(guiScale * Math.max(1f, sizeFactor)), 1, MAX_SCALE);
        FontDescription font = (emphasis ? SEMIBOLD : REGULAR)[scale - 1];
        return text.copy().withStyle(style -> style.withFont(font));
    }
}
