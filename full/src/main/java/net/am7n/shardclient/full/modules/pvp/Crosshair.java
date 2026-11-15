package net.am7n.shardclient.full.modules.pvp;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.shared.render.Canvas;

public final class Crosshair extends Module {
    private final ChoiceSetting shape = choice("shape", "Shape", "Cross", "Cross", "Dot", "Circle", "T");
    private final ChoiceSetting color = choice("color", "Colour", "White", "White", "Lilac", "Green", "Red", "Cyan", "Yellow");
    private final NumberSetting size = number("size", "Size", 5, 2, 12, 1, " px");
    private final NumberSetting gap = number("gap", "Gap", 2, 0, 8, 1, " px");
    private final NumberSetting thickness = number("thickness", "Thickness", 1, 1, 3, 1, " px");
    private final BooleanSetting outline = bool("outline", "Outline", true);
    private final BooleanSetting dynamic = bool("dynamic", "Widen while attack recharges", false);
    private final BooleanSetting centerDot = bool("dot", "Centre dot", false);

    public Crosshair() {
        super("crosshair", "Custom crosshair", "Your own crosshair: shape, size, gap and colour.", Category.PVP);
    }

    private int argb() {
        return switch (color.get()) {
            case "Lilac" -> 0xFFB98CFF;
            case "Green" -> 0xFF5CFF8A;
            case "Red" -> 0xFFFF5A5A;
            case "Cyan" -> 0xFF5CE1FF;
            case "Yellow" -> 0xFFFFE15C;
            default -> 0xFFFFFFFF;
        };
    }

    public void draw(Canvas canvas) {
        int cx = canvas.width() / 2;
        int cy = canvas.height() / 2;
        int t = thickness.intValue();
        int len = size.intValue();
        int g = gap.intValue();
        if (dynamic.on() && mc.player != null) {
            g += Math.round((1f - mc.player.getAttackStrengthScale(0f)) * 4);
        }
        int c = argb();
        switch (shape.get()) {
            case "Dot" -> bar(canvas, cx - t, cy - t, t * 2, t * 2, c);
            case "Circle" -> {
                int r = len + g;
                if (outline.on()) {
                    canvas.smoothFrame(cx - r - 1, cy - r - 1, r * 2 + 2, r * 2 + 2, r + 1f, t + 2, 0xC0000000);
                }
                canvas.smoothFrame(cx - r, cy - r, r * 2, r * 2, r, t, c);
            }
            default -> {
                int half = t / 2;
                if (!shape.is("T")) {
                    bar(canvas, cx - half, cy - g - len, t, len, c);
                }
                bar(canvas, cx - half, cy + g + (t > 1 ? 0 : 1), t, len, c);
                bar(canvas, cx - g - len, cy - half, len, t, c);
                bar(canvas, cx + g + (t > 1 ? 0 : 1), cy - half, len, t, c);
            }
        }
        if (centerDot.on() && !shape.is("Dot")) {
            bar(canvas, cx - t / 2, cy - t / 2, t, t, c);
        }
    }

    private void bar(Canvas canvas, int x, int y, int w, int h, int argb) {
        if (outline.on()) {
            canvas.fill(x - 1, y - 1, w + 2, h + 2, 0xC0000000);
        }
        canvas.fill(x, y, w, h, argb);
    }
}
