package net.am7n.shardclient.shared.platform;

import net.am7n.shardclient.shared.net.ShardPayload;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class Channel {
    private Channel() {
    }

    public static void register(Consumer<byte[]> receiver) {
        PayloadTypeRegistry.playS2C().register(ShardPayload.TYPE, ShardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ShardPayload.TYPE, ShardPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ShardPayload.TYPE,
                (payload, context) -> context.client().execute(() -> receiver.accept(payload.data())));
    }

    public static void onOffered(Runnable action) {
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(ShardPayload.TYPE.id())) {
                Minecraft.getInstance().execute(action);
            }
        });
    }

    public static boolean offered() {
        return ClientPlayNetworking.canSend(ShardPayload.TYPE);
    }

    public static void send(byte[] data) {
        ClientPlayNetworking.send(new ShardPayload(data));
    }
}
