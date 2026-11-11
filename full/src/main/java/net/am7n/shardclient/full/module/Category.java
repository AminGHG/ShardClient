package net.am7n.shardclient.full.module;

/** Where a module is listed in the client menu. */
public enum Category {

    VISUAL("Visual"),
    HUD("HUD"),
    PVP("PvP"),
    UTILITY("Utility");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
