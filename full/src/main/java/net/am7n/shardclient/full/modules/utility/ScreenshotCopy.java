package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.BooleanSetting;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class ScreenshotCopy extends Module {
    private final BooleanSetting notify = bool("notify", "Show a message", true);

    public ScreenshotCopy() {
        super("screenshot_copy", "Screenshot to clipboard",
                "Copies every screenshot (F2) to your clipboard, ready to paste into Discord.", Category.UTILITY);
        setEnabled(true);
    }

    public void saved(File file) {
        if (!isEnabled() || file == null || !file.isFile()) {
            return;
        }
        boolean copied = copy(file.getAbsoluteFile());
        if (notify.on()) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.sendOverlayMessage(Component.literal(copied ? "Screenshot copied to clipboard"
                            : "Couldn't copy the screenshot"));
                }
            });
        }
    }

    private static boolean copy(File file) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path = file.getPath();
        try {
            ProcessBuilder builder;
            if (os.contains("win")) {
                String quoted = "'" + path.replace("'", "''") + "'";
                builder = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-STA", "-Command",
                        "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; "
                                + "$i = [System.Drawing.Image]::FromFile(" + quoted + "); "
                                + "[System.Windows.Forms.Clipboard]::SetImage($i); $i.Dispose()");
            } else if (os.contains("mac")) {
                builder = new ProcessBuilder("osascript", "-e",
                        "set the clipboard to (read (POSIX file \"" + path.replace("\"", "\\\"")
                                + "\") as «class PNGf»)");
            } else if (System.getenv("WAYLAND_DISPLAY") != null) {
                builder = new ProcessBuilder("wl-copy", "--type", "image/png").redirectInput(file);
            } else {
                builder = new ProcessBuilder(List.of("xclip", "-selection", "clipboard", "-t", "image/png", "-i", path));
            }
            Process process = builder.redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            if (process.exitValue() != 0) {
                ShardClientRuntime.LOGGER.warn("Couldn't copy the screenshot to the clipboard: {}", output);
                return false;
            }
            ShardClientRuntime.LOGGER.info("Copied {} to the clipboard", file.getName());
            return true;
        } catch (Exception ex) {
            ShardClientRuntime.LOGGER.warn("Couldn't copy the screenshot to the clipboard", ex);
            return false;
        }
    }
}
