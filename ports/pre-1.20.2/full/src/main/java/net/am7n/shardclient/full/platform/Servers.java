package net.am7n.shardclient.full.platform;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class Servers {
    public enum Status {
        INITIAL, PINGING, UNREACHABLE, INCOMPATIBLE, SUCCESSFUL
    }

    public static Status status(ServerData server) {
        if (!server.pinged) {
            return Status.INITIAL;
        }
        if (server.ping == -2) {
            return Status.PINGING;
        }
        if (server.ping < 0) {
            return Status.UNREACHABLE;
        }
        return server.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                ? Status.SUCCESSFUL : Status.INCOMPATIBLE;
    }

    public static void setStatus(ServerData server, Status status) {
        server.pinged = status != Status.INITIAL;
        if (status == Status.PINGING) {
            server.ping = -2;
        } else if (status == Status.UNREACHABLE) {
            server.ping = -1;
        }
    }

    private Servers() {
    }

    public static void inBackground(Runnable task) {
        Util.backgroundExecutor().execute(task);
    }

    public static void ping(ServerStatusPinger pinger, ServerData server, Runnable changed) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        pinger.pingServer(server, () -> mc.execute(changed));
    }

    public static void connect(Screen parent, ServerData server) {
        ConnectScreen.startConnecting(parent, Minecraft.getInstance(), ServerAddress.parseString(server.ip), server, false);
    }

    public static ServerData create(String name, String address) {
        return new ServerData(name, address, false);
    }

    public static byte[] iconBytes(ServerData server) {
        return server.getIconBytes();
    }

    public static int[] players(ServerData server) {
        return server.players == null ? null : new int[]{server.players.online(), server.players.max()};
    }

    public static String dimension() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : mc.level.dimension().location().toString();
    }
}
