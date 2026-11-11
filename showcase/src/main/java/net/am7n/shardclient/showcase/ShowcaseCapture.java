package net.am7n.shardclient.showcase;

import net.am7n.shardclient.shared.render.MinecraftLook;
import net.am7n.shardclient.shared.render.SemiVanillaLook;
import net.am7n.shardclient.shared.render.ShardPvpLook;
import net.am7n.shardclient.shared.render.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.practice.MatchInfo;
import net.am7n.shardclient.shared.practice.ui.MatchResultScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a fixed script on the showcase screen and saves a screenshot at each step, then quits the game: the
 * entrance half-way, the settled screen with keyboard focus, two other GUI scales, and the exit half-way.
 */
final class ShowcaseCapture {

    private static final int KEY_TAB = 258;

    private record Step(long atMs, Runnable action) {
    }

    private final ShowcaseScreen screen;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final List<Step> steps = new ArrayList<>();
    private final int originalScale = minecraft.options.guiScale().get();
    private long startNanos;
    private int next;

    ShowcaseCapture(ShowcaseScreen screen) {
        this.screen = screen;
        long t = 0;
        steps.add(new Step(t += 60, () -> shot("1-entering")));
        steps.add(new Step(t += 700, () -> screen.keyPressed(new KeyEvent(KEY_TAB, 0, 0))));
        steps.add(new Step(t += 400, () -> shot("2-settled-focus")));
        steps.add(new Step(t += 50, () -> scale(1)));
        steps.add(new Step(t += 400, () -> shot("3-gui-scale-1")));
        steps.add(new Step(t += 50, () -> scale(3)));
        steps.add(new Step(t += 400, () -> shot("4-gui-scale-3")));
        steps.add(new Step(t += 50, () -> scale(originalScale)));
        steps.add(new Step(t += 300, () -> Theme.use(ShardPvpLook.LIGHT)));
        steps.add(new Step(t += 300, () -> shot("6-shardpvp-light")));
        steps.add(new Step(t += 50, () -> Theme.use(SemiVanillaLook.INSTANCE)));
        steps.add(new Step(t += 300, () -> shot("7-semi-vanilla")));
        steps.add(new Step(t += 50, () -> Theme.use(MinecraftLook.INSTANCE)));
        steps.add(new Step(t += 300, () -> shot("8-minecraft")));
        steps.add(new Step(t += 50, () -> Theme.use(ShardPvpLook.DARK)));
        steps.add(new Step(t += 300, screen::previewOverlays));
        steps.add(new Step(t += 120, () -> shot("9-overlays-entering")));
        steps.add(new Step(t += 700, () -> shot("10-overlays-dark")));
        steps.add(new Step(t += 50, () -> Theme.use(ShardPvpLook.LIGHT)));
        steps.add(new Step(t += 300, () -> shot("11-overlays-light")));
        steps.add(new Step(t += 50, () -> Theme.use(MinecraftLook.INSTANCE)));
        steps.add(new Step(t += 300, () -> shot("12-overlays-minecraft")));
        steps.add(new Step(t += 50, () -> Theme.use(ShardPvpLook.DARK)));
        steps.add(new Step(t += 100, screen::previewResult));
        steps.add(new Step(t += 450, () -> shot("13-result-counting")));
        steps.add(new Step(t += 900, () -> shot("14-result")));
        steps.add(new Step(t += 50, screen::previewTransition));
        steps.add(new Step(t += 90, () -> shot("15-transition-covering")));
        steps.add(new Step(t += 200, () -> shot("16-transition-covered")));
        steps.add(new Step(t += 50, screen::previewReveal));
        steps.add(new Step(t += 400, () -> shot("17-transition-revealing")));
        steps.add(new Step(t += 600, () -> shot("18-round-banner")));
        steps.add(new Step(t += 400, screen::onClose));
        steps.add(new Step(t += 400, () -> minecraft.gui.setScreen(new MatchResultScreen(null,
                new MatchEnded(MatchEnded.Result.WIN, 2, 1, 1172, 1214, "Gold I", 1100, 1200, "Gold II", 1200, 1300),
                new MatchInfo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), "Alex_02", "Sword",
                        "", "Badlands", true, "Gold I"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "Steve_01"))));
        steps.add(new Step(t += 450, () -> shot("19-result-screen-rising")));
        steps.add(new Step(t += 1200, () -> shot("20-bar-filling")));
        steps.add(new Step(t += 600, () -> shot("21-bar-rank-up")));
        steps.add(new Step(t += 1600, () -> shot("22-result-screen")));
        steps.add(new Step(t += 600, minecraft::stop));
        // The screen stops drawing once it has closed, so the tick keeps the script going to the end.
        ClientTickEvents.END_CLIENT_TICK.register(client -> frame());
    }

    /** Called once per rendered frame. */
    void frame() {
        if (minecraft.gui.overlay() != null) {
            return; // the loading overlay is still fading out over the screen
        }
        long now = System.nanoTime();
        if (startNanos == 0) {
            startNanos = now;
        }
        long elapsedMs = (now - startNanos) / 1_000_000L;
        while (next < steps.size() && elapsedMs >= steps.get(next).atMs()) {
            steps.get(next++).action().run();
        }
    }

    private void shot(String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1, message -> { });
    }

    private void scale(int guiScale) {
        minecraft.options.guiScale().set(guiScale);
        minecraft.resizeGui();
    }
}
