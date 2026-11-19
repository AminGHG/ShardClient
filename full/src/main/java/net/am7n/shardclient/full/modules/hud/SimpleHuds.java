package net.am7n.shardclient.full.modules.hud;

import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.full.platform.Client;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SimpleHuds {
    private SimpleHuds() {
    }

    public static final class Fps extends TextHud {
        public Fps() {
            super("fps", "FPS", "Frames per second.", 0.0f, 0.0f);
        }

        @Override
        protected Component text() {
            return labelled("FPS", Integer.toString(Client.fps()));
        }
    }

    public static final class Ping extends TextHud {
        public Ping() {
            super("ping", "Ping", "Your latency to the server, as the server measures it.", 0.0f, 0.075f);
        }

        @Override
        public boolean visible() {
            return mc.getConnection() != null && !mc.isLocalServer();
        }

        @Override
        protected Component text() {
            PlayerInfo info = mc.player == null || mc.getConnection() == null ? null
                    : mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return labelled("Ping", info == null ? "?" : info.getLatency() + " ms");
        }
    }

    public static final class Coordinates extends TextHud {
        private final BooleanSetting otherDimension = bool("other", "Nether / Overworld coordinates", false);
        private final BooleanSetting decimals = bool("decimals", "Decimals", false);

        public Coordinates() {
            super("coordinates", "Coordinates", "Where you are (X Y Z).", 0.0f, 0.15f);
        }

        @Override
        protected Component text() {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            String main = format(x) + "  " + format(y) + "  " + format(z);
            if (otherDimension.on() && mc.level != null) {
                if (mc.level.dimension() == Level.OVERWORLD) {
                    main += "   Nether " + format(x / 8) + "  " + format(z / 8);
                } else if (mc.level.dimension() == Level.NETHER) {
                    main += "   Overworld " + format(x * 8) + "  " + format(z * 8);
                }
            }
            return labelled("XYZ", main);
        }

        private String format(double value) {
            return decimals.on() ? String.format(Locale.ROOT, "%.1f", value) : Long.toString((long) Math.floor(value));
        }
    }

    public static final class Direction extends TextHud {
        private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

        public Direction() {
            super("direction", "Direction", "Which way you are facing.", 0.0f, 0.225f);
        }

        @Override
        protected Component text() {
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            int index = Math.floorMod(Math.round(yaw / 45f), 8);
            String axis = switch (mc.player.getDirection()) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case EAST -> "+X";
                case WEST -> "-X";
                default -> "";
            };
            return labelled("Facing", NAMES[index] + "  " + axis);
        }
    }

    public static final class Clock extends TextHud {
        private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
        private final ChoiceSetting format = choice("format", "Format", "24 h", "24 h", "12 h");

        public Clock() {
            super("clock", "Clock", "The time on your computer.", 1.0f, 0.0f);
        }

        @Override
        protected Component text() {
            return labelled("Time", LocalTime.now().format(format.is("12 h") ? H12 : H24));
        }
    }

    public static final class Server extends TextHud {
        public Server() {
            super("server", "Server address", "The server you are playing on.", 1.0f, 0.075f);
        }

        @Override
        public boolean visible() {
            return mc.getCurrentServer() != null;
        }

        @Override
        protected Component text() {
            ServerData server = mc.getCurrentServer();
            return labelled("Server", server == null ? "" : server.ip);
        }
    }

    public static final class Speed extends TextHud {
        public Speed() {
            super("speed", "Speed", "How fast you move horizontally, in blocks per second.", 0.0f, 0.30f);
        }

        @Override
        protected Component text() {
            double dx = mc.player.getX() - mc.player.xo;
            double dz = mc.player.getZ() - mc.player.zo;
            return labelled("Speed", String.format(Locale.ROOT, "%.2f b/s", Math.sqrt(dx * dx + dz * dz) * 20));
        }
    }

    public static final class Memory extends TextHud {
        public Memory() {
            super("memory", "Memory", "How much memory the game uses.", 1.0f, 0.15f);
        }

        @Override
        protected Component text() {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long max = runtime.maxMemory() / (1024 * 1024);
            return labelled("RAM", used + " / " + max + " MB  " + used * 100 / Math.max(1, max) + "%");
        }
    }

    public static final class Session extends TextHud {
        private static volatile long joinedAt;

        public Session() {
            super("session", "Session timer", "How long you have been playing on this server.", 1.0f, 0.225f);
        }

        public static void joined() {
            joinedAt = System.currentTimeMillis();
        }

        @Override
        public boolean visible() {
            return joinedAt > 0;
        }

        @Override
        protected Component text() {
            long seconds = (System.currentTimeMillis() - joinedAt) / 1000;
            String value = seconds >= 3600
                    ? String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                    : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            return labelled("Session", value);
        }
    }

    public static final class Cps extends TextHud {
        private final BooleanSetting showRight = bool("right", "Right clicks too", true);

        public Cps() {
            super("cps", "CPS", "Clicks per second.", 0.0f, 0.375f);
        }

        @Override
        protected Component text() {
            return labelled("CPS", showRight.on() ? ClickCounter.left() + " | " + ClickCounter.right()
                    : Integer.toString(ClickCounter.left()));
        }
    }
}
