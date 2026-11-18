package net.am7n.shardclient.shared.render;

public record Asset(String namespace, String path) {
    public static Asset shared(String path) {
        return new Asset("shardclient-shared", path);
    }

    public static Asset of(String namespace, String path) {
        return new Asset(namespace, path);
    }

    public static Asset minecraft(String path) {
        return new Asset("minecraft", path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
