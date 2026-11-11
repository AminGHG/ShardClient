package net.am7n.shardclient.full;

import com.mojang.blaze3d.platform.InputConstants;
import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.gui.ClickGuiScreen;
import net.am7n.shardclient.full.hud.HudRenderer;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.full.modules.Modules;
import net.am7n.shardclient.protocol.ClientKind;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Entrypoint of the Full Client: the shared Practice Server integration plus the module system and menus. */
public final class ShardClientFull implements ClientModInitializer {

    public static final String MOD_ID = "shardclient";

    private static ShardClientFull instance;

    private ShardClientRuntime runtime;
    private final ModuleManager modules = new ModuleManager();
    private ClientConfig config;
    private boolean menuKeyWasDown;

    public static ShardClientFull get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        runtime = ShardClientRuntime.start(ClientKind.FULL, MOD_ID);
        Modules.registerAll(modules, runtime);
        config = new ClientConfig(modules);
        config.load();
        new HudRenderer(modules).register();
        Modules.registerHooks(modules);
        net.am7n.shardclient.full.menu.MenuScreens.registerVanillaToggle();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());
    }

    private void tick(Minecraft mc) {
        modules.tick(mc);
        boolean down = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_RSHIFT);
        if (down && !menuKeyWasDown && mc.gui.screen() == null) {
            openMenu(mc);
        }
        menuKeyWasDown = down;
    }

    public void openMenu(Minecraft mc) {
        mc.gui.setScreen(new ClickGuiScreen(mc.gui.screen(), modules, config, runtime.version()));
    }

    public ModuleManager modules() {
        return modules;
    }

    public ClientConfig config() {
        return config;
    }

    public ShardClientRuntime runtime() {
        return runtime;
    }
}
