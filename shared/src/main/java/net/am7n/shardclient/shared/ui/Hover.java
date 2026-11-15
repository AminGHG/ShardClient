package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.practice.ui.UiSounds;

public final class Hover {
    private static volatile boolean effects = true;

    private boolean was;

    public static void setEffects(boolean value) {
        effects = value;
    }

    public static boolean effects() {
        return effects;
    }

    public void update(boolean hovered) {
        if (hovered && !was) {
            UiSounds.hover();
        }
        was = hovered;
    }

    public static float lift(float amount) {
        return effects ? amount : 0f;
    }

    public static float scale(float amount, float grow) {
        return effects ? 1f + grow * amount : 1f;
    }
}
