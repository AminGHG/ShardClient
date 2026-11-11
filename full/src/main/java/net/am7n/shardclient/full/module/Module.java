package net.am7n.shardclient.full.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A feature of the Full Client the player can switch on and off: name, description, category, settings, an
 * optional keybind and enable/disable callbacks. Everything here is client-side; no module needs the server.
 */
public abstract class Module {

    /** No key bound. */
    public static final int NO_KEY = -1;

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = NO_KEY;

    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int key() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Switches the module, running its callbacks. A failing callback leaves it off and is logged. */
    public void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        try {
            if (value) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.error("Module {} failed to {}", id, value ? "enable" : "disable", ex);
            enabled = false;
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Every client tick while enabled. */
    public void tick() {
    }

    /** Draws on the HUD every frame while enabled (overlays such as a crosshair or a warning). */
    public void renderHud(net.am7n.shardclient.shared.render.Canvas canvas) {
    }

    protected BooleanSetting bool(String id, String name, boolean value) {
        return add(new BooleanSetting(id, name, value));
    }

    protected NumberSetting number(String id, String name, double value, double min, double max, double step,
                                   String unit) {
        return add(new NumberSetting(id, name, value, min, max, step, unit));
    }

    protected ChoiceSetting choice(String id, String name, String value, String... options) {
        return add(new ChoiceSetting(id, name, value, List.of(options)));
    }

    private <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("key", key);
        JsonObject values = new JsonObject();
        for (Setting<?> setting : settings) {
            values.add(setting.id(), setting.toJson());
        }
        json.add("settings", values);
        return json;
    }

    /** Applies stored values; anything missing or malformed keeps its default. */
    public void fromJson(JsonObject json) {
        JsonElement keyJson = json.get("key");
        if (keyJson != null && keyJson.isJsonPrimitive() && keyJson.getAsJsonPrimitive().isNumber()) {
            key = keyJson.getAsInt();
        }
        if (json.get("settings") instanceof JsonObject values) {
            for (Setting<?> setting : settings) {
                setting.fromJson(values.get(setting.id()));
            }
        }
        JsonElement enabledJson = json.get("enabled");
        if (enabledJson != null && enabledJson.isJsonPrimitive() && enabledJson.getAsJsonPrimitive().isBoolean()) {
            setEnabled(enabledJson.getAsBoolean());
        }
    }
}
