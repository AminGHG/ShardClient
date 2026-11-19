package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.full.modules.pvp.ArmorTint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void shardclient$hitTintStart(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                          float limbSwing, float limbSwingAmount, float partialTick, float bob,
                                          float yRot, float xRot, CallbackInfo ci) {
        boolean red = entity.hurtTime > 0 || entity.deathTime > 0;
        ArmorTint.set(red && Features.on(Features.hitColor) ? Features.hitColor.armorTint() : -1);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void shardclient$hitTintEnd(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                        float limbSwing, float limbSwingAmount, float partialTick, float bob,
                                        float yRot, float xRot, CallbackInfo ci) {
        ArmorTint.set(-1);
    }

    @ModifyArgs(method = {"renderModel", "renderTrim"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void shardclient$hitTint(Args args) {
        int tint = ArmorTint.apply(-1);
        if (tint == -1) {
            return;
        }
        args.set(4, (float) args.get(4) * (tint >> 16 & 0xFF) / 255f);
        args.set(5, (float) args.get(5) * (tint >> 8 & 0xFF) / 255f);
        args.set(6, (float) args.get(6) * (tint & 0xFF) / 255f);
    }
}
