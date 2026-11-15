package net.am7n.shardclient.protocol;

import net.am7n.shardclient.protocol.packet.ClientboundPacket;
import net.am7n.shardclient.protocol.packet.Hello;
import net.am7n.shardclient.protocol.packet.Rejected;
import net.am7n.shardclient.protocol.packet.Welcome;

public final class Handshake {
    private Handshake() {
    }

    public static ClientboundPacket answer(Hello hello, boolean enabled, long serverFeatures) {
        if (!enabled) {
            return rejected(Rejected.Reason.DISABLED);
        }
        if (hello.kind() == null || hello.minVersion() < 1 || hello.maxVersion() < hello.minVersion()) {
            return rejected(Rejected.Reason.INVALID_HELLO);
        }
        int version = Protocol.negotiate(hello.minVersion(), hello.maxVersion());
        if (version == 0) {
            return rejected(Rejected.Reason.UNSUPPORTED_VERSION);
        }
        return new Welcome(version, hello.features() & serverFeatures);
    }

    private static Rejected rejected(Rejected.Reason reason) {
        return new Rejected(reason, Protocol.VERSION_MIN, Protocol.VERSION_MAX);
    }
}
