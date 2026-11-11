package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * The server is about to move the player and the client may cover the screen while it happens. Sent only where
 * the player cannot be harmed until well after the move (ranked's teleport grace, or the match is over). The
 * cover is lifted by {@link MatchStarted} or {@link ReturnToLobby}; a client never keeps it up for long on its
 * own if neither arrives.
 */
public record TransitionStart(Kind kind) implements ClientboundPacket {

    public static final int ID = 0x30;

    public enum Kind {
        OTHER(0),
        /** Into the match arena. */
        ARENA(1),
        /** Back to the lobby after a match. */
        LOBBY(2);

        private final int wireId;

        Kind(int wireId) {
            this.wireId = wireId;
        }

        static Kind byWireId(int wireId) {
            for (Kind kind : values()) {
                if (kind.wireId == wireId) {
                    return kind;
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
        return Feature.MATCH_FLOW;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeByte(kind.wireId);
    }

    static TransitionStart read(PacketReader in) throws ProtocolException {
        return new TransitionStart(Kind.byWireId(in.readUnsignedByte()));
    }
}
