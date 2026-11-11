package net.am7n.shardclient.protocol.packet;

import net.am7n.shardclient.protocol.Feature;
import net.am7n.shardclient.protocol.PacketReader;
import net.am7n.shardclient.protocol.PacketWriter;
import net.am7n.shardclient.protocol.ProtocolException;

/**
 * Something the server wants the player to notice. Plain text: how it looks is the client's business.
 *
 * @param title   short headline; may be empty
 * @param message the text
 */
public record Notification(Category category, Level level, String title, String message)
        implements ClientboundPacket {

    public static final int ID = 0x20;
    public static final int MAX_TITLE_CHARS = 64;
    public static final int MAX_MESSAGE_CHARS = 256;

    public enum Category {
        GENERAL(0),
        MATCHMAKING(1),
        QUEUE(2),
        ANNOUNCEMENT(3),
        PLAYER(4);

        private final int wireId;

        Category(int wireId) {
            this.wireId = wireId;
        }

        static Category byWireId(int wireId) {
            for (Category category : values()) {
                if (category.wireId == wireId) {
                    return category;
                }
            }
            return GENERAL;
        }
    }

    public enum Level {
        INFO(0),
        SUCCESS(1),
        WARNING(2),
        ERROR(3);

        private final int wireId;

        Level(int wireId) {
            this.wireId = wireId;
        }

        static Level byWireId(int wireId) {
            for (Level level : values()) {
                if (level.wireId == wireId) {
                    return level;
                }
            }
            return INFO;
        }
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    public Feature feature() {
        return Feature.NOTIFICATIONS;
    }

    @Override
    public void write(PacketWriter out) {
        out.writeByte(category.wireId).writeByte(level.wireId)
                .writeString(title, MAX_TITLE_CHARS).writeString(message, MAX_MESSAGE_CHARS);
    }

    static Notification read(PacketReader in) throws ProtocolException {
        return new Notification(Category.byWireId(in.readUnsignedByte()), Level.byWireId(in.readUnsignedByte()),
                in.readString(MAX_TITLE_CHARS), in.readString(MAX_MESSAGE_CHARS));
    }
}
