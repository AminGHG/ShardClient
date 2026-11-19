package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.shared.platform.Game;
import net.am7n.shardclient.shared.anim.Maths;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.modules.Keys;
import net.minecraft.util.Mth;

public final class Zoom extends Module {
    private static final double TICK = 0.05;
    private static final double MAX_DIVISOR = 500;

    private final NumberSetting initial = number("initial", "Zoom", 4, 1, 10, 1, "×");
    private final NumberSetting inTime = number("in_time", "Zoom-in time", 1.0, 0, 5, 0.1, " s");
    private final NumberSetting outTime = number("out_time", "Zoom-out time", 0.5, 0, 5, 0.1, " s");
    private final ChoiceSetting key = choice("key_behaviour", "Key", "Hold", "Hold", "Toggle");
    private final BooleanSetting scroll = bool("scroll", "Scroll to zoom", true);
    private final NumberSetting steps = number("steps", "Scroll steps", 10, 1, 20, 1, "");
    private final NumberSetting perStep = number("per_step", "Zoom per step", 150, 110, 300, 10, "%");
    private final NumberSetting smoothness = number("smoothness", "Scroll smoothness", 70, 0, 100, 5, "%");
    private final BooleanSetting retain = bool("retain", "Keep scroll steps", false);
    private final NumberSetting sensitivity = number("sensitivity", "Relative sensitivity", 100, 0, 150, 5, "%");
    private final BooleanSetting hand = bool("hand", "Zoom the hand too", true);

    private boolean zooming;
    private boolean toggled;

    private double progress;

    private double time;
    private boolean goingIn;
    private int scrollSteps;

    private double scrollSmooth;
    private double divisor = 1;
    private double previousDivisor = 1;

    public Zoom() {
        super("zoom", "Zoom", "Hold the zoom key (C) to zoom in, scroll for more.", Category.VISUAL);
        setEnabled(true);
    }

    private static double easeOutExp(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        return (1 - Math.pow(2, -10 * t)) * 1024 / 1023;
    }

    private static double inverseEaseOutExp(double p) {
        if (p <= 0) {
            return 0;
        }
        if (p >= 1) {
            return 1;
        }
        return -Math.log(1 - p * 1023 / 1024) / Math.log(2) / 10;
    }

    public boolean zooming() {
        return isEnabled() && zooming;
    }

    @Override
    public void tick() {
        boolean free = Game.screen() == null;
        if (key.is("Toggle")) {
            while (Keys.zoom.consumeClick()) {
                if (free) {
                    toggled = !toggled;
                }
            }
            zooming = toggled;
        } else {
            zooming = free && Keys.zoom.isDown();
        }
        if (!zooming && !retain.on()) {
            scrollSteps = 0;
        }

        if (zooming != goingIn) {
            goingIn = zooming;
            time = goingIn ? inverseEaseOutExp(progress) : inverseEaseOutExp(1 - progress);
        }
        double duration = goingIn ? inTime.get() : outTime.get();
        time = duration <= 0 ? 1 : Math.min(1, time + TICK / duration);
        progress = goingIn ? easeOutExp(time) : 1 - easeOutExp(time);

        double share = Mth.lerp(smoothness.get() / 100.0, 1.0, 0.1);
        scrollSmooth += (scrollSteps - scrollSmooth) * share;
        if (Math.abs(scrollSteps - scrollSmooth) < 0.001) {
            scrollSmooth = scrollSteps;
        }

        double multiplier = Mth.lerp(progress, 1.0, 1.0 / initial.get());
        double base = 1 / multiplier;
        double raw = base * Math.pow(perStep.get() / 100.0, scrollSmooth * progressForScroll());
        previousDivisor = divisor;
        divisor = Maths.clamp(raw, 1, MAX_DIVISOR);
    }

    private double progressForScroll() {
        return progress;
    }

    @Override
    protected void onDisable() {
        zooming = toggled = false;
        progress = time = scrollSmooth = 0;
        scrollSteps = 0;
        divisor = previousDivisor = 1;
    }

    public float factor(float partialTick) {
        return isEnabled() ? (float) Mth.lerp(partialTick, previousDivisor, divisor) : 1f;
    }

    public boolean scroll(double amount) {
        if (!zooming() || !scroll.on() || amount == 0) {
            return false;
        }
        int max = steps.intValue();
        int min = -Maths.clamp((int) Math.ceil(Math.log(initial.get()) / Math.log(perStep.get() / 100.0)), 0, max);
        scrollSteps = Maths.clamp(scrollSteps + (amount > 0 ? 1 : -1), min, max);
        return true;
    }

    public double sensitivity(double value) {
        if (!isEnabled()) {
            return value;
        }
        return value / Mth.lerp(sensitivity.get() / 100.0, 1.0, previousDivisor);
    }

    public boolean affectsHand() {
        return hand.on();
    }
}
