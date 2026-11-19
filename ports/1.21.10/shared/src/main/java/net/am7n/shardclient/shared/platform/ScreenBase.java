package net.am7n.shardclient.shared.platform;

import net.am7n.shardclient.shared.render.Canvas;
import net.am7n.shardclient.shared.ui.Key;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            renderPanorama(graphics, partialTick);
        }
        if (blurBehind()) {
            renderBlurredBackground(graphics);
        }
        drawBackdrop(Gfx.canvas(graphics, font, mouseX, mouseY));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        draw(Gfx.canvas(graphics, font, mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return onMouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return onMouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return onMouseDragged(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return onKey(new Key(event.key(), event.isEscape(), event.isCycleFocus(), event.hasShiftDown(),
                event.isConfirmation() || event.isSelection()));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return onChar(event.codepointAsString());
    }
}
