package net.am7n.shardclient.shared.anim;

/**
 * Items that animate one after another (list rows, menu tiles). Driven by one overall linear progress, so the
 * whole sequence can be played backwards or stopped half-way simply by moving that progress.
 *
 * @param count   number of items
 * @param delayMs how much later each item starts than the one before it
 * @param itemMs  how long a single item takes
 */
public record Stagger(int count, long delayMs, long itemMs) {

    /** How long the whole sequence takes: the time to give the driving {@link Transition}. */
    public long totalMs() {
        return count <= 0 ? 0 : delayMs * (count - 1) + itemMs;
    }

    /**
     * Progress 0..1 of item {@code index} when the whole sequence is at {@code linear} (0..1, not eased).
     */
    public float item(float linear, int index, Easing easing) {
        if (itemMs <= 0) {
            return linear > 0 ? 1 : 0;
        }
        float elapsed = Math.clamp(linear, 0f, 1f) * totalMs();
        float t = (elapsed - index * delayMs) / (float) itemMs;
        return easing.apply(Math.clamp(t, 0f, 1f));
    }
}
