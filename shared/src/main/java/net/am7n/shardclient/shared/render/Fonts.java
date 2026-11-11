package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** Inter Tight, the website's typeface (bundled; Minecraft's font fills in characters it lacks). */
final class Fonts {

    private static final FontDescription REGULAR =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "inter"));
    private static final FontDescription SEMIBOLD =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("shardclient-shared", "inter_semibold"));

    private Fonts() {
    }

    static Component inter(Component text, boolean emphasis) {
        FontDescription font = emphasis ? SEMIBOLD : REGULAR;
        return text.copy().withStyle(style -> style.withFont(font));
    }
}
