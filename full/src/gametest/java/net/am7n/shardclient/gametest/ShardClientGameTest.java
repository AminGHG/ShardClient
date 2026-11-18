package net.am7n.shardclient.gametest;

import net.am7n.shardclient.full.ShardClientFull;
import net.am7n.shardclient.full.config.ClientConfig;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.module.setting.NumberSetting;
import net.am7n.shardclient.full.module.setting.Setting;
import net.am7n.shardclient.shared.practice.BadgeTestAccess;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.am7n.shardclient.shared.ui.Container;
import net.am7n.shardclient.shared.ui.Element;
import net.am7n.shardclient.shared.ui.Pill;
import net.am7n.shardclient.shared.ui.SearchField;
import net.am7n.shardclient.shared.ui.ShardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ShardClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            Module chams = module("totem_pop_chams");
            set(chams, "own_pops", true);
            set(chams, "lifetime", 200.0);
            set(chams, "fade_out", false);
            chams.setEnabled(true);
            Module hit = module("hit_color");
            set(hit, "color", "Blue");
            set(hit, "strength", 90.0);
            hit.setEnabled(true);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamerule doMobSpawning false");
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 0 100 0 0 0");
            server.runCommand("fill -6 99 -6 6 99 12 minecraft:stone");
            context.waitTicks(40);

            context.runOnClient(mc -> BadgeTestAccess.give(mc.player.getUUID()));
            context.waitTicks(20);
            context.runOnClient(mc -> mc.player.connection.sendChat("badge test"));
            context.waitTicks(10);
            context.takeScreenshot("shardclient-chat-badge");

            String name = context.computeOnClient(mc -> mc.player.getGameProfile().name());
            server.runCommand("summon minecraft:armor_stand -1.5 100 3 {NoGravity:1b,Invisible:1b,CustomNameVisible:1b,CustomName:\"VIP " + name + " | -4\"}");
            server.runCommand("summon minecraft:text_display 1.5 101.5 3 {billboard:\"center\",text:\"Level 12\\nVIP " + name + " | -4\\nLTS | #299\"}");
            context.waitTicks(20);
            context.takeScreenshot("shardclient-nametags");
            server.runCommand("kill @e[type=minecraft:armor_stand]");
            server.runCommand("kill @e[type=minecraft:text_display]");

            server.runCommand("item replace entity @p weapon.offhand with minecraft:totem_of_undying");
            context.waitTicks(5);
            server.runCommand("damage @p 100 minecraft:generic");
            context.waitTicks(5);
            server.runCommand("tp @p 0 100 -4 0 0");
            context.waitTicks(60);
            context.takeScreenshot("shardclient-totem-chams");

            server.runCommand("summon minecraft:zombie 0 100 0 {NoAI:1b,Rotation:[180f,0f],equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:diamond_chestplate\"},legs:{id:\"minecraft:iron_leggings\"},feet:{id:\"minecraft:golden_boots\"}}}");
            context.waitTicks(20);
            server.runCommand("damage @e[type=minecraft:zombie,limit=1] 1 minecraft:generic");
            context.waitTicks(2);
            context.takeScreenshot("shardclient-hit-colour-armor");

            context.runOnClient(mc -> ShardClientFull.get().openMenu(mc));
            context.waitTicks(10);
            context.runOnClient(mc -> find(root(mc), SearchField.class, e -> true).focus(true));
            context.getInput().typeChars("Hit colour");
            context.waitTicks(10);
            context.runOnClient(mc -> {
                Element card = find(root(mc), Element.class, e -> moduleOf(e) == module("hit_color"));
                root(mc).mouseClicked(card.x() + 20, card.y() + 10, 0);
            });
            context.waitTicks(15);
            context.takeScreenshot("shardclient-settings-before-reset");
            context.runOnClient(mc -> {
                Element card = find(root(mc), Element.class, e -> moduleOf(e) == module("hit_color"));
                Element reset = find(card, Pill.class, e -> e.y() > card.y() && "Reset".equals(pillText((Pill) e)));
                root(mc).mouseClicked(reset.x() + reset.width() / 2.0, reset.y() + reset.height() / 2.0, 0);
            });
            context.waitTicks(10);
            context.takeScreenshot("shardclient-settings-after-reset");
            context.runOnClient(mc -> {
                Module hit = module("hit_color");
                Object colour = setting(hit, "color").get();
                Object strength = setting(hit, "strength").get();
                if (!"Lilac".equals(colour) || !Double.valueOf(40).equals(strength)) {
                    throw new AssertionError("Reset left hit colour at " + colour + " / " + strength);
                }
                mc.gui.setScreen(null);
            });
        }
        context.runOnClient(mc -> ShardClientFull.get().config().setMenuStyle(ClientConfig.MenuStyle.VANILLA));
        context.setScreen(TitleScreen::new);
        context.waitTicks(2);
    }

    private static Element root(Minecraft mc) {
        try {
            Field field = ShardScreen.class.getDeclaredField("root");
            field.setAccessible(true);
            return (Element) field.get(mc.gui.screen());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static Module moduleOf(Element element) {
        if (!element.getClass().getSimpleName().equals("ModuleCard")) {
            return null;
        }
        try {
            Field field = element.getClass().getDeclaredField("module");
            field.setAccessible(true);
            return (Module) field.get(element);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String pillText(Pill pill) {
        try {
            Field field = Pill.class.getDeclaredField("text");
            field.setAccessible(true);
            return ((Supplier<?>) field.get(pill)).get().toString();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static <T extends Element> T find(Element from, Class<T> type, Predicate<Element> match) {
        if (type.isInstance(from) && match.test(from)) {
            return type.cast(from);
        }
        if (from instanceof Container container) {
            for (Element child : container.children()) {
                T found = find(child, type, match);
                if (found != null) {
                    return found;
                }
            }
        }
        if (from == null) {
            throw new AssertionError("Nothing to search");
        }
        return null;
    }

    private static Setting<?> setting(Module module, String id) {
        return module.settings().stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("No setting " + id));
    }

    private static Module module(String id) {
        Module module = ShardClientFull.get().modules().get(id);
        if (module == null) {
            throw new AssertionError("No module " + id);
        }
        return module;
    }

    @SuppressWarnings("unchecked")
    private static void set(Module module, String id, Object value) {
        for (Setting<?> setting : module.settings()) {
            if (!setting.id().equals(id)) {
                continue;
            }
            switch (setting) {
                case BooleanSetting bool -> bool.set((Boolean) value);
                case NumberSetting number -> number.set((Double) value);
                case ChoiceSetting choice -> choice.set((String) value);
                default -> ((Setting<Object>) setting).set(value);
            }
            return;
        }
        throw new AssertionError("No setting " + id + " on " + module.id());
    }
}
