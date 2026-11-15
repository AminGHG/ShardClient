package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;

abstract class TextHud extends HudModule {
    TextHud(String id, String name, String description, float anchorX, float anchorY) {
        super(id, name, description, anchorX, anchorY);
    }

    protected abstract Component text();

    @Override
    public Size size(Canvas canvas) {
        return textSize(canvas, text());
    }

    @Override
    public void draw(Canvas canvas, int x, int y) {
        drawText(canvas, x, y, text());
    }
}
