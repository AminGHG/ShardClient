package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

public final class Poses {
    private Poses() {
    }

    public static void rotate(PoseStack pose, Quaternionf rotation) {
        pose.mulPose(rotation);
    }
}
