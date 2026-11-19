package net.am7n.shardclient.full.mixin.module;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.am7n.shardclient.full.modules.Features;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.render.Asset;
import net.am7n.shardclient.shared.render.Assets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private boolean shardclient$blurFailed;

    @Unique
    private PostChain shardclient$blur;

    @Unique
    private int shardclient$blurWidth;

    @Unique
    private int shardclient$blurHeight;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void shardclient$motionBlur(float partialTick, long nanos, boolean renderLevel, CallbackInfo ci) {
        shardclient$applyBlur(renderLevel, minecraft.getDeltaFrameTime());
    }

    @Unique
    private void shardclient$applyBlur(boolean renderLevel, float frameTime) {
        if (!renderLevel || minecraft.level == null || !Features.on(Features.motionBlur) || shardclient$blurFailed) {
            return;
        }
        float blend = Features.motionBlur.blendFactor();
        if (blend <= 0) {
            return;
        }
        try {
            RenderTarget main = minecraft.getMainRenderTarget();
            if (shardclient$blur == null) {
                shardclient$blur = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main,
                        Assets.id(Asset.of("shardclient", "shaders/post/motion_blur.json")));
                shardclient$blurWidth = -1;
            }
            if (main.width != shardclient$blurWidth || main.height != shardclient$blurHeight) {
                shardclient$blur.resize(main.width, main.height);
                shardclient$blurWidth = main.width;
                shardclient$blurHeight = main.height;
            }
            for (PostPass pass : ((LegacyPostChainAccessor) shardclient$blur).shardclient$passes()) {
                pass.getEffect().safeGetUniform("BlendFactor").set(blend);
            }
            shardclient$blur.process(frameTime);
            main.bindWrite(false);
        } catch (Exception ex) {
            shardclient$blurFailed = true;
            ShardClientRuntime.LOGGER.error("Motion blur failed and was turned off", ex);
        }
    }
}
