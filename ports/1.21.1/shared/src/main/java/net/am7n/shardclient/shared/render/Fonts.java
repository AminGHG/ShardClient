package net.am7n.shardclient.shared.render;

import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.shared.platform.Game;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class Fonts {
    private static final int MAX_SCALE = 16;
    private static final ResourceLocation[] REGULAR = load("inter");
    private static final ResourceLocation[] SEMIBOLD = load("inter_semibold");
    private static final ResourceLocation BADGE =
            Assets.id(Asset.of("shardclient-shared", "badge"));

    private Fonts() {
    }

    private static ResourceLocation[] load(String name) {
        ResourceLocation[] fonts = new ResourceLocation[MAX_SCALE];
        for (int scale = 1; scale <= MAX_SCALE; scale++) {
            fonts[scale - 1] = Assets.id(Asset.of("shardclient-shared", name + "_x" + scale));
        }
        return fonts;
    }

    public static net.minecraft.network.chat.Style badge(net.minecraft.network.chat.Style style) {
        return style.withFont(BADGE);
    }

    public static net.minecraft.network.chat.Style font(net.minecraft.network.chat.Style style, Asset font) {
        return style.withFont(Assets.id(font));
    }

    static Component inter(Component text, boolean emphasis) {
        return inter(text, emphasis, 1f);
    }

    static Component inter(Component text, boolean emphasis, float sizeFactor) {
        int guiScale = Game.guiScale();
        int scale = Maths.clamp(Math.round(guiScale * Math.max(1f, sizeFactor)), 1, MAX_SCALE);
        ResourceLocation font = (emphasis ? SEMIBOLD : REGULAR)[scale - 1];
        return text.copy().withStyle(style -> style.withFont(font));
    }
}
