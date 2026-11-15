package net.am7n.shardclient.full.modules.hud;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;

public final class ClickCounter {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean leftWas;
    private static boolean rightWas;

    private ClickCounter() {
    }

    public static void sample(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean playing = mc.gui.screen() == null;
        boolean left = playing && mc.mouseHandler.isLeftPressed();
        boolean right = playing && mc.mouseHandler.isRightPressed();
        if (left && !leftWas) {
            LEFT.addLast(now);
        }
        if (right && !rightWas) {
            RIGHT.addLast(now);
        }
        leftWas = left;
        rightWas = right;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
            clicks.removeFirst();
        }
    }

    public static int left() {
        return LEFT.size();
    }

    public static int right() {
        return RIGHT.size();
    }
}
