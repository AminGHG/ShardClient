package net.am7n.shardclient.full.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;

public final class PlayerPose {
    public final float glidingProcess;
    public final float pitch;
    public final float flyingRotation;
    public final boolean autoSpinAttack;
    public final boolean applyFlyingYRot;
    private final PlayerModel model;

    private PlayerPose(PlayerModel model, float glidingProcess, float pitch, float flyingRotation,
                       boolean autoSpinAttack, boolean applyFlyingYRot) {
        this.model = model;
        this.glidingProcess = glidingProcess;
        this.pitch = pitch;
        this.flyingRotation = flyingRotation;
        this.autoSpinAttack = autoSpinAttack;
        this.applyFlyingYRot = applyFlyingYRot;
    }

    public static PlayerPose capture(Player player) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderState raw = mc.getEntityRenderDispatcher().getRenderer(player).createRenderState(player, 1.0F);
        if (!(raw instanceof AvatarRenderState state)) {
            return null;
        }
        boolean slim = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
        PlayerModel model = new PlayerModel(
                mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
        model.setupAnim(state);
        hideLayers(model);
        return new PlayerPose(model, state.fallFlyingScale(), state.xRot, state.flyingYRot, state.isAutoSpinAttack,
                state.shouldApplyFlyingYRot);
    }

    private static void hideLayers(PlayerModel model) {
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    public ModelPart[] parts() {
        return new ModelPart[]{model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg};
    }

    public void render(Verts out, int light, int argb) {
        out.model(model, light, argb);
    }
}
