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

/**
 * The Full Client's settings file, {@code config/shardclient/full.json}: client preferences, every module's
 * state, settings, keybind and HUD position. The Light Version keeps its own file and never reads this one.
 *
 * <p>A missing file means defaults. An unreadable one is kept aside as {@code full.json.broken} and replaced by
 * defaults. Unknown or malformed entries are ignored one by one, so an older or newer file still loads what it
 * can. Saving writes a temporary file and moves it into place, so a crash never leaves half a file.
 */
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

    public boolean uiSounds() {
        return uiSounds;
    }

    public void setUiSounds(boolean value) {
        this.uiSounds = value;
        UiSounds.setEnabled(value);
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
                // Nothing more to do; the next save replaces it.
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
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        JsonObject client = new JsonObject();
        client.addProperty("look", Theme.look().id());
        client.addProperty("menuStyle", menuStyle == MenuStyle.VANILLA ? "vanilla" : "shard");
        client.addProperty("uiSounds", uiSounds);
        json.add("client", client);
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
