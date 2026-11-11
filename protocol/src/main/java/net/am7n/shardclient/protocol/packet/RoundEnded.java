package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/** A round is over. Scores already include it and are from this player's point of view. */
public record RoundEnded(boolean won, int scoreSelf, int scoreOpponent) implements ClientboundPacket {

    public static final int ID = 0x32;

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
        out.writeBoolean(won).writeInt(scoreSelf).writeInt(scoreOpponent);
    }

    static RoundEnded read(PacketReader in) throws ProtocolException {
        return new RoundEnded(in.readBoolean(), in.readInt(), in.readInt());
    }
}
