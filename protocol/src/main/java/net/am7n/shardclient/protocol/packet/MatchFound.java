package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

import java.util.UUID;

/**
 * The server paired this player with an opponent. Each player gets their own copy, describing the other one.
 * Receiving it also means the player is out of whatever queue they were in.
 *
 * @param opponentId   the opponent's profile id; the client already knows the skin of anyone on the server
 * @param mode         the gamemode, e.g. {@code 1v1}
 * @param kit          the kit being played; empty when the server does not say
 * @param arena        where the match is played; empty when the server does not say
 * @param opponentRank the opponent's rank as text; empty when there is none (unranked)
 */
public record MatchFound(UUID opponentId, String opponentName, String mode, String kit, String arena,
                         boolean ranked, String opponentRank) implements ClientboundPacket {

    public static final int ID = 0x13;
    public static final int MAX_PLAYER_NAME_CHARS = 16;
    public static final int MAX_TEXT_CHARS = 48;

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Feature feature() {
        return Feature.MATCHMAKING;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeUuid(opponentId).writeString(opponentName, MAX_PLAYER_NAME_CHARS)
                .writeString(mode, MAX_TEXT_CHARS).writeString(kit, MAX_TEXT_CHARS)
                .writeString(arena, MAX_TEXT_CHARS).writeBoolean(ranked)
                .writeString(opponentRank, MAX_TEXT_CHARS);
    }

    static MatchFound read(PacketReader in) throws ProtocolException {
        return new MatchFound(in.readUuid(), in.readString(MAX_PLAYER_NAME_CHARS),
                in.readString(MAX_TEXT_CHARS), in.readString(MAX_TEXT_CHARS),
                in.readString(MAX_TEXT_CHARS), in.readBoolean(), in.readString(MAX_TEXT_CHARS));
    }
}
