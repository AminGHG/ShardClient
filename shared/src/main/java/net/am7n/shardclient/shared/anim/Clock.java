package net.am7n.shardclient.shared.anim;

@FunctionalInterface
public interface Clock {
    long nanos();

    Clock SYSTEM = System::nanoTime;
}
