package net.am7n.shardclient.shared.net;

import net.am7n.shardclient.protocol.Protocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The raw bytes of one protocol message on the {@code shardclient:main} channel. What is inside is the business
 * of the protocol module, which the server uses too; Minecraft only carries it.
 */
public record ShardPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<ShardPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Protocol.NAMESPACE, Protocol.CHANNEL_PATH));

    public static final StreamCodec<FriendlyByteBuf, ShardPayload> CODEC =
            StreamCodec.of(ShardPayload::write, ShardPayload::read);

    private static void write(FriendlyByteBuf buffer, ShardPayload payload) {
        buffer.writeBytes(payload.data);
    }

    private static ShardPayload read(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        if (length > Protocol.MAX_CLIENTBOUND_BYTES) {
            // Throwing here would disconnect the player. Swallow the message; the empty one is then ignored.
            buffer.skipBytes(length);
            return new ShardPayload(new byte[0]);
        }
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return new ShardPayload(data);
    }

    @Override
    public Type<ShardPayload> type() {
        return TYPE;
    }
}
