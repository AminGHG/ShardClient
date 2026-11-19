package net.am7n.shardclient.full.modules;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private static final String CATEGORY = "key.category.shardclient.main";

    public static KeyMapping zoom;
    public static KeyMapping freelook;
    public static KeyMapping copyCoordinates;
    public static KeyMapping addWaypoint;
    public static KeyMapping removeWaypoint;

    private Keys() {
    }

    public static void register() {
        zoom = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.shardclient.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        freelook = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.shardclient.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
        copyCoordinates = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.shardclient.copy_coordinates",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
        addWaypoint = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.shardclient.add_waypoint", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        removeWaypoint = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.shardclient.remove_waypoint",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }
}
