package net.am7n.shardclient.shared.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.am7n.shardclient.shared.render.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class Gfx extends GuiComponent {
    private final PoseStack pose;

    public Gfx(PoseStack pose) {
        this.pose = pose;
    }

    public static Canvas canvas(PoseStack pose, Font font, int mouseX, int mouseY) {
        return new Canvas(new Gfx(pose), font, mouseX, mouseY);
    }

    public PoseStack raw() {
        return pose;
    }

    public int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public void push() {
        pose.pushPose();
    }

    public void pop() {
        pose.popPose();
    }

    public void translate(float x, float y) {
        pose.translate(x, y, 0);
    }

    public void scale(float x, float y) {
        pose.scale(x, y, 1);
    }

    public void scissor(int x1, int y1, int x2, int y2) {
        enableScissor(x1, y1, x2, y2);
    }

    public void unscissor() {
        disableScissor();
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        fill(pose, x1, y1, x2, y2, argb);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        fillGradient(pose, x1, y1, x2, y2, top, bottom);
    }

    public void outline(int x, int y, int width, int height, int argb) {
        fill(pose, x, y, x + width, y + 1, argb);
        fill(pose, x, y + height - 1, x + width, y + height, argb);
        fill(pose, x, y + 1, x + 1, y + height - 1, argb);
        fill(pose, x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    public void blit(Asset texture, int x, int y, float u, float v, int width, int height, int uWidth, int vHeight,
                     int textureWidth, int textureHeight, int argb) {
        RenderSystem.setShaderTexture(0, Assets.id(texture));
        tint(argb);
        blit(pose, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
        untint();
    }

    public void sprite(Asset sprite, int x, int y, int width, int height, int argb) {
    }

    public void text(Font font, Component text, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, argb);
        } else {
            font.draw(pose, text, x, y, argb);
        }
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, argb);
        } else {
            font.draw(pose, text, x, y, argb);
        }
    }

    public void item(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderGuiItem(pose, stack, x, y);
    }

    public void face(UUID playerId, int x, int y, int size, int argb) {
        RenderSystem.setShaderTexture(0, skinOf(playerId));
        tint(argb);
        PlayerFaceRenderer.draw(pose, x, y, size);
        untint();
    }

    private static void tint(int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f,
                (argb >>> 24) / 255f);
    }

    private static void untint() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation skinOf(UUID playerId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(playerId);
            if (info != null) {
                return info.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(playerId);
    }

    public static void registerTexture(Asset asset, int width, int height, int[] argb) {
        Textures.register(asset, width, height, argb);
    }
}
