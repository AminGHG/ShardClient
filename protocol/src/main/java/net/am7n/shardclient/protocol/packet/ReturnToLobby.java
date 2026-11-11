package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;

/** The player is back in the lobby after a match. */
public record ReturnToLobby() implements ClientboundPacket {

    public static final int ID = 0x34;

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
    }

    static ReturnToLobby read(PacketReader in) {
        return new ReturnToLobby();
    }
}
