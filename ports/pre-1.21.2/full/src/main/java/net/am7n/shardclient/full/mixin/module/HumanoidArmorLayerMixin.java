package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.full.modules.pvp.ArmorTint;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @ModifyArg(method = "renderModel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int shardclient$hitTint(int color) {
        return ArmorTint.apply(color);
    }

    @Redirect(method = "renderTrim", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void shardclient$hitTintTrim(HumanoidModel<?> model, PoseStack pose, VertexConsumer consumer, int light,
                                         int overlay) {
        model.renderToBuffer(pose, consumer, light, overlay, ArmorTint.apply(-1));
    }
}
