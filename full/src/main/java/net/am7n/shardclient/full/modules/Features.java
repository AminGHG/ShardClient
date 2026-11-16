package net.am7n.shardclient.full.modules;

import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.modules.pvp.Crosshair;
import net.am7n.shardclient.full.modules.visual.Freelook;
import net.am7n.shardclient.full.modules.visual.Fullbright;
import net.am7n.shardclient.full.modules.visual.LowFire;
import net.am7n.shardclient.full.modules.visual.NoHurtCam;
import net.am7n.shardclient.full.modules.visual.OwnNametag;
import net.am7n.shardclient.full.modules.visual.WeatherChanger;
import net.am7n.shardclient.full.modules.visual.Zoom;
import net.am7n.shardclient.full.modules.utility.ChatTimestamps;

public final class Features {
    public static Fullbright fullbright;
    public static Zoom zoom;
    public static Freelook freelook;
    public static NoHurtCam noHurtCam;
    public static LowFire lowFire;
    public static WeatherChanger weather;
    public static OwnNametag ownNametag;
    public static ChatTimestamps chatTimestamps;
    public static Crosshair crosshair;
    public static net.am7n.shardclient.full.modules.lyrics.LyricsModule lyrics;
    public static net.am7n.shardclient.full.modules.visual.MotionBlur motionBlur;
    public static net.am7n.shardclient.full.modules.utility.ScreenshotCopy screenshotCopy;
    public static net.am7n.shardclient.full.modules.utility.Waypoints waypoints;

    private Features() {
    }

    public static boolean on(Module module) {
        return module != null && module.isEnabled();
    }
}
