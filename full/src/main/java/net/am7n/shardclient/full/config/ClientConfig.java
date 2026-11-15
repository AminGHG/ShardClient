package net.am7n.shardclient.full.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.practice.ui.UiSounds;
import net.am7n.shardclient.shared.render.Theme;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ClientConfig {
    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum MenuStyle {
        SHARD,
        VANILLA
    }

    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("shardclient").resolve("full.json");
    private final ModuleManager modules;

    private MenuStyle menuStyle = MenuStyle.SHARD;
    private boolean uiSounds = true;
    private boolean badgesEverywhere = true;
    private boolean hoverEffects = true;
    private boolean hoverSounds = true;

    public record RecentServer(String name, String address, long joinedAt) {
    }

    private static final int MAX_RECENT = 30;

    private final java.util.Map<String, RecentServer> recent = new java.util.HashMap<>();

    public ClientConfig(ModuleManager modules) {
        this.modules = modules;
    }

    public MenuStyle menuStyle() {
        return menuStyle;
    }

    public void setMenuStyle(MenuStyle style) {
        this.menuStyle = style;
        save();
    }

    public boolean badgesEverywhere() {
        return badgesEverywhere;
    }

    public void setBadgesEverywhere(boolean value) {
        this.badgesEverywhere = value;
        apply();
        save();
    }

    public boolean hoverEffects() {
        return hoverEffects;
    }

    public void setHoverEffects(boolean value) {
        this.hoverEffects = value;
        apply();
    }

    public boolean hoverSounds() {
        return hoverSounds;
    }

    public void setHoverSounds(boolean value) {
        this.hoverSounds = value;
        apply();
    }

    public boolean uiSounds() {
        return uiSounds;
    }

    public void setUiSounds(boolean value) {
        this.uiSounds = value;
        UiSounds.setEnabled(value);
    }

    public java.util.List<RecentServer> recentServers() {
        return recent.values().stream()
                .sorted(java.util.Comparator.comparingLong(RecentServer::joinedAt).reversed())
                .toList();
    }

    public void markJoined(String name, String address) {
        recent.put(address.toLowerCase(java.util.Locale.ROOT),
                new RecentServer(name, address, System.currentTimeMillis()));
        recentServers().stream().skip(MAX_RECENT)
                .forEach(old -> recent.remove(old.address().toLowerCase(java.util.Locale.ROOT)));
        save();
    }

    public void load() {
        if (!Files.isRegularFile(file)) {
            apply();
            return;
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException ex) {
            ShardClientRuntime.LOGGER.warn("Config {} is unreadable; keeping it as .broken and using defaults", file, ex);
            try {
                Files.move(file, file.resolveSibling("full.json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            apply();
            return;
        }

        if (json.get("client") instanceof JsonObject client) {
            if (string(client.get("look")) instanceof String look) {
                Theme.use(Theme.byId(look));
            }
            if (string(client.get("menuStyle")) instanceof String style) {
                menuStyle = "vanilla".equals(style) ? MenuStyle.VANILLA : MenuStyle.SHARD;
            }
            if (client.get("uiSounds") instanceof JsonElement sounds && sounds.isJsonPrimitive()
                    && sounds.getAsJsonPrimitive().isBoolean()) {
                uiSounds = sounds.getAsBoolean();
            }
            if (client.get("badgesEverywhere") instanceof JsonElement badges && badges.isJsonPrimitive()
                    && badges.getAsJsonPrimitive().isBoolean()) {
                badgesEverywhere = badges.getAsBoolean();
            }
            if (client.get("hoverEffects") instanceof JsonElement effects && effects.isJsonPrimitive()
                    && effects.getAsJsonPrimitive().isBoolean()) {
                hoverEffects = effects.getAsBoolean();
            }
            if (client.get("hoverSounds") instanceof JsonElement hoverSound && hoverSound.isJsonPrimitive()
                    && hoverSound.getAsJsonPrimitive().isBoolean()) {
                hoverSounds = hoverSound.getAsBoolean();
            }
        }
        if (json.get("servers") instanceof JsonObject servers) {
            if (servers.get("recent") instanceof com.google.gson.JsonArray list) {
                for (JsonElement entry : list) {
                    if (entry instanceof JsonObject server && string(server.get("address")) instanceof String address
                            && server.get("at") instanceof JsonElement at && at.isJsonPrimitive()
                            && at.getAsJsonPrimitive().isNumber()) {
                        String name = string(server.get("name"));
                        recent.put(address.toLowerCase(java.util.Locale.ROOT),
                                new RecentServer(name == null ? address : name, address, at.getAsLong()));
                    }
                }
            }
        }
        if (json.get("modules") instanceof JsonObject stored) {
            for (Module module : modules.all()) {
                if (stored.get(module.id()) instanceof JsonObject moduleJson) {
                    try {
                        module.fromJson(moduleJson);
                    } catch (RuntimeException ex) {
                        ShardClientRuntime.LOGGER.warn("Ignoring broken settings of module {}", module.id(), ex);
                    }
                }
            }
        }
        apply();
    }

    private void apply() {
        UiSounds.setEnabled(uiSounds);
        net.am7n.shardclient.shared.badges.GlobalBadges.setEnabled(badgesEverywhere);
        net.am7n.shardclient.shared.ui.Hover.setEffects(hoverEffects);
        UiSounds.setHoverEnabled(hoverSounds);
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        JsonObject client = new JsonObject();
        client.addProperty("look", Theme.look().id());
        client.addProperty("menuStyle", menuStyle == MenuStyle.VANILLA ? "vanilla" : "shard");
        client.addProperty("uiSounds", uiSounds);
        client.addProperty("badgesEverywhere", badgesEverywhere);
        client.addProperty("hoverEffects", hoverEffects);
        client.addProperty("hoverSounds", hoverSounds);
        json.add("client", client);
        JsonObject servers = new JsonObject();
        com.google.gson.JsonArray recentList = new com.google.gson.JsonArray();
        for (RecentServer server : recentServers()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", server.name());
            entry.addProperty("address", server.address());
            entry.addProperty("at", server.joinedAt());
            recentList.add(entry);
        }
        servers.add("recent", recentList);
        json.add("servers", servers);
        JsonObject stored = new JsonObject();
        for (Module module : modules.all()) {
            stored.add(module.id(), module.toJson());
        }
        json.add("modules", stored);

        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("full.json.tmp");
            Files.writeString(temp, GSON.toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            ShardClientRuntime.LOGGER.error("Could not save {}", file, ex);
        }
    }

    private static String string(JsonElement json) {
        return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString() ? json.getAsString() : null;
    }
}
