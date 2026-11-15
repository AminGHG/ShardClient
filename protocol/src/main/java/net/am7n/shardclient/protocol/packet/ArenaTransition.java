package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record ArenaTransition(String arena) implements ClientboundPacket {
    public static final int ID = 0x14;

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
        out.writeString(arena, MatchFound.MAX_TEXT_CHARS);
    }

    static ArenaTransition read(PacketReader in) throws ProtocolException {
        return new ArenaTransition(in.readString(MatchFound.MAX_TEXT_CHARS));
    }
}
