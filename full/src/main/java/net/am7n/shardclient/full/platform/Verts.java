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
        this.stack.last().pose().set(pose.pose());
        this.stack.last().normal().set(pose.normal());
        this.out = out;
    }

    public PoseStack stack() {
        return stack;
    }

    public void color(float x, float y, float z, int argb, int light) {
        out.addVertex(stack.last(), x, y, z).setColor(argb).setLight(light);
    }

    public void textured(float x, float y, float z, int argb, float u, float v, int light) {
        out.addVertex(stack.last(), x, y, z).setColor(argb).setUv(u, v).setLight(light);
    }

    public void line(float x, float y, float z, int argb, float width, float nx, float ny, float nz) {
        out.addVertex(stack.last(), x, y, z).setColor(argb).setLineWidth(width).setNormal(stack.last(), nx, ny, nz);
    }

    public void part(ModelPart part, int light, int argb) {
        part.render(stack, out, light, OverlayTexture.NO_OVERLAY, argb);
    }

    public void model(Model model, int light, int argb) {
        model.renderToBuffer(stack, out, light, OverlayTexture.NO_OVERLAY, argb);
    }
}
