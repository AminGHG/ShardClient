package net.am7n.shardclient.shared.platform;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.ui.Key;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class ScreenBase extends Screen {
    protected ScreenBase(Component title) {
        super(title);
    }

    protected boolean blurBehind() {
        return false;
    }

    protected abstract void drawBackdrop(Canvas canvas);

    protected abstract void draw(Canvas canvas);

    protected abstract boolean onMouseClicked(double x, double y, int button);

    protected abstract boolean onMouseReleased(double x, double y, int button);

    protected abstract boolean onMouseDragged(double x, double y, int button);

    protected abstract boolean onMouseScrolled(double x, double y, double amount);

    protected abstract boolean onKey(Key key);

    protected abstract boolean onChar(String text);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            renderDirtBackground(graphics);
        }
        drawBackdrop(Gfx.canvas(graphics, font, mouseX, mouseY));
        super.render(graphics, mouseX, mouseY, partialTick);
        draw(Gfx.canvas(graphics, font, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return onMouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return onMouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean confirm = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE;
        return onKey(new Key(keyCode, keyCode == GLFW.GLFW_KEY_ESCAPE, keyCode == GLFW.GLFW_KEY_TAB,
                (modifiers & GLFW.GLFW_MOD_SHIFT) != 0, confirm));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return onChar(String.valueOf(codePoint));
    }
}
