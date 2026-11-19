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

    public static Asset parse(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? minecraft(id) : new Asset(id.substring(0, colon), id.substring(colon + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
