package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.vertex.PoseStack;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.full.modules.pvp.ArmorTint;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    private static final String SUBMIT = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V";

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void shardclient$hitTintStart(PoseStack pose, SubmitNodeCollector collector, int light,
                                          HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        ArmorTint.set(state.hasRedOverlay && Features.on(Features.hitColor) ? Features.hitColor.armorTint() : -1);
    }

    @Inject(method = SUBMIT, at = @At("RETURN"))
    private void shardclient$hitTintEnd(PoseStack pose, SubmitNodeCollector collector, int light,
                                        HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        ArmorTint.set(-1);
    }
}
