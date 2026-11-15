package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.protocol.ProtocolException;

public sealed interface ClientboundPacket
        permits Welcome, Rejected, QueueJoined, QueueLeft, QueueUpdate, MatchFound, ArenaTransition, Notification,
        TransitionStart, MatchStarted, RoundEnded, MatchEnded, ReturnToLobby, ClientUsers {
    int id();

    Feature feature();

    void write(PacketWriter out);

    default byte[] encode() {
        PacketWriter out = new PacketWriter().writeByte(id());
        write(out);
        return out.toByteArray();
    }

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
            case TransitionStart.ID -> TransitionStart.read(in);
            case MatchStarted.ID -> MatchStarted.read(in);
            case RoundEnded.ID -> RoundEnded.read(in);
            case MatchEnded.ID -> MatchEnded.read(in);
            case ReturnToLobby.ID -> ReturnToLobby.read(in);
            case ClientUsers.ID -> ClientUsers.read(in);
            default -> null;
        };
    }
}
