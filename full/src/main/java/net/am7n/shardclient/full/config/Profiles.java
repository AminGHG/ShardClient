package net.am7n.shardclient.full.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.ModuleManager;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Profiles {
    public static final String FIRST = "Default";
    public static final int MAX_NAME = 24;

    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9 _\\-]+");
    private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("shardclient").resolve("profiles");
    private final ModuleManager modules;
    private final Map<String, JsonObject> defaults = new HashMap<>();
    private final List<String> names = new ArrayList<>();
    private String active = FIRST;

    Profiles(ModuleManager modules) {
        this.modules = modules;
        for (Module module : modules.all()) {
            defaults.put(module.id(), module.toJson());
        }
    }

    public List<String> names() {
        return List.copyOf(names);
    }

    public String active() {
        return active;
    }

    public boolean isActive(String name) {
        return active.equalsIgnoreCase(name);
    }

    void load(String wanted, JsonObject legacy) {
        refresh();
        if (names.isEmpty()) {
            write(FIRST, legacy != null ? legacy : current());
            refresh();
        }
        String pick = find(wanted);
        active = pick != null ? pick : find(FIRST) != null ? find(FIRST) : names.get(0);
        JsonObject stored = read(active);
        if (stored != null) {
            apply(stored);
        } else if (legacy != null) {
            apply(legacy);
        }
    }

    void save() {
        write(active, current());
    }

    public String check(String name, String ignoring) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) {
            return "Type a name first";
        }
        if (clean.length() > MAX_NAME) {
            return "Names can be up to " + MAX_NAME + " characters";
        }
        if (!NAME.matcher(clean).matches()) {
            return "Use letters, numbers, spaces, - and _";
        }
        if (RESERVED.contains(clean.toLowerCase(Locale.ROOT))) {
            return "That name can't be used";
        }
        String taken = find(clean);
        if (taken != null && (ignoring == null || !taken.equalsIgnoreCase(ignoring))) {
            return "There's already a profile called " + taken;
        }
        return null;
    }

    public String create(String name, boolean blank) {
        String problem = check(name, null);
        if (problem != null) {
            return problem;
        }
        String clean = name.strip();
        save();
        JsonObject start = blank ? blank() : current();
        if (!write(clean, start)) {
            return "Couldn't save the profile";
        }
        refresh();
        active = clean;
        apply(start);
        return null;
    }

    public String duplicate(String source) {
        String base = source + " copy";
        String name = base.length() > MAX_NAME ? base.substring(0, MAX_NAME).strip() : base;
        for (int i = 2; find(name) != null; i++) {
            String suffix = " " + i;
            String cut = base.length() + suffix.length() > MAX_NAME ? base.substring(0, MAX_NAME - suffix.length()).strip() : base;
            name = cut + suffix;
        }
        if (isActive(source)) {
            save();
        }
        JsonObject stored = read(source);
        if (stored == null || !write(name, stored)) {
            return "Couldn't copy " + source;
        }
        refresh();
        return null;
    }

    public void use(String name) {
        String target = find(name);
        if (target == null || isActive(target)) {
            return;
        }
        save();
        JsonObject stored = read(target);
        active = target;
        apply(stored != null ? stored : blank());
    }

    public String rename(String from, String to) {
        String source = find(from);
        if (source == null) {
            return "That profile is gone";
        }
        String problem = check(to, source);
        if (problem != null) {
            return problem;
        }
        String clean = to.strip();
        if (clean.equals(source)) {
            return null;
        }
        try {
            Path temp = dir.resolve(clean + ".json.rename");
            Files.move(file(source), temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, file(clean), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ShardClientRuntime.LOGGER.error("Could not rename profile {} to {}", source, clean, ex);
            return "Couldn't rename the profile";
        }
        if (isActive(source)) {
            active = clean;
        }
        refresh();
        return null;
    }

    public String delete(String name) {
        String target = find(name);
        if (target == null) {
            return null;
        }
        if (names.size() <= 1) {
            return "You need at least one profile";
        }
        if (isActive(target)) {
            String next = names.stream().filter(n -> !n.equalsIgnoreCase(target)).findFirst().orElseThrow();
            JsonObject stored = read(next);
            active = next;
            apply(stored != null ? stored : blank());
        }
        try {
            Files.deleteIfExists(file(target));
        } catch (IOException ex) {
            ShardClientRuntime.LOGGER.error("Could not delete profile {}", target, ex);
            return "Couldn't delete the profile";
        }
        refresh();
        return null;
    }

    public Path folder() {
        return dir;
    }

    private String find(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.strip();
        for (String existing : names) {
            if (existing.equalsIgnoreCase(clean)) {
                return existing;
            }
        }
        return null;
    }

    public void rescan() {
        refresh();
    }

    private void refresh() {
        names.clear();
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path f : files) {
                    String fileName = f.getFileName().toString();
                    String name = fileName.substring(0, fileName.length() - ".json".length());
                    if (Files.isRegularFile(f) && NAME.matcher(name).matches()) {
                        names.add(name);
                    }
                }
            } catch (IOException ex) {
                ShardClientRuntime.LOGGER.warn("Could not list profiles in {}", dir, ex);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private Path file(String name) {
        return dir.resolve(name + ".json");
    }

    private JsonObject current() {
        JsonObject json = new JsonObject();
        for (Module module : modules.all()) {
            json.add(module.id(), module.toJson());
        }
        return json;
    }

    private JsonObject blank() {
        JsonObject json = new JsonObject();
        defaults.forEach((id, value) -> json.add(id, value.deepCopy()));
        return json;
    }

    private void apply(JsonObject stored) {
        for (Module module : modules.all()) {
            try {
                module.resetSettings();
                module.setKey(Module.NO_KEY);
                JsonObject base = defaults.get(module.id());
                if (base != null) {
                    module.fromJson(base);
                }
                if (stored.get(module.id()) instanceof JsonObject moduleJson) {
                    module.fromJson(moduleJson);
                }
            } catch (RuntimeException ex) {
                ShardClientRuntime.LOGGER.warn("Ignoring broken settings of module {}", module.id(), ex);
            }
        }
    }

    private JsonObject read(String name) {
        Path f = file(name);
        try {
            JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            return json.get("modules") instanceof JsonObject stored ? stored : new JsonObject();
        } catch (IOException | RuntimeException ex) {
            ShardClientRuntime.LOGGER.warn("Profile {} is unreadable; keeping it as .broken", f, ex);
            try {
                Files.move(f, f.resolveSibling(name + ".json.broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            return null;
        }
    }

    private boolean write(String name, JsonObject stored) {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.add("modules", stored);
        try {
            Files.createDirectories(dir);
            Path temp = dir.resolve(name + ".json.tmp");
            Files.writeString(temp, GSON.toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            ShardClientRuntime.LOGGER.error("Could not save profile {}", name, ex);
            return false;
        }
    }
}
