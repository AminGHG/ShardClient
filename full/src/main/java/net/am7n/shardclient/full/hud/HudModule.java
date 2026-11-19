package net.am7n.shardclient.full.hud;

import net.am7n.shardclient.shared.anim.Maths;
import com.google.gson.JsonObject;
import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.render.Look;
import net.am7n.shardclient.shared.render.Theme;
import net.am7n.shardclient.shared.ui.Size;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public abstract class HudModule extends Module {
    protected static final int PADDING = 4;

    private float anchorX;
    private float anchorY;
    protected final NumberSetting scale = number("scale", "Scale", 1, 0.5, 2, 0.05, "×");
    protected final BooleanSetting background = bool("background", "Background", true);

    protected HudModule(String id, String name, String description, float anchorX, float anchorY) {
        super(id, name, description, Category.HUD);
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public abstract Size size(Canvas canvas);

    public abstract void draw(Canvas canvas, int x, int y);

    public boolean visible() {
        return true;
    }

    public float scaleFactor() {
        return scale.floatValue();
    }

    public int x(int screenWidth, int width) {
        return Math.round(anchorX * Math.max(0, screenWidth - width * scaleFactor()));
    }

    public int y(int screenHeight, int height) {
        return Math.round(anchorY * Math.max(0, screenHeight - height * scaleFactor()));
    }

    public void moveTo(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        float freeX = screenWidth - width * scaleFactor();
        float freeY = screenHeight - height * scaleFactor();
        anchorX = freeX <= 0 ? 0 : Maths.clamp(x / freeX, 0f, 1f);
        anchorY = freeY <= 0 ? 0 : Maths.clamp(y / freeY, 0f, 1f);
    }

    public void render(Canvas canvas) {
        Size size = size(canvas);
        renderAt(canvas, x(canvas.width(), size.width()), y(canvas.height(), size.height()));
    }

    public void renderAt(Canvas canvas, int x, int y) {
        canvas.push();
        canvas.translate(x, y);
        canvas.scale(scaleFactor(), 0, 0);
        draw(canvas, 0, 0);
        canvas.pop();
    }

    protected Size textSize(Canvas canvas, Component text) {
        return new Size(canvas.textWidth(text) + PADDING * 2, canvas.lineHeight() - 1 + PADDING * 2);
    }

    protected void drawText(Canvas canvas, int x, int y, Component text) {
        Look look = Theme.look();
        Size size = textSize(canvas, text);
        drawBackground(canvas, x, y, size.width(), size.height());
        canvas.text(text, x + PADDING, y + PADDING, look.colors().text(), look.textShadow() || !background.on());
    }

    protected void drawBackground(Canvas canvas, int x, int y, int width, int height) {
        if (background.on()) {
            Theme.look().shape(canvas, x, y, width, height, 4f, Theme.colors().surface());
        }
    }

    protected static Component labelled(String label, String value) {
        var colors = Theme.colors();
        return Theme.text(Component.literal(label + " ").withStyle(Style.EMPTY.withColor(colors.textMuted() & 0xFFFFFF))
                .append(Component.literal(value).withStyle(Style.EMPTY.withColor(colors.text() & 0xFFFFFF))), false);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("x", anchorX);
        json.addProperty("y", anchorY);
        return json;
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        if (json.has("x") && json.get("x").isJsonPrimitive()) {
            anchorX = Maths.clamp(json.get("x").getAsFloat(), 0f, 1f);
        }
        if (json.has("y") && json.get("y").isJsonPrimitive()) {
            anchorY = Maths.clamp(json.get("y").getAsFloat(), 0f, 1f);
        }
    }
}
