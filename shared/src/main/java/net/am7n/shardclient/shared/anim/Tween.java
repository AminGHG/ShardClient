package net.am7n.shardclient.shared.anim;

public final class Tween {
    private final Clock clock;
    private final long durationNanos;
    private final Easing easing;

    private float from;
    private float to;
    private long startNanos;
    private boolean moving;

    public Tween(float initial, long durationMs, Easing easing) {
        this(initial, durationMs, easing, Clock.SYSTEM);
    }

    public Tween(float initial, long durationMs, Easing easing, Clock clock) {
        this.from = initial;
        this.to = initial;
        this.durationNanos = Math.max(1, durationMs) * 1_000_000L;
        this.easing = easing;
        this.clock = clock;
    }

    public float get() {
        if (!moving) {
            return to;
        }
        float t = (clock.nanos() - startNanos) / (float) durationNanos;
        if (t >= 1) {
            moving = false;
            return to;
        }
        return from + (to - from) * easing.apply(Math.max(0, t));
    }

    public void set(float target) {
        if (target == to) {
            return;
        }
        from = get();
        to = target;
        startNanos = clock.nanos();
        moving = true;
    }

    public void snap(float value) {
        from = value;
        to = value;
        moving = false;
    }

    public float target() {
        return to;
    }

    public boolean isSettled() {
        get();
        return !moving;
    }
}
