package net.am7n.shardclient.shared.anim;

public final class Motion {
    private Motion() {
    }

    public static final Easing ENTER = Easing.OUT_CUBIC;
    public static final Easing EXIT = Easing.IN_CUBIC;
    public static final Easing MOVE = Easing.IN_OUT_CUBIC;

    public static final long POPOVER_MS = 200;
    public static final long ROW_STAGGER_MS = 30;
    public static final long PRESS_RELEASE_MS = 140;

    public static final float PRESS_SCALE = 0.97f;
    public static final float POPOVER_START_SCALE = 0.95f;
    public static final float KNOB_STRETCH = 0.12f;
    public static final float PULSE_SCALE = 1.06f;

    public static long exitMs(long enterMs) {
        return Math.round(enterMs * 0.8);
    }

    public static Transition transition(long enterMs) {
        return new Transition(enterMs, ENTER, exitMs(enterMs), EXIT);
    }

    public static float rubberBand(float overshoot, float max) {
        if (max <= 0) {
            return 0;
        }
        float sign = Math.signum(overshoot);
        float distance = Math.abs(overshoot);
        return sign * max * (1 - 1 / (distance / max + 1));
    }
}
