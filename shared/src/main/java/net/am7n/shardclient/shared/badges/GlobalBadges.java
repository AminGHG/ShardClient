package net.am7n.shardclient.shared.badges;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalBadges {
    private static final String API = System.getProperty("shardclient.api", "https://shardpvp.xyz/api/client");
    private static final long HEARTBEAT_MS = 60_000;
    private static final long LOOKUP_EVERY_MS = 5_000;

    private static final long ANSWER_TTL_MS = 5 * 60_000;
    private static final int MAX_LOOKUP = 200;
    private static final long RETRY_MIN_MS = 30_000;
    private static final long RETRY_MAX_MS = 30 * 60_000;

    private static final Map<UUID, Answer> ANSWERS = new ConcurrentHashMap<>();
    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ShardClient-Badges");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final AtomicBoolean BUSY = new AtomicBoolean();

    private static volatile boolean enabled = true;
    private static volatile String token;
    private static volatile UUID tokenOwner;
    private static volatile long retryAt;
    private static volatile long retryDelay = RETRY_MIN_MS;
    private static long lastHeartbeat;
    private static long lastLookup;

    private record Answer(boolean user, long at) {
    }

    private GlobalBadges() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GlobalBadges::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ANSWERS.clear();
            USERS.clear();
        });
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            ANSWERS.clear();
            USERS.clear();
            token = null;
            tokenOwner = null;
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean has(UUID player) {
        return enabled && player != null && USERS.contains(player);
    }

    private static void tick(Minecraft client) {
        if (!enabled) {
            return;
        }
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.isLocalServer()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < retryAt || !BUSY.compareAndSet(false, true)) {
            return;
        }
        User user = client.getUser();

        List<UUID> ask = new ArrayList<>();
        boolean doLookup = now - lastLookup >= LOOKUP_EVERY_MS;
        if (doLookup) {
            lastLookup = now;
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                UUID id = info.getProfile().id();
                Answer answer = ANSWERS.get(id);
                if ((answer == null || now - answer.at() > ANSWER_TTL_MS) && ask.size() < MAX_LOOKUP) {
                    ask.add(id);
                }
            }
        }
        boolean doHeartbeat = now - lastHeartbeat >= HEARTBEAT_MS;
        if (doHeartbeat) {
            lastHeartbeat = now;
        }
        if (ask.isEmpty() && !doHeartbeat && token != null && user.getProfileId().equals(tokenOwner)) {
            BUSY.set(false);
            return;
        }
        WORKER.execute(() -> {
            try {
                work(client, user, ask, doHeartbeat);
                retryDelay = RETRY_MIN_MS;
            } catch (Exception ex) {
                retryAt = System.currentTimeMillis() + retryDelay;
                retryDelay = Math.min(RETRY_MAX_MS, retryDelay * 2);
                ShardClientRuntime.LOGGER.debug("Badges unavailable for now: {}", ex.toString());
            } finally {
                BUSY.set(false);
            }
        });
    }

    private static void work(Minecraft client, User user, List<UUID> ask, boolean heartbeat) throws Exception {
        if (token == null || !user.getProfileId().equals(tokenOwner)) {
            signIn(client, user);
            USERS.add(user.getProfileId());
        } else if (heartbeat) {
            if (post("/presence", new JsonObject(), true) == null) {
                return;
            }
        }
        if (ask.isEmpty()) {
            return;
        }
        JsonObject body = new JsonObject();
        JsonArray uuids = new JsonArray();
        ask.forEach(id -> uuids.add(id.toString()));
        body.add("uuids", uuids);
        JsonObject answer = post("/lookup", body, true);
        if (answer == null) {
            return;
        }
        Set<UUID> yes = new java.util.HashSet<>();
        if (answer.get("users") instanceof JsonArray users) {
            for (JsonElement element : users) {
                try {
                    yes.add(UUID.fromString(element.getAsString()));
                } catch (RuntimeException ignored) {
                }
            }
        }
        long now = System.currentTimeMillis();
        for (UUID id : ask) {
            boolean isUser = yes.contains(id) || id.equals(user.getProfileId());
            ANSWERS.put(id, new Answer(isUser, now));
            if (isUser) {
                USERS.add(id);
            } else {
                USERS.remove(id);
            }
        }
    }

    private static void signIn(Minecraft client, User user) throws Exception {
        token = null;
        JsonObject challenge = post("/auth/challenge", new JsonObject(), false);
        if (challenge == null || !(challenge.get("serverId") instanceof JsonElement id) || !id.isJsonPrimitive()) {
            throw new IllegalStateException("no challenge");
        }
        String serverId = id.getAsString();
        if (!serverId.matches("[0-9a-zA-Z-]{1,64}")) {
            throw new IllegalStateException("bad challenge");
        }
        try {
            client.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
        } catch (AuthenticationException ex) {
            throw new IllegalStateException("Mojang refused the sign-in (offline account?)", ex);
        }
        ShardClientRuntime runtime = ShardClientRuntime.get();
        JsonObject verify = new JsonObject();
        verify.addProperty("name", user.getName());
        verify.addProperty("serverId", serverId);
        verify.addProperty("kind", runtime.kind() == ClientKind.FULL ? "FULL" : "LIGHT");
        verify.addProperty("version", runtime.version());
        JsonObject session = post("/auth/verify", verify, false);
        if (session == null || !(session.get("token") instanceof JsonElement value) || !value.isJsonPrimitive()) {
            throw new IllegalStateException("sign-in refused");
        }
        token = value.getAsString();
        tokenOwner = user.getProfileId();
        lastHeartbeat = System.currentTimeMillis();
    }

    private static JsonObject post(String path, JsonObject body, boolean authorized) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(API + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "ShardClient/" + ShardClientRuntime.get().version())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (authorized) {
            String current = token;
            if (current == null) {
                return null;
            }
            request.header("Authorization", "Bearer " + current);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            token = null;
            return null;
        }
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + path);
        }
        String text = response.body();
        if (text == null || text.isBlank()) {
            return new JsonObject();
        }
        JsonElement parsed = JsonParser.parseString(text);
        return parsed instanceof JsonObject object ? object : new JsonObject();
    }
}
