package net.am7n.shardclient.shared.ui;

import net.am7n.shardclient.shared.render.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

/**
 * A player's face. The skin is the one the game already has for that player on the current server (everyone in
 * the player list); for anyone else it is the default skin Minecraft assigns to that id. Nothing is downloaded.
 */
public final class PlayerFace extends Element {

    private final UUID playerId;
    private final int size;

    public PlayerFace(UUID playerId, int size) {
        this.playerId = playerId;
        this.size = size;
    }

    @Override
    public Size measure() {
        return new Size(size, size);
    }

    @Override
    public void render(Canvas canvas) {
        canvas.face(skin(), x + (width - size) / 2, y + (height - size) / 2, size);
    }

    private PlayerSkin skin() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(playerId);
            if (info != null) {
                return info.getSkin();
            }
        }
        return DefaultPlayerSkin.get(playerId);
    }
}
