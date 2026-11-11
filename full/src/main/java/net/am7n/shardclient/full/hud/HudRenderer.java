package net.am7n.shardclient.full.hud;

import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Canvas;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Draws every enabled HUD module on Minecraft's HUD. A module that fails is switched off and logged. */
public final class HudRenderer {

    private final ModuleManager modules;

    public HudRenderer(ModuleManager modules) {
        this.modules = modules;
    }

    public void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("shardclient", "hud"), this::render);
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof HudEditorScreen || mc.player == null) {
            return;
        }
        Canvas canvas = new Canvas(graphics, mc.font, -1, -1);
        for (var module : modules.all()) {
            if (module.isEnabled() && !(module instanceof HudModule)) {
                try {
                    module.renderHud(canvas);
                } catch (RuntimeException ex) {
                    ShardClientRuntime.LOGGER.error("Module {} failed to draw and was switched off", module.id(), ex);
                    module.setEnabled(false);
                }
            }
        }
        for (HudModule module : modules.hud()) {
            if (!module.isEnabled() || !module.visible()) {
                continue;
            }
            try {
                module.render(canvas);
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.error("HUD module {} failed and was switched off", module.id(), ex);
                module.setEnabled(false);
            }
        }
    }
}
