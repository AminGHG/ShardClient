package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

public record MatchEnded(Result result, int scoreSelf, int scoreOpponent, int ratingBefore, int ratingAfter,
                         String rankBefore, int beforeFloor, int beforeCeiling,
                         String rankAfter, int afterFloor, int afterCeiling) implements ClientboundPacket {
    public static final int ID = 0x33;
    public static final int MAX_RANK_CHARS = 48;

    public enum Result {
        OTHER(0),
        WIN(1),
        LOSS(2),

        CANCELLED(3);

        private final int wireId;

        Result(int wireId) {
            this.wireId = wireId;
        }

        static Result byWireId(int wireId) {
            for (Result result : values()) {
                if (result.wireId == wireId) {
                    return result;
                }
            }
            return OTHER;
        }
    }

    public static MatchEnded unrated(Result result, int scoreSelf, int scoreOpponent) {
        return new MatchEnded(result, scoreSelf, scoreOpponent, 0, 0, "", 0, 0, "", 0, 0);
    }

    public boolean rated() {
        return ratingBefore != 0 || ratingAfter != 0;
    }

    public int ratingChange() {
        return ratingAfter - ratingBefore;
    }

    public boolean rankChanged() {
        return !rankBefore.isEmpty() && !rankAfter.isEmpty() && !rankBefore.equals(rankAfter);
    }

    public boolean hasRankProgress() {
        return rated() && afterCeiling > afterFloor && (!rankChanged() || beforeCeiling > beforeFloor);
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
        out.writeByte(result.wireId).writeInt(scoreSelf).writeInt(scoreOpponent)
                .writeInt(ratingBefore).writeInt(ratingAfter)
                .writeString(rankBefore, MAX_RANK_CHARS).writeInt(beforeFloor).writeInt(beforeCeiling)
                .writeString(rankAfter, MAX_RANK_CHARS).writeInt(afterFloor).writeInt(afterCeiling);
    }

    static MatchEnded read(PacketReader in) throws ProtocolException {
        return new MatchEnded(Result.byWireId(in.readUnsignedByte()), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(),
                in.readString(MAX_RANK_CHARS), in.readInt(), in.readInt(),
                in.readString(MAX_RANK_CHARS), in.readInt(), in.readInt());
    }
}
