package net.am7n.shardclient.shared.anim;

public final class Cycle {
    private final Clock clock;
    private final long periodNanos;
    private final long startNanos;

    public Cycle(long periodMs) {
        this(periodMs, Clock.SYSTEM);
    }

    public Cycle(long periodMs, Clock clock) {
        this.clock = clock;
        this.periodNanos = Math.max(1, periodMs) * 1_000_000L;
        this.startNanos = clock.nanos();
    }

    public float phase() {
        long elapsed = clock.nanos() - startNanos;
        return (elapsed % periodNanos) / (float) periodNanos;
    }

    public float wave() {
        return (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * phase()));
    }
}
