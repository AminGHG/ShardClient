package net.am7n.shardclient.full.modules;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("shardclient", "main"));

    public static KeyMapping zoom;
    public static KeyMapping freelook;
    public static KeyMapping copyCoordinates;
    public static KeyMapping addWaypoint;
    public static KeyMapping removeWaypoint;

    private Keys() {
    }

    public static void register() {
        zoom = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.shardclient.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        freelook = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.shardclient.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
        copyCoordinates = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.shardclient.copy_coordinates",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
        addWaypoint = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.shardclient.add_waypoint", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        removeWaypoint = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.shardclient.remove_waypoint",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }
}
