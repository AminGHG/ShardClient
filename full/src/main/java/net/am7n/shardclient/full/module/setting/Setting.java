package net.am7n.shardclient.full.module.setting;

import com.google.gson.JsonElement;

/**
 * One configurable value of a module. It knows its default, how to store itself in the config file, and how to
 * read itself back, ignoring anything it does not understand (so a broken or outdated config never breaks it).
 */
public abstract class Setting<T> {

    private final String id;
    private final String name;
    private final T defaultValue;
    private T value;

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = sanitize(value);
    }

    public void reset() {
        this.value = defaultValue;
    }

    public T defaultValue() {
        return defaultValue;
    }

    /** The value as it may be stored; out-of-range values are brought back in range. */
    protected T sanitize(T value) {
        return value == null ? defaultValue : value;
    }

    public abstract JsonElement toJson();

    /** Reads a stored value; keeps the current one if {@code json} is missing or not understood. */
    public abstract void fromJson(JsonElement json);
}
