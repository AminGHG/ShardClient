package net.am7n.shardclient.full.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PlayerPose {
    public final float glidingProcess;
    public final float pitch;
    public final float flyingRotation;
    public final boolean autoSpinAttack;
    public final boolean applyFlyingYRot;
    private final PlayerModel<AbstractClientPlayer> model;

    private PlayerPose(PlayerModel<AbstractClientPlayer> model, float glidingProcess, float pitch, float flyingRotation,
                       boolean autoSpinAttack, boolean applyFlyingYRot) {
        this.model = model;
        this.glidingProcess = glidingProcess;
        this.pitch = pitch;
        this.flyingRotation = flyingRotation;
        this.autoSpinAttack = autoSpinAttack;
        this.applyFlyingYRot = applyFlyingYRot;
    }

    public static PlayerPose capture(Player entity) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean slim = Skins.slim(player);
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(
                mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
        float limbSpeed = Math.min(player.walkAnimation.speed(1f), 1f);
        float limbPosition = player.walkAnimation.position(1f);
        float netHeadYaw = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot);
        model.attackTime = player.getAttackAnim(1f);
        model.riding = player.isPassenger();
        model.young = player.isBaby();
        model.crouching = player.isCrouching();
        model.prepareMobModel(player, limbPosition, limbSpeed, 1f);
        model.setupAnim(player, limbPosition, limbSpeed, player.tickCount + 1f, netHeadYaw, player.getXRot());
        hideLayers(model);

        float gliding = 0;
        float flyingRotation = 0;
        boolean applyFlying = false;
        if (player.isFallFlying()) {
            float ticks = player.getFallFlyingTicks() + 1f;
            gliding = Mth.clamp(ticks * ticks / 100f, 0f, 1f);
            Vec3 view = player.getViewVector(1f);
            Vec3 motion = player.getDeltaMovementLerped(1f);
            double motionSq = motion.horizontalDistanceSqr();
            double viewSq = view.horizontalDistanceSqr();
            if (motionSq > 0 && viewSq > 0) {
                applyFlying = true;
                double dot = (motion.x * view.x + motion.z * view.z) / Math.sqrt(motionSq * viewSq);
                double cross = motion.x * view.z - motion.z * view.x;
                flyingRotation = (float) (Math.signum(cross) * Math.acos(Math.min(1, Math.max(-1, dot))));
            }
        }
        return new PlayerPose(model, gliding, player.getXRot(), flyingRotation, player.isAutoSpinAttack(), applyFlying);
    }

    private static void hideLayers(PlayerModel<AbstractClientPlayer> model) {
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
