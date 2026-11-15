package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.minecraft.client.OptionInstance;

public final class ToggleKeys {
    private ToggleKeys() {
    }

    private abstract static class OptionToggle extends Module {
        private Boolean before;

        private boolean pending;

        OptionToggle(String id, String name, String description) {
            super(id, name, description, Category.UTILITY);
        }

        protected abstract OptionInstance<Boolean> option();

        @Override
        protected void onEnable() {
            if (mc.options == null) {
                pending = true;
                return;
            }
            apply();
        }

        private void apply() {
            pending = false;
            before = option().get();
            option().set(true);
        }

        @Override
        public void tick() {
            if (pending && mc.options != null) {
                apply();
            }
        }

        @Override
        protected void onDisable() {
            if (pending) {
                pending = false;
                return;
            }
            option().set(before != null ? before : false);
            before = null;
        }
    }

    public static final class Sprint extends OptionToggle {
        public Sprint() {
            super("toggle_sprint", "Toggle sprint", "Press sprint once to keep sprinting.");
        }

        @Override
        protected OptionInstance<Boolean> option() {
            return mc.options.toggleSprint();
        }
    }

    public static final class Sneak extends OptionToggle {
        public Sneak() {
            super("toggle_sneak", "Toggle sneak", "Press sneak once to keep sneaking.");
        }

        @Override
        protected OptionInstance<Boolean> option() {
            return mc.options.toggleCrouch();
        }
    }
}
