package net.am7n.shardclient.full.platform;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

public final class Servers {

    public enum Status {
        INITIAL, PINGING, UNREACHABLE, INCOMPATIBLE, SUCCESSFUL
    }

    public static Status status(ServerData server) {
        return Status.valueOf(server.state().name());
    }

    public static void setStatus(ServerData server, Status status) {
        server.setState(ServerData.State.valueOf(status.name()));
    }

    private Servers() {
    }

    public static void inBackground(Runnable task) {
        Util.backgroundExecutor().execute(task);
    }

    public static void ping(ServerStatusPinger pinger, ServerData server, Runnable changed) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        pinger.pingServer(server, () -> mc.execute(changed),
                () -> server.setState(server.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                        ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE),
                EventLoopGroupHolder.remote(mc.options.useNativeTransport()));
    }

    public static void connect(Screen parent, ServerData server) {
        ConnectScreen.startConnecting(parent, Minecraft.getInstance(), ServerAddress.parseString(server.ip), server, false, null);
    }

    public static ServerData create(String name, String address) {
        return new ServerData(name, address, ServerData.Type.OTHER);
    }

    public static byte[] iconBytes(ServerData server) {
        return server.getIconBytes();
    }

    public static int[] players(ServerData server) {
        return server.players == null ? null : new int[]{server.players.online(), server.players.max()};
    }

    public static String dimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : mc.level.dimension().identifier().toString();
    }
}
