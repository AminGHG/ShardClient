package net.am7n.shardclient.full.modules;

import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.full.platform.Commands;
import net.am7n.shardclient.full.platform.HudHooks;
import net.am7n.shardclient.full.modules.hud.ArmorHud;
import net.am7n.shardclient.full.modules.hud.ClickCounter;
import net.am7n.shardclient.full.modules.hud.KeystrokesHud;
import net.am7n.shardclient.full.modules.hud.PotionHud;
import net.am7n.shardclient.full.modules.hud.SimpleHuds;
import net.am7n.shardclient.full.modules.pvp.CombatHuds;
import net.am7n.shardclient.full.modules.pvp.CombatTracker;
import net.am7n.shardclient.full.modules.pvp.Crosshair;
import net.am7n.shardclient.full.modules.pvp.LowHealthWarning;
import net.am7n.shardclient.full.modules.utility.AutoGG;
import net.am7n.shardclient.full.modules.utility.ChatTimestamps;
import net.am7n.shardclient.full.modules.utility.CopyCoordinates;
import net.am7n.shardclient.full.modules.utility.ToggleKeys;
import net.am7n.shardclient.full.modules.visual.Freelook;
import net.am7n.shardclient.full.modules.visual.Fullbright;
import net.am7n.shardclient.full.modules.visual.LowFire;
import net.am7n.shardclient.full.modules.visual.NoHurtCam;
import net.am7n.shardclient.full.modules.visual.OwnNametag;
import net.am7n.shardclient.full.modules.visual.WeatherChanger;
import net.am7n.shardclient.full.modules.visual.Zoom;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Modules {
    private Modules() {
    }

    public static void registerAll(ModuleManager modules, ShardClientRuntime runtime) {
        net.am7n.shardclient.full.platform.Glow.init();
        net.am7n.shardclient.full.platform.World.init();
        Keys.register();

        modules.register(Features.fullbright = new Fullbright());
        modules.register(Features.zoom = new Zoom());
        modules.register(Features.freelook = new Freelook());
        modules.register(Features.noHurtCam = new NoHurtCam());
        modules.register(Features.lowFire = new LowFire());
        modules.register(Features.weather = new WeatherChanger());
        modules.register(Features.ownNametag = new OwnNametag());
        modules.register(Features.motionBlur = new net.am7n.shardclient.full.modules.visual.MotionBlur());

        modules.register(new SimpleHuds.Fps());
        modules.register(new SimpleHuds.Ping());
        modules.register(new SimpleHuds.Cps());
        modules.register(new KeystrokesHud());
        modules.register(new SimpleHuds.Coordinates());
        modules.register(new SimpleHuds.Direction());
        modules.register(new ArmorHud());
        modules.register(new PotionHud());
        modules.register(new SimpleHuds.Speed());
        modules.register(new SimpleHuds.Clock());
        modules.register(new SimpleHuds.Session());
        modules.register(new SimpleHuds.Server());
        modules.register(new SimpleHuds.Memory());
        modules.register(Features.lyrics = new net.am7n.shardclient.full.modules.lyrics.LyricsModule());
        modules.register(new net.am7n.shardclient.full.modules.hud.NowPlayingHud());
        modules.register(new CombatHuds.Reach());
        modules.register(new CombatHuds.Combo());
        modules.register(new CombatHuds.TotemPops());
        modules.register(new CombatHuds.Target());
        modules.register(new CombatHuds.Cooldowns());

        modules.register(Features.crosshair = new Crosshair());
        modules.register(new LowHealthWarning());
        modules.register(Features.hitColor = new net.am7n.shardclient.full.modules.pvp.HitColor());
        modules.register(Features.totemPopChams = new net.am7n.shardclient.full.modules.pvp.TotemPopChams());

        modules.register(new ToggleKeys.Sprint());
        modules.register(new ToggleKeys.Sneak());
        modules.register(Features.chatTimestamps = new ChatTimestamps());
        modules.register(new AutoGG());
        modules.register(new CopyCoordinates());
        modules.register(Features.waypoints = new net.am7n.shardclient.full.modules.utility.Waypoints());
        modules.register(Features.screenshotCopy = new net.am7n.shardclient.full.modules.utility.ScreenshotCopy());
        modules.register(new net.am7n.shardclient.full.discord.DiscordPresence());
    }

    public static void registerHooks(ModuleManager modules) {
        CombatTracker.register();
        new net.am7n.shardclient.full.modules.lyrics.WorldLyrics(Features.lyrics).register();
        net.am7n.shardclient.full.platform.World.onRender(
                world -> Features.waypoints.render(world));
        net.am7n.shardclient.full.platform.World.onRender(
                world -> Features.totemPopChams.render(world));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SimpleHuds.Session.joined());

        Commands.number("motionblur", "percent", 0, 100, percent -> {
            Features.motionBlur.setAmountFromCommand(percent);
            return Component.literal("Motion Blur: " + percent + "%");
        });

        HudHooks.beforeHud("click_counter", () -> ClickCounter.sample(Minecraft.getInstance()));

        HudHooks.crosshair(canvas -> {
            Minecraft mc = Minecraft.getInstance();
            if (Features.on(Features.crosshair) && mc.options.getCameraType().isFirstPerson()
                    && mc.player != null && !mc.player.isSpectator()) {
                Features.crosshair.draw(canvas);
                return true;
            }
            return false;
        });
    }
}
