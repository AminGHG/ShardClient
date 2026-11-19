package net.am7n.shardclient.shared.platform;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public final class Profiles {
    private Profiles() {
    }

    public static String name(GameProfile profile) {
        return profile.getName();
    }

    public static UUID id(GameProfile profile) {
        return profile.getId();
    }
}
