package net.am7n.shardclient.full.modules.pvp;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.shared.render.Canvas;

public final class LowHealthWarning extends Module {
    private static final int DEPTH = 28;

    private final NumberSetting hearts = number("hearts", "Below", 4, 1, 10, 0.5, " hearts");
    private final NumberSetting strength = number("strength", "Strength", 60, 10, 100, 5, "%");

    public LowHealthWarning() {
        super("low_health", "Low health warning", "Red screen edges when your health is low.", Category.PVP);
    }

    @Override
    public void renderHud(Canvas canvas) {
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float limit = (float) (hearts.get() * 2);
        if (health > limit || !mc.player.isAlive()) {
            return;
        }
        float danger = 1f - health / limit;
        double speed = 2 + danger * 4;
        float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 1000.0 * speed * Math.PI);
        float alpha = (strength.floatValue() / 100f) * (0.45f + 0.55f * danger) * pulse;
        int w = canvas.width();
        int h = canvas.height();
        for (int i = 0; i < DEPTH; i++) {
            float fade = 1f - i / (float) DEPTH;
            int a = Math.round(255 * alpha * fade * fade / 3);
            if (a <= 0) {
                continue;
            }
            int c = a << 24 | 0xE01E1E;
            canvas.fill(0, i, w, 1, c);
            canvas.fill(0, h - 1 - i, w, 1, c);
            canvas.fill(i, DEPTH, 1, h - DEPTH * 2, c);
            canvas.fill(w - 1 - i, DEPTH, 1, h - DEPTH * 2, c);
        }
    }
}
