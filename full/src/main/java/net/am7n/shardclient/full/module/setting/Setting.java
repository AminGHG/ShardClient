package net.am7n.shardclient.full.module.setting;

import com.google.gson.JsonElement;

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

    protected T sanitize(T value) {
        return value == null ? defaultValue : value;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);
}
