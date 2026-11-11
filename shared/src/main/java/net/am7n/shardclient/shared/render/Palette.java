package net.am7n.shardclient.shared.render;

/**
 * One colour scheme, as ARGB. The two schemes are the dark and light modes of the ShardPvP website
 * (shardpvp.xyz), with the same names it uses.
 *
 * @param bg0            page background
 * @param bg1            raised surface, opaque
 * @param text           primary text
 * @param textMuted      secondary text
 * @param textDisabled   text of a control that cannot be used
 * @param textOnPrimary  text on a {@code primary} surface
 * @param accent         the brand accent, for highlighted text and marks
 * @param accentStrong   a stronger accent, for bars and indicators
 * @param primary        fill of the main action button
 * @param primaryHovered that button while hovered or focused
 * @param positive       success
 * @param warning        caution
 * @param negative       failure
 * @param backdrop       dims the world behind a screen
 * @param surface        a panel: the raised surface, slightly see-through like the website's cards
 * @param hairline       the thin line around surfaces and quiet controls
 * @param inset          recessed areas: icon wells, disabled controls
 * @param ghost          fill of a quiet button and of bar tracks
 * @param ghostHovered   a quiet button while hovered or focused
 */
public record Palette(int bg0, int bg1, int text, int textMuted, int textDisabled, int textOnPrimary,
                      int accent, int accentStrong, int primary, int primaryHovered,
                      int positive, int warning, int negative,
                      int backdrop, int surface, int hairline, int inset, int ghost, int ghostHovered) {

    /** The website's dark mode. The default in game: it sits well over a moving world. */
    public static final Palette DARK = new Palette(
            0xFF09070F, 0xFF130F1C, 0xFFF5F2FA, 0xFFB6AEC8, 0xFF746E84, 0xFFFFFFFF,
            0xFFE7CCF2, 0xFFCF99E6, 0xFFA266EB, 0xFFB583F0,
            0xFF6ED648, 0xFFFF9233, 0xFFFF6363,
            0xB309070F, 0xEB130F1C, 0x24FFFFFF, 0x59000000, 0x12FFFFFF, 0x26FFFFFF);

    /** The website's light mode. */
    public static final Palette LIGHT = new Palette(
            0xFFF6F4FA, 0xFFFFFFFF, 0xFF161024, 0xFF58506C, 0xFFA09AAE, 0xFFFFFFFF,
            0xFF6E34BE, 0xFF8046CD, 0xFF8046CD, 0xFF6E34BE,
            0xFF268C28, 0xFFC4620A, 0xFFCE2E2E,
            0x8C161024, 0xF5FFFFFF, 0x24161024, 0x0F161024, 0x12161024, 0x24161024);
}
