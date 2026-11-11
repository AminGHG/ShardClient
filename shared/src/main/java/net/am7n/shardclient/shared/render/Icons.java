package net.am7n.shardclient.shared.render;

import net.minecraft.resources.Identifier;

/** ShardClient's interface icons: one set, one stroke weight, white so they can be tinted. */
public final class Icons {

    private Icons() {
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("shardclient-shared", "textures/gui/icons/" + name + ".png");
    }

    public static final Identifier PERSON = icon("person");
    public static final Identifier PEOPLE = icon("people");
    public static final Identifier GEAR = icon("gear");
    public static final Identifier POWER = icon("power");
    public static final Identifier SWAP = icon("swap");
    public static final Identifier GRID = icon("grid");
    public static final Identifier STAR = icon("star");
    public static final Identifier STAR_FILLED = icon("star-filled");
    public static final Identifier REFRESH = icon("refresh");
    public static final Identifier PLUS = icon("plus");
    public static final Identifier PENCIL = icon("pencil");
    public static final Identifier TRASH = icon("trash");
    public static final Identifier LINK = icon("link");
    public static final Identifier BACK = icon("back");
    public static final Identifier PLAY = icon("play");
    public static final Identifier CHEVRON = icon("chevron");
}
