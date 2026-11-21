package net.am7n.shardclient.full.mixin.module;

import net.am7n.shardclient.full.modules.Features;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public abstract class FogMixin {
    @Redirect(method = "setupFog", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F"))
    private void shardclient$hideFog(FogData fog, float value) {
        fog.renderDistanceEnd = value;
        if (Features.sky != null && Features.sky.hidesFog()) {
            fog.environmentalStart = 1.0e6f;
            fog.environmentalEnd = 2.0e6f;
            fog.renderDistanceStart = 1.0e6f;
            fog.renderDistanceEnd = 2.0e6f;
        }
    }
}
