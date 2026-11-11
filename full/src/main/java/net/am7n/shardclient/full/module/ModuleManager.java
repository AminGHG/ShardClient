package net.am7n.shardclient.full.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.am7n.shardclient.full.hud.HudModule;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every module of the Full Client, their keybinds and their per-tick work. */
public final class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<Integer, Boolean> keyWasDown = new HashMap<>();

    public void register(Module module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Two modules with id " + module.id());
        }
    }

    public List<Module> all() {
        return List.copyOf(modules.values());
    }

    public Module get(String id) {
        return modules.get(id);
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M get(Class<M> type) {
        for (Module module : modules.values()) {
            if (type.isInstance(module)) {
                return (M) module;
            }
        }
        return null;
    }

    public List<Module> in(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module.category() == category) {
                list.add(module);
            }
        }
        return list;
    }

    public List<HudModule> hud() {
        List<HudModule> list = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module instanceof HudModule hud) {
                list.add(hud);
            }
        }
        return list;
    }

    /** Called every client tick: module keybinds (only with no screen open) and enabled modules' work. */
    public void tick(Minecraft mc) {
        if (mc.gui.screen() == null) {
            for (Module module : modules.values()) {
                int key = module.key();
                if (key == Module.NO_KEY) {
                    continue;
                }
                boolean down = InputConstants.isKeyDown(mc.getWindow(), key);
                if (down && !keyWasDown.getOrDefault(key, false)) {
                    module.toggle();
                }
                keyWasDown.put(key, down);
            }
        } else {
            keyWasDown.clear();
        }
        for (Module module : modules.values()) {
            if (module.isEnabled()) {
                try {
                    module.tick();
                } catch (RuntimeException ex) {
                    ShardClientRuntime.LOGGER.error("Module {} failed and was switched off", module.id(), ex);
                    module.setEnabled(false);
                }
            }
        }
    }
}
