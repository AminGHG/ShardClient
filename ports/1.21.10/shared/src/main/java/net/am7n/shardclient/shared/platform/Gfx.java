package net.am7n.shardclient.shared.platform;

import net.minecraft.client.renderer.RenderPipelines;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.am7n.shardclient.shared.render.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class Gfx {
    private final GuiGraphics graphics;

    public Gfx(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public static Canvas canvas(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        return new Canvas(new Gfx(graphics), font, mouseX, mouseY);
    }

    public GuiGraphics raw() {
        return graphics;
    }

    public int width() {
        return graphics.guiWidth();
    }

    public int height() {
        return graphics.guiHeight();
    }

    public void push() {
        graphics.pose().pushMatrix();
    }

    public void pop() {
        graphics.pose().popMatrix();
    }

    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    public void scale(float x, float y) {
        graphics.pose().scale(x, y);
    }

    public void scissor(int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    public void unscissor() {
        graphics.disableScissor();
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        graphics.fill(x1, y1, x2, y2, argb);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        graphics.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public void outline(int x, int y, int width, int height, int argb) {
        graphics.submitOutline(x, y, width, height, argb);
    }

    public void blit(Asset texture, int x, int y, float u, float v, int width, int height, int uWidth, int vHeight,
                     int textureWidth, int textureHeight, int argb) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, Assets.id(texture), x, y, u, v, width, height, uWidth, vHeight,
                textureWidth, textureHeight, argb);
    }

    public void sprite(Asset sprite, int x, int y, int width, int height, int argb) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Assets.id(sprite), x, y, width, height, argb);
    }

    public void text(Font font, Component text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderFakeItem(stack, x, y);
    }

    public void face(UUID playerId, int x, int y, int size, int argb) {
        PlayerFaceRenderer.draw(graphics, skinOf(playerId), x, y, size, argb);
    }

    private static PlayerSkin skinOf(UUID playerId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(playerId);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(playerId);
    }

    public static void registerTexture(Asset asset, int width, int height, int[] argb) {
        Textures.register(asset, width, height, argb);
    }
}
