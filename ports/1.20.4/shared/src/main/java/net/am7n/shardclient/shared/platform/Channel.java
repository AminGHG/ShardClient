package net.am7n.shardclient.shared.platform;

import net.am7n.shardclient.protocol.Protocol;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class Channel {
    private static final ResourceLocation ID = Assets.id(Asset.of(Protocol.NAMESPACE, Protocol.CHANNEL_PATH));

    private Channel() {
    }

    public static void register(Consumer<byte[]> receiver) {
        ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buffer, sender) -> {
            byte[] data = read(buffer);
            client.execute(() -> receiver.accept(data));
        });
    }

    private static byte[] read(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        if (length > Protocol.MAX_CLIENTBOUND_BYTES) {
            buffer.skipBytes(length);
            return new byte[0];
        }
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return data;
    }

    public static void onOffered(Runnable action) {
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ID)) {
                Minecraft.getInstance().execute(action);
            }
        });
    }

    public static boolean offered() {
        return ClientPlayNetworking.canSend(ID);
    }

    public static void send(byte[] data) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeBytes(data);
        ClientPlayNetworking.send(ID, buffer);
    }
}
