package net.am7n.shardclient.showcase;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.menu.MenuScreens;
import net.am7n.shardclient.full.menu.ShardTitleScreen;
import net.am7n.shardclient.shared.render.SemiVanillaLook;
import net.am7n.shardclient.shared.render.ShardPvpLook;
import net.am7n.shardclient.shared.render.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.ArrayList;
import java.util.List;

final class MenuCapture {
    private record Step(long atMs, Runnable action) {
    }

    private MenuCapture() {
    }

    static void register() {
        Minecraft mc = Minecraft.getInstance();
        List<Step> steps = new ArrayList<>();
        long[] t = {1200};
        java.util.function.BiConsumer<Runnable, String> at = (action, shot) -> {
            steps.add(new Step(t[0], action));
            steps.add(new Step(t[0] + 1600, () -> shot(mc, shot)));
            t[0] += 1900;
        };
        for (int i = 0; i < 4; i++) {
            steps.add(new Step(400 + i * 150L, net.am7n.shardclient.shared.practice.ui.UiSounds::hover));
        }

        at.accept(() -> Theme.use(ShardPvpLook.LIGHT), "l-title");
        at.accept(() -> mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen())), "l-servers");
        at.accept(() -> mc.gui.setScreen(new SelectWorldScreen(new ShardTitleScreen())), "l-worlds");
        at.accept(() -> {
            mc.gui.setScreen(new ShardTitleScreen());
            ShardClientFull.get().openMenu(mc);
        }, "l-clientmenu");

        at.accept(() -> {
            var screen = mc.gui.screen();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.width / 2 + 133,
                    screen.height / 2 - 90, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
            screen.charTyped(new net.minecraft.client.input.CharacterEvent('c'));
            screen.charTyped(new net.minecraft.client.input.CharacterEvent('o'));
        }, "l-search");
        at.accept(() -> mc.gui.setScreen(new ShardTitleScreen()), "l-title2");
        at.accept(() -> {
            Theme.use(SemiVanillaLook.LIGHT);
            mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen()));
        }, "l-servers-semi");
        at.accept(() -> Theme.use(ShardPvpLook.DARK), "v-title");
        at.accept(() -> Theme.use(SemiVanillaLook.INSTANCE), "v-title-semi");
        at.accept(() -> mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen())), "v-servers-semi");
        at.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new JoinMultiplayerScreen(new ShardTitleScreen()));
        }, "v-servers");
        at.accept(() -> mc.gui.setScreen(new SelectWorldScreen(new ShardTitleScreen())), "v-worlds");
        at.accept(() -> {
            Theme.use(SemiVanillaLook.INSTANCE);
            mc.gui.setScreen(new SelectWorldScreen(new ShardTitleScreen()));
        }, "v-worlds-semi");
        at.accept(() -> {
            Theme.use(ShardPvpLook.DARK);
            mc.gui.setScreen(new ShardTitleScreen());
            ShardClientFull.get().openMenu(mc);
        }, "v-clientmenu");
        at.accept(() -> mc.gui.setScreen(new BadgePreviewScreen()), "v-badge");
        at.accept(MenuScreens::toggle, "v-vanilla-title");
        steps.add(new Step(t[0], MenuScreens::toggle));

        t[0] += 600;
        steps.add(new Step(t[0], () -> {
            mc.options.pauseOnLostFocus = false;
            ShardClientFull.get().modules().all().forEach(module -> module.setEnabled(true));
            mc.createWorldOpenFlows().openWorld("Demo_World", () -> { });
        }));
        steps.add(new Step(t[0] + 39_000, () -> mc.gui.setScreen(null)));
        steps.add(new Step(t[0] + 40_000, () -> shot(mc, "v-hud")));
        steps.add(new Step(t[0] + 40_050, () -> Theme.use(ShardPvpLook.LIGHT)));
        steps.add(new Step(t[0] + 40_150, () -> shot(mc, "l-hud")));
        steps.add(new Step(t[0] + 40_180, () -> Theme.use(ShardPvpLook.DARK)));

        steps.add(new Step(t[0] + 40_190, () -> {
            mc.options.guiScale().set(4);
            mc.resizeGui();
        }));
        steps.add(new Step(t[0] + 40_700, () -> shot(mc, "v-hud-small")));
        steps.add(new Step(t[0] + 40_800, () -> {
            mc.options.guiScale().set(0);
            mc.resizeGui();
        }));

        steps.add(new Step(t[0] + 41_000, () -> net.am7n.shardclient.full.modules.Features.lyrics.preset.set("Starry Sky")));
        steps.add(new Step(t[0] + 41_400, () -> shot(mc, "v-lyrics-sky")));
        steps.add(new Step(t[0] + 41_500, () -> net.am7n.shardclient.full.modules.Features.lyrics.place.set("Around you")));
        steps.add(new Step(t[0] + 41_900, () -> shot(mc, "v-lyrics-around")));
        steps.add(new Step(t[0] + 42_000, () -> net.am7n.shardclient.full.modules.Features.lyrics.place.set("In front")));
        steps.add(new Step(t[0] + 42_400, () -> shot(mc, "v-lyrics-front")));

        steps.add(new Step(t[0] + 42_500, () -> {
            var look = mc.player.getLookAngle();
            mc.player.setPos(mc.player.getX() - look.z * 3, mc.player.getY(), mc.player.getZ() + look.x * 3);
        }));
        steps.add(new Step(t[0] + 42_900, () -> shot(mc, "v-lyrics-angle")));

        steps.add(new Step(t[0] + 43_000, () -> {
            net.am7n.shardclient.full.modules.Features.fullbright.setEnabled(false);
            ShardClientFull.get().modules().all().forEach(module -> {
                if (module instanceof net.am7n.shardclient.full.hud.HudModule) {
                    module.setEnabled(false);
                }
            });
        }));
        steps.add(new Step(t[0] + 43_500, () -> shot(mc, "v-nofullbright")));
        steps.add(new Step(t[0] + 43_700, () -> {
            ShardClientFull.get().modules().all().forEach(module -> module.setEnabled(false));
            mc.stop();
        }));

        long[] start = {0};
        int[] next = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (start[0] == 0) {
                if (client.gui.overlay() == null && client.gui.screen() instanceof ShardTitleScreen) {
                    start[0] = System.currentTimeMillis();
                }
                return;
            }
            long elapsed = System.currentTimeMillis() - start[0];
            while (next[0] < steps.size() && elapsed >= steps.get(next[0]).atMs()) {
                steps.get(next[0]++).action().run();
            }
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> { });
    }
}
