package net.am7n.shardclient.full.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** One of a fixed list of named options; shown as a button that cycles through them. */
public final class ChoiceSetting extends Setting<String> {

    private final List<String> options;

    public ChoiceSetting(String id, String name, String defaultValue, List<String> options) {
        super(id, name, defaultValue);
        this.options = List.copyOf(options);
    }

    public List<String> options() {
        return options;
    }

    public boolean is(String option) {
        return get().equals(option);
    }

    public int index() {
        return Math.max(0, options.indexOf(get()));
    }

    public void cycle(int direction) {
        set(options.get(Math.floorMod(index() + direction, options.size())));
    }

    @Override
    protected String sanitize(String value) {
        return value != null && options.contains(value) ? value : defaultValue();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            set(json.getAsString());
        }
    }
}
