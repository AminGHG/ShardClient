package net.am7n.shardclient.full.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import org.joml.Quaternionf;

public final class Poses {
    private Poses() {
    }

    public static void rotate(PoseStack pose, Quaternionf rotation) {
        pose.mulPose(new Quaternion(rotation.x, rotation.y, rotation.z, rotation.w));
    }
}
