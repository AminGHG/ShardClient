package net.am7n.shardclient.full.hud;

import net.am7n.shardclient.shared.platform.Gfx;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.ui.Size;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    private static final int GAP = 2;
    private static final int MAX_MOVES = 24;

    private final ModuleManager modules;

    public HudRenderer(ModuleManager modules) {
        this.modules = modules;
    }

    public void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("shardclient", "hud"), this::render);
    }

    private record Box(HudModule module, int x, int y, int w, int h) {
        boolean overlaps(Box other) {
            return x < other.x + other.w + GAP && other.x < x + w + GAP
                    && y < other.y + other.h + GAP && other.y < y + h + GAP;
        }

        Box at(int nx, int ny) {
            return new Box(module, nx, ny, w, h);
        }
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof HudEditorScreen || mc.player == null) {
            return;
        }
        Canvas canvas = Gfx.canvas(graphics, mc.font, -1, -1);
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
        List<Box> placed = new ArrayList<>();
        for (HudModule module : modules.hud()) {
            try {
                if (!module.isEnabled() || !module.visible()) {
                    continue;
                }
                Box box = settle(place(canvas, module), placed, canvas.width(), canvas.height());
                placed.add(box);
                module.renderAt(canvas, box.x(), box.y());
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.error("HUD module {} failed and was switched off", module.id(), ex);
                module.setEnabled(false);
            }
        }
    }

    private static Box place(Canvas canvas, HudModule module) {
        Size size = module.size(canvas);
        int w = Math.round(size.width() * module.scaleFactor());
        int h = Math.round(size.height() * module.scaleFactor());
        return new Box(module, module.x(canvas.width(), size.width()), module.y(canvas.height(), size.height()), w, h);
    }

    private static Box settle(Box box, List<Box> placed, int screenWidth, int screenHeight) {
        boolean downward = box.y() + box.h() / 2 < screenHeight / 2;
        boolean rightward = box.x() + box.w() / 2 < screenWidth / 2;
        for (int move = 0; move < MAX_MOVES; move++) {
            Box hit = null;
            for (Box other : placed) {
                if (box.overlaps(other)) {
                    hit = other;
                    break;
                }
            }
            if (hit == null) {
                return box;
            }
            int ny = downward ? hit.y() + hit.h() + GAP : hit.y() - box.h() - GAP;
            if (ny >= 0 && ny + box.h() <= screenHeight) {
                box = box.at(box.x(), ny);
            } else {
                int nx = rightward ? hit.x() + hit.w() + GAP : hit.x() - box.w() - GAP;
                if (nx < 0 || nx + box.w() > screenWidth) {
                    return box;
                }
                box = box.at(nx, Math.clamp(box.y(), 0, Math.max(0, screenHeight - box.h())));
            }
        }
        return box;
    }
}
