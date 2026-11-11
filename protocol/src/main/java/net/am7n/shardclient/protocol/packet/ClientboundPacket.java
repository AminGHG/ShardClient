package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.protocol.ProtocolException;

/** A message from the server to a client. */
public sealed interface ClientboundPacket
        permits Welcome, Rejected, QueueJoined, QueueLeft, QueueUpdate, MatchFound, ArenaTransition, Notification {

    int id();

    /** The feature this packet belongs to; null for handshake packets, which every client understands. */
    Feature feature();

    void write(PacketWriter out);

    default byte[] encode() {
        PacketWriter out = new PacketWriter().writeByte(id());
        write(out);
        return out.toByteArray();
    }

    /** The packet in {@code message}, or null when its id is unknown to this build (to be ignored). */
    static ClientboundPacket decode(byte[] message) throws ProtocolException {
        if (message.length > Protocol.MAX_CLIENTBOUND_BYTES) {
            throw new ProtocolException("message of " + message.length + " bytes is too large");
        }
        PacketReader in = new PacketReader(message);
        return switch (in.readUnsignedByte()) {
            case Welcome.ID -> Welcome.read(in);
            case Rejected.ID -> Rejected.read(in);
            case QueueJoined.ID -> QueueJoined.read(in);
            case QueueLeft.ID -> QueueLeft.read(in);
            case QueueUpdate.ID -> QueueUpdate.read(in);
            case MatchFound.ID -> MatchFound.read(in);
            case ArenaTransition.ID -> ArenaTransition.read(in);
            case Notification.ID -> Notification.read(in);
            default -> null;
        };
    }
}
