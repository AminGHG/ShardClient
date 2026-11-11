package net.am7n.shardclient.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/** Builds a message body. Big-endian, strings as {@code [byte length: 2 bytes][UTF-8]}. */
public final class PacketWriter {

    private byte[] buffer = new byte[64];
    private int size;

    public PacketWriter writeByte(int value) {
        ensure(1);
        buffer[size++] = (byte) value;
        return this;
    }

    public PacketWriter writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    public PacketWriter writeInt(int value) {
        ensure(4);
        buffer[size++] = (byte) (value >>> 24);
        buffer[size++] = (byte) (value >>> 16);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
        return this;
    }

    public PacketWriter writeLong(long value) {
        writeInt((int) (value >>> 32));
        return writeInt((int) value);
    }

    public PacketWriter writeUuid(UUID value) {
        writeLong(value.getMostSignificantBits());
        return writeLong(value.getLeastSignificantBits());
    }

    /**
     * Writes {@code value}, cut to {@code maxChars} UTF-16 units (never through a surrogate pair). Cutting rather
     * than failing keeps an over-long name or message from ever breaking the sender.
     */
    public PacketWriter writeString(String value, int maxChars) {
        byte[] utf8 = truncate(value == null ? "" : value, maxChars).getBytes(StandardCharsets.UTF_8);
        ensure(2 + utf8.length);
        buffer[size++] = (byte) (utf8.length >>> 8);
        buffer[size++] = (byte) utf8.length;
        System.arraycopy(utf8, 0, buffer, size, utf8.length);
        size += utf8.length;
        return this;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    static String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        int end = maxChars;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private void ensure(int extra) {
        if (size + extra > buffer.length) {
            buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + extra));
        }
    }
}
