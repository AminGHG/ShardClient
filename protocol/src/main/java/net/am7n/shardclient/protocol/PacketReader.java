package net.am7n.shardclient.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Reads a message body written by {@link PacketWriter}. Every read is bounds-checked. */
public final class PacketReader {

    private final byte[] data;
    private int position;

    public PacketReader(byte[] data) {
        this.data = data;
    }

    public int remaining() {
        return data.length - position;
    }

    public int readUnsignedByte() throws ProtocolException {
        require(1);
        return data[position++] & 0xFF;
    }

    public boolean readBoolean() throws ProtocolException {
        return readUnsignedByte() != 0;
    }

    public int readInt() throws ProtocolException {
        require(4);
        int value = (data[position] & 0xFF) << 24
                | (data[position + 1] & 0xFF) << 16
                | (data[position + 2] & 0xFF) << 8
                | (data[position + 3] & 0xFF);
        position += 4;
        return value;
    }

    public long readLong() throws ProtocolException {
        long high = readInt();
        long low = readInt() & 0xFFFFFFFFL;
        return high << 32 | low;
    }

    public UUID readUuid() throws ProtocolException {
        long most = readLong();
        long least = readLong();
        return new UUID(most, least);
    }

    /** A string of at most {@code maxChars} UTF-16 units; longer is a protocol error, not something to cut. */
    public String readString(int maxChars) throws ProtocolException {
        require(2);
        int length = (data[position] & 0xFF) << 8 | (data[position + 1] & 0xFF);
        position += 2;
        // One UTF-16 unit is at most 3 UTF-8 bytes (a surrogate pair: 2 units, 4 bytes).
        if (length > maxChars * 3) {
            throw new ProtocolException("string of " + length + " bytes exceeds " + maxChars + " chars");
        }
        require(length);
        String value = new String(data, position, length, StandardCharsets.UTF_8);
        position += length;
        if (value.length() > maxChars) {
            throw new ProtocolException("string of " + value.length() + " chars exceeds " + maxChars);
        }
        return value;
    }

    private void require(int bytes) throws ProtocolException {
        if (bytes < 0 || remaining() < bytes) {
            throw new ProtocolException("message ends " + (bytes - remaining()) + " bytes early");
        }
    }
}
