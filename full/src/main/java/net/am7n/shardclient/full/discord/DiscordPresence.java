package net.am7n.shardclient.full.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.practice.PracticeSession;
import net.am7n.shardclient.shared.practice.QueueState;
import net.am7n.shardclient.shared.practice.ShardPvpServers;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class DiscordPresence extends Module {
    public static final String APPLICATION_ID = "1464355170447261727";
    private static final String DOWNLOAD = "https://shardpvp.xyz/client";
    private static final long RETRY_MILLIS = 15_000;

    private final BooleanSetting showServer = bool("show_server", "Show the server address", false);
    private final BooleanSetting showMatch = bool("show_match", "Show ShardPvP queue and match", true);
    private final BooleanSetting showButton = bool("show_button", "Show a download button", true);

    private record Activity(String details, String state, long start) {
    }

    private static final Activity CLEAR = new Activity(null, null, 0);

    private final LinkedBlockingQueue<Activity> updates = new LinkedBlockingQueue<>();
    private Thread worker;
    private Activity current;
    private String context = "";
    private long contextStart;
    private int ticks;

    public DiscordPresence() {
        super("discord_presence", "Discord presence",
                "Shows what you are playing in your Discord status.", Category.UTILITY);
        setEnabled(true);
    }

    @Override
    protected void onEnable() {
        if (APPLICATION_ID.isEmpty() || worker != null) {
            return;
        }
        current = null;
        worker = new Thread(this::run, "ShardClient Discord");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    protected void onDisable() {
        Thread running = worker;
        worker = null;
        if (running != null) {
            updates.offer(CLEAR);
            running.interrupt();
        }
    }

    @Override
    public void tick() {
        if (worker == null || ticks++ % 40 != 0) {
            return;
        }
        Activity next = describe();
        if (!next.equals(current)) {
            current = next;
            updates.offer(next);
        }
    }

    private Activity describe() {
        String details;
        String state = null;
        String where;
        if (mc.level == null || mc.player == null) {
            details = "In the menus";
            where = "menu";
        } else if (mc.getSingleplayerServer() != null) {
            details = "Playing singleplayer";
            where = "singleplayer";
        } else if (ShardPvpServers.isCurrent(mc)) {
            details = "Playing on ShardPvP";
            where = "shardpvp";
            if (showMatch.on()) {
                state = practiceState();
            }
        } else {
            ServerData server = mc.getCurrentServer();
            details = "Playing multiplayer";
            where = "server:" + (server == null ? "" : server.ip);
            if (showServer.on() && server != null) {
                state = server.ip;
            }
        }
        if (!where.equals(context)) {
            context = where;
            contextStart = System.currentTimeMillis() / 1000;
        }
        return new Activity(details, state, contextStart);
    }

    private static String practiceState() {
        ShardClientRuntime runtime = ShardClientRuntime.get();
        if (runtime == null || runtime.practice() == null) {
            return null;
        }
        PracticeSession session = runtime.practice().session();
        MatchInfo match = session.match();
        if (match != null) {
            String mode = match.ranked() ? "Ranked" : "Unranked";
            String kit = match.kit() == null || match.kit().isBlank() ? match.mode() : match.kit();
            return mode + " " + kit + " vs " + match.opponentName();
        }
        QueueState queue = session.queue();
        if (queue != null) {
            return "In queue: " + queue.displayName();
        }
        return null;
    }

    private void run() {
        DiscordIpc ipc = null;
        Activity pending = null;
        long retryAt = 0;
        try {
            while (true) {
                Activity next = updates.poll(1, TimeUnit.SECONDS);
                if (next != null) {
                    pending = next;
                }
                if (pending == null) {
                    continue;
                }
                if (ipc == null) {
                    if (System.currentTimeMillis() < retryAt) {
                        continue;
                    }
                    try {
                        ipc = DiscordIpc.connect(APPLICATION_ID);
                        ShardClientRuntime.LOGGER.info("Connected to Discord");
                    } catch (IOException | RuntimeException ex) {
                        retryAt = System.currentTimeMillis() + RETRY_MILLIS;
                        continue;
                    }
                }
                try {
                    ipc.setActivity(pending == CLEAR ? null : json(pending));
                    if (pending == CLEAR) {
                        break;
                    }
                    pending = null;
                } catch (IOException | RuntimeException ex) {
                    ipc.close();
                    ipc = null;
                    retryAt = System.currentTimeMillis() + RETRY_MILLIS;
                }
            }
        } catch (InterruptedException ignored) {
            if (ipc != null) {
                try {
                    ipc.setActivity(null);
                } catch (IOException ignoredToo) {
                }
            }
        } finally {
            if (ipc != null) {
                ipc.close();
            }
        }
    }

    private JsonObject json(Activity activity) {
        JsonObject json = new JsonObject();
        json.addProperty("details", activity.details());
        if (activity.state() != null && !activity.state().isBlank()) {
            json.addProperty("state", activity.state());
        }
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", activity.start());
        json.add("timestamps", timestamps);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", "logo");
        ShardClientRuntime runtime = ShardClientRuntime.get();
        String version = runtime == null ? "" : " " + runtime.version();
        assets.addProperty("large_text", "ShardClient" + version);
        json.add("assets", assets);
        if (showButton.on()) {
            JsonObject button = new JsonObject();
            button.addProperty("label", "Get ShardClient");
            button.addProperty("url", DOWNLOAD);
            JsonArray buttons = new JsonArray();
            buttons.add(button);
            json.add("buttons", buttons);
        }
        return json;
    }
}
