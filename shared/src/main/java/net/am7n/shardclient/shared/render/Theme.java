package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;

import java.util.List;

public final class Theme {
    public static final List<Look> LOOKS = List.of(ShardPvpLook.DARK, ShardPvpLook.LIGHT, SemiVanillaLook.DARK,
            SemiVanillaLook.LIGHT, MinecraftLook.INSTANCE);

    private static Look look = ShardPvpLook.DARK;

    public enum Style {
        SHARDPVP("ShardPvP"),
        SEMI_VANILLA("Semi Vanilla"),
        MINECRAFT("Minecraft");

        private final String displayName;

        Style(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static boolean preferLight;

    public static Style style() {
        if (look instanceof SemiVanillaLook) {
            return Style.SEMI_VANILLA;
        }
        return look instanceof MinecraftLook ? Style.MINECRAFT : Style.SHARDPVP;
    }

    public static boolean lightMode() {
        return style() != Style.MINECRAFT && preferLight;
    }

    public static boolean lightModeAvailable() {
        return style() != Style.MINECRAFT;
    }

    public static void useStyle(Style style) {
        look = switch (style) {
            case SHARDPVP -> preferLight ? ShardPvpLook.LIGHT : ShardPvpLook.DARK;
            case SEMI_VANILLA -> preferLight ? SemiVanillaLook.LIGHT : SemiVanillaLook.DARK;
            case MINECRAFT -> MinecraftLook.INSTANCE;
        };
    }

    public static void useLightMode(boolean value) {
        preferLight = value;
        useStyle(style());
    }

    public enum PrimaryStyle { LILAC, WHITE, OUTLINE, SOFT }

    private static PrimaryStyle primaryStyle = PrimaryStyle.OUTLINE;

    private static boolean minecraftButtonFont = true;

    public static PrimaryStyle primaryStyle() {
        return primaryStyle;
    }

    public static void usePrimaryStyle(PrimaryStyle style) {
        primaryStyle = style;
    }

    public static boolean minecraftButtonFont() {
        return minecraftButtonFont;
    }

    public static void useMinecraftButtonFont(boolean value) {
        minecraftButtonFont = value;
    }

    private Theme() {
    }

    public static Look look() {
        return look;
    }

    public static Palette colors() {
        return look.colors();
    }

    public static boolean light() {
        return look.colors() == Palette.LIGHT;
    }

    public static boolean mcShadow() {
        return !light();
    }

    public static void use(Look next) {
        look = next;
        if (!(next instanceof MinecraftLook)) {
            preferLight = next.colors() == Palette.LIGHT;
        }
    }

    public static Look byId(String id) {
        for (Look candidate : LOOKS) {
            if (candidate.id().equals(id)) {
                return candidate;
            }
        }
        return LOOKS.get(0);
    }

    public static Component text(Component text, boolean emphasis, float sizeFactor) {
        return look.styled(text, emphasis, sizeFactor);
    }

    public static Component text(Component text, boolean emphasis) {
        return look.styled(text, emphasis);
    }

    public static final int RADIUS_PANEL = 6;
    public static final int RADIUS_CONTROL = 4;
    public static final int RADIUS_SMALL = 2;

    public static final int GAP_SMALL = 2;
    public static final int GAP = 4;
    public static final int GAP_LARGE = 8;
    public static final int PANEL_PADDING = 12;
    public static final int BUTTON_HEIGHT = 20;

    public static final long FAST_MS = 110;
    public static final long NORMAL_MS = 180;
    public static final long SLOW_MS = 300;
}
