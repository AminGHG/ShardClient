package net.am7n.shardclient.shared.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Assets {
    private static final Map<Asset, ResourceLocation> IDS = new ConcurrentHashMap<>();

    private Assets() {
    }

    public static Asset asset(ResourceLocation id) {
        return new Asset(id.getNamespace(), id.getPath());
    }

    public static ResourceLocation id(Asset asset) {
        return IDS.computeIfAbsent(asset, a -> new ResourceLocation(a.namespace(), a.path()));
    }
}
