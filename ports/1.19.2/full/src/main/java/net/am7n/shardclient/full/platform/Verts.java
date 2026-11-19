package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class Verts {
    private final PoseStack stack = new PoseStack();
    private final VertexConsumer out;

    Verts(PoseStack.Pose pose, VertexConsumer out) {
        this.stack.last().pose().load(pose.pose());
        this.stack.last().normal().load(pose.normal());
        this.out = out;
    }

    public PoseStack stack() {
        return stack;
    }

    public void color(float x, float y, float z, int argb, int light) {
        out.vertex(stack.last().pose(), x, y, z).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
                .uv2(light).endVertex();
    }

    public void textured(float x, float y, float z, int argb, float u, float v, int light) {
        out.vertex(stack.last().pose(), x, y, z).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
                .uv(u, v).uv2(light).endVertex();
    }

    public void line(float x, float y, float z, int argb, float width, float nx, float ny, float nz) {

        out.vertex(stack.last().pose(), x, y, z).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
                .normal(stack.last().normal(), nx, ny, nz).endVertex();
    }

    public void part(ModelPart part, int light, int argb) {
        part.render(stack, out, light, OverlayTexture.NO_OVERLAY, (argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f,
                (argb & 0xFF) / 255f, (argb >>> 24) / 255f);
    }

    public void model(Model model, int light, int argb) {
        model.renderToBuffer(stack, out, light, OverlayTexture.NO_OVERLAY, (argb >> 16 & 0xFF) / 255f,
                (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f, (argb >>> 24) / 255f);
    }
}
