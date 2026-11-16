package net.am7n.shardclient.full.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final T defaultValue;
    private T value;
    private BooleanSupplier shown = () -> true;
    private String heading;

    public void showIf(BooleanSupplier condition) {
        this.shown = condition;
    }

    public boolean shown() {
        return shown.getAsBoolean();
    }

    public void heading(String heading) {
        this.heading = heading;
    }

    public String heading() {
        return heading;
    }

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
