package net.am7n.shardclient.shared.render;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The active {@link Look} plus the measurements every look shares (spacing, timing). Components never hardcode a
 * colour or texture: they ask the look, so switching it restyles everything at once.
 */
public final class Theme {

    /** Every look a player can pick, default first. */
    public static final List<Look> LOOKS =
            List.of(ShardPvpLook.DARK, ShardPvpLook.LIGHT, SemiVanillaLook.INSTANCE, MinecraftLook.INSTANCE);

    private static Look look = ShardPvpLook.DARK;

    private Theme() {
    }

    /** The look in use. Read it while drawing, never keep a copy: it can change at any time. */
    public static Look look() {
        return look;
    }

    public static Palette colors() {
        return look.colors();
    }

    public static void use(Look next) {
        look = next;
    }

    /** The look with {@code id}, or the default for an unknown or missing id. */
    public static Look byId(String id) {
        for (Look candidate : LOOKS) {
            if (candidate.id().equals(id)) {
                return candidate;
            }
        }
        return LOOKS.get(0);
    }

    /** {@code text} in the active look's font. */
    public static Component text(Component text, boolean emphasis) {
        return look.styled(text, emphasis);
    }

    // Shapes, in GUI pixels
    public static final int RADIUS_PANEL = 6;
    public static final int RADIUS_CONTROL = 4;
    public static final int RADIUS_SMALL = 2;

    // Spacing, in GUI pixels
    public static final int GAP_SMALL = 2;
    public static final int GAP = 4;
    public static final int GAP_LARGE = 8;
    public static final int PANEL_PADDING = 12;
    public static final int BUTTON_HEIGHT = 20;

    // Timing, in milliseconds
    public static final long FAST_MS = 110;
    public static final long NORMAL_MS = 180;
    public static final long SLOW_MS = 300;
}
