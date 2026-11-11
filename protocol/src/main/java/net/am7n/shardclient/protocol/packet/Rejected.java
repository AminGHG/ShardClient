package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * The server will not (or no longer) talk to this client; the client falls back to plain Minecraft behaviour.
 * Sent as the answer to a {@link Hello}, or later when the integration is switched off on the server.
 *
 * @param serverMinVersion the range the server speaks, so the client can tell whether it is too old or too new
 */
public record Rejected(Reason reason, int serverMinVersion, int serverMaxVersion) implements ClientboundPacket {

    public static final int ID = 0x02;

    public enum Reason {
        /** Anything this build has no name for. */
        OTHER(0),
        /** No protocol version in common. */
        UNSUPPORTED_VERSION(1),
        /** The integration is switched off on the server. */
        DISABLED(2),
        /** The hello was not understood (unknown product, broken values). */
        INVALID_HELLO(3);

        private final int wireId;

        Reason(int wireId) {
            this.wireId = wireId;
        }

        static Reason byWireId(int wireId) {
            for (Reason reason : values()) {
                if (reason.wireId == wireId) {
                    return reason;
                }
            }
            return OTHER;
        }
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Feature feature() {
        return null;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeByte(reason.wireId).writeInt(serverMinVersion).writeInt(serverMaxVersion);
    }

    static Rejected read(PacketReader in) throws ProtocolException {
        return new Rejected(Reason.byWireId(in.readUnsignedByte()), in.readInt(), in.readInt());
    }
}
