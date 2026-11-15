package net.am7n.shardclient.full.modules.visual;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;

public final class WeatherChanger extends Module {
    private final ChoiceSetting weather = choice("weather", "Weather", "Clear", "Clear", "Rain", "Thunder");

    public WeatherChanger() {
        super("weather", "Weather changer", "Clear skies, rain or thunder, just for you.", Category.VISUAL);
    }

    public float rain() {
        return weather.is("Clear") ? 0f : 1f;
    }

    public float thunder() {
        return weather.is("Thunder") ? 1f : 0f;
    }
}
