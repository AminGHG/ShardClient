package net.am7n.shardclient.shared.render;

public final class Icons {
    private Icons() {
    }

    private static Asset icon(String name) {
        return Asset.shared("textures/gui/icons/" + name + ".png");
    }

    public static final Asset PERSON = icon("person");
    public static final Asset PEOPLE = icon("people");
    public static final Asset GEAR = icon("gear");
    public static final Asset POWER = icon("power");
    public static final Asset SWAP = icon("swap");
    public static final Asset GRID = icon("grid");
    public static final Asset STAR = icon("star");
    public static final Asset STAR_FILLED = icon("star-filled");
    public static final Asset REFRESH = icon("refresh");
    public static final Asset PLUS = icon("plus");
    public static final Asset PENCIL = icon("pencil");
    public static final Asset TRASH = icon("trash");
    public static final Asset LINK = icon("link");
    public static final Asset BACK = icon("back");
    public static final Asset PLAY = icon("play");
    public static final Asset CHEVRON = icon("chevron");
}
