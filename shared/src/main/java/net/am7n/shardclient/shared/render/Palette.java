package net.am7n.shardclient.shared.render;

public record Palette(int bg0, int bg1, int text, int textMuted, int textDisabled, int textOnPrimary,
                      int accent, int accentStrong, int primary, int primaryHovered,
                      int positive, int warning, int negative,
                      int backdrop, int surface, int hairline, int inset, int ghost, int ghostHovered) {
    public static final Palette DARK = new Palette(
            0xFF09070F, 0xFF130F1C, 0xFFF5F2FA, 0xFFB6AEC8, 0xFF746E84, 0xFFFFFFFF,
            0xFFE7CCF2, 0xFFCF99E6, 0xFFA266EB, 0xFFB583F0,
            0xFF6ED648, 0xFFFF9233, 0xFFFF6363,
            0xB309070F, 0xEB130F1C, 0x24FFFFFF, 0x59000000, 0x12FFFFFF, 0x26FFFFFF);

    public static final Palette LIGHT = new Palette(
            0xFFF6F4FA, 0xFFFFFFFF, 0xFF161024, 0xFF58506C, 0xFFA09AAE, 0xFFFFFFFF,
            0xFF6E34BE, 0xFF8046CD, 0xFF8046CD, 0xFF6E34BE,
            0xFF268C28, 0xFFC4620A, 0xFFCE2E2E,
            0x8C161024, 0xF5FFFFFF, 0x24161024, 0x0F161024, 0x12161024, 0x24161024);
}
