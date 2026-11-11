package net.am7n.shardclient.shared.anim;

/**
 * A number that moves to its target over time instead of jumping. Giving it a new target while it is moving
 * starts from wherever it is right now, so an animation can be interrupted or reversed at any point without a
 * jump. Reading it is all it takes to drive it: there is nothing to tick.
 */
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

    /** The value right now. */
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

    /** Starts moving towards {@code target} from the current value. */
    public void set(float target) {
        if (target == to) {
            return;
        }
        from = get();
        to = target;
        startNanos = clock.nanos();
        moving = true;
    }

    /** Jumps to {@code value} without animating. */
    public void snap(float value) {
        from = value;
        to = value;
        moving = false;
    }

    public float target() {
        return to;
    }

    /** True once the value has arrived. */
    public boolean isSettled() {
        get();
        return !moving;
    }
}
