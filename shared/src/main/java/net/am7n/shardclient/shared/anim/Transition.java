package net.am7n.shardclient.shared.anim;

public final class Transition {
    public enum State {
        HIDDEN,
        ENTERING,
        SHOWN,
        EXITING
    }

    private final Clock clock;
    private final long enterNanos;
    private final long exitNanos;
    private final Easing enterEasing;
    private final Easing exitEasing;

    private boolean showing;
    private float from;
    private long startNanos;
    private long durationNanos;

    public Transition(long enterMs, Easing enterEasing, long exitMs, Easing exitEasing) {
        this(enterMs, enterEasing, exitMs, exitEasing, Clock.SYSTEM);
    }

    public Transition(long enterMs, Easing enterEasing, long exitMs, Easing exitEasing, Clock clock) {
        this.enterNanos = Math.max(1, enterMs) * 1_000_000L;
        this.exitNanos = Math.max(1, exitMs) * 1_000_000L;
        this.enterEasing = enterEasing;
        this.exitEasing = exitEasing;
        this.clock = clock;
    }

    public static Transition symmetric(long ms, Easing easing) {
        return new Transition(ms, easing, ms, easing);
    }

    public void show() {
        turn(true);
    }

    public void hide() {
        turn(false);
    }

    public void set(boolean shown) {
        turn(shown);
    }

    public void snap(boolean shown) {
        showing = shown;
        from = shown ? 1 : 0;
        durationNanos = 0;
    }

    public float progress() {
        float target = showing ? 1 : 0;
        if (durationNanos <= 0) {
            return target;
        }
        float t = (clock.nanos() - startNanos) / (float) durationNanos;
        if (t >= 1) {
            durationNanos = 0;
            from = target;
            return target;
        }
        Easing easing = showing ? enterEasing : exitEasing;
        return from + (target - from) * easing.apply(Math.max(0, t));
    }

    public State state() {
        float progress = progress();
        if (durationNanos > 0) {
            return showing ? State.ENTERING : State.EXITING;
        }
        return progress > 0 ? State.SHOWN : State.HIDDEN;
    }

    public boolean isVisible() {
        return state() != State.HIDDEN;
    }

    public boolean isShowing() {
        return showing;
    }

    private void turn(boolean show) {
        if (showing == show) {
            return;
        }
        float current = Math.clamp(progress(), 0f, 1f);
        showing = show;
        from = current;
        float distance = show ? 1 - current : current;
        durationNanos = (long) ((show ? enterNanos : exitNanos) * distance);
        startNanos = clock.nanos();
    }
}
