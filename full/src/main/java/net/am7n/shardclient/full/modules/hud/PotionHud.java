package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Palette;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionHud extends HudModule {
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    public PotionHud() {
        super("potions", "Potion effects", "Your active effects and how long they last.", 1.0f, 0.35f);
    }

    private List<MobEffectInstance> effects() {
        List<MobEffectInstance> out = new ArrayList<>(mc.player.getActiveEffects());
        out.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        return out;
    }

    @Override
    public boolean visible() {
        return mc.player != null && !mc.player.getActiveEffects().isEmpty();
    }

    private Component line(MobEffectInstance effect) {
        int amplifier = effect.getAmplifier();
        String level = amplifier >= 0 && amplifier < ROMAN.length ? ROMAN[amplifier] : " " + (amplifier + 1);
        return Component.empty().append(effect.getEffect().value().getDisplayName()).append(level);
    }

    private static String time(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return "∞";
        }
        int seconds = effect.getDuration() / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    @Override
    public Size size(Canvas canvas) {
        int width = 0;
        for (MobEffectInstance effect : effects()) {
            width = Math.max(width, canvas.textWidth(line(effect)) + 8 + canvas.textWidth(Component.literal(time(effect))));
        }
        return new Size(width + PADDING * 2, effects().size() * 11 - 3 + PADDING * 2);
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        Size size = size(canvas);
        drawBackground(canvas, x, y, size.width(), size.height());
        Palette colors = Theme.colors();
        int rowY = y + PADDING;
        for (MobEffectInstance effect : effects()) {
            canvas.text(line(effect), x + PADDING, rowY, colors.text(), net.am7n.shardclient.shared.render.Theme.mcShadow());
            boolean ending = !effect.isInfiniteDuration() && effect.endsWithin(200);
            canvas.textRight(Component.literal(time(effect)), x + size.width() - PADDING, rowY,
                    ending ? 0xFFFF5A5A : colors.textMuted(), net.am7n.shardclient.shared.render.Theme.mcShadow());
            rowY += 11;
        }
    }
}
