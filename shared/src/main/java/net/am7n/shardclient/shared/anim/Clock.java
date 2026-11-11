package net.am7n.shardclient.shared.anim;

/**
 * Where animations read the time. Real time rather than game ticks, so motion is as smooth as the frame rate
 * and keeps running while the game is paused or a tick is slow.
 */
@FunctionalInterface
public interface Clock {

    long nanos();

    Clock SYSTEM = System::nanoTime;
}
