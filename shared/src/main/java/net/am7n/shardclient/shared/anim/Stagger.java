package net.am7n.shardclient.shared.anim;

public record Stagger(int count, long delayMs, long itemMs) {
    public long totalMs() {
        return count <= 0 ? 0 : delayMs * (count - 1) + itemMs;
    }

    public float item(float linear, int index, Easing easing) {
        if (itemMs <= 0) {
            return linear > 0 ? 1 : 0;
        }
        float elapsed = Math.clamp(linear, 0f, 1f) * totalMs();
        float t = (elapsed - index * delayMs) / (float) itemMs;
        return easing.apply(Math.clamp(t, 0f, 1f));
    }
}
