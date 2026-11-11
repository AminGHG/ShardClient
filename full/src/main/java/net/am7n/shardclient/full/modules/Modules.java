package net.am7n.shardclient.full.modules;

import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.ShardClientRuntime;

/** The list of every module of the Full Client, in the order they appear in the menu. */
public final class Modules {

    private Modules() {
    }

    public static void registerAll(ModuleManager modules, ShardClientRuntime runtime) {
    }

    /** Event hooks some modules need (attacks, HUD element replacements); after the config is loaded. */
    public static void registerHooks(ModuleManager modules) {
    }
}
