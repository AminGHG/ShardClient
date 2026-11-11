package net.am7n.shardclient.full.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A number between {@code min} and {@code max}, snapped to {@code step}; shown as a slider. */
public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final String unit;

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step, String unit) {
        super(id, name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public float floatValue() {
        return get().floatValue();
    }

    public int intValue() {
        return (int) Math.round(get());
    }

    /** The value as text, with as many decimals as the step needs. */
    public String display() {
        double value = get();
        String number = step >= 1 ? Long.toString(Math.round(value)) : String.format("%.2f", value)
                .replaceAll("0+$", "").replaceAll("\\.$", "");
        return number + unit;
    }

    @Override
    protected Double sanitize(Double value) {
        if (value == null || value.isNaN()) {
            return defaultValue();
        }
        double snapped = step > 0 ? min + Math.round((value - min) / step) * step : value;
        return Math.clamp(snapped, min, max);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            set(json.getAsDouble());
        }
    }
}
