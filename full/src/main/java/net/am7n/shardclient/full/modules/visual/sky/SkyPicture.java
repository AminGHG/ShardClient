package net.am7n.shardclient.full.modules.visual.sky;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class SkyPicture {
    private static final int MAX_SIZE = 1024;

    record Image(int width, int height, int[] argb) {
    }

    private SkyPicture() {
    }

    static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("shardclient").resolve("sky").resolve("picture.png");
    }

    static Image load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            return image == null ? null : pixels(image);
        } catch (IOException ex) {
            return null;
        }
    }

    static void delete() throws IOException {
        Files.deleteIfExists(file());
    }

    static Path choose() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(5);
            for (String pattern : List.of("*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")) {
                filters.put(stack.UTF8(pattern));
            }
            filters.flip();
            String picked = TinyFileDialogs.tinyfd_openFileDialog("Choose a sky picture", "", filters, "Images", false);
            return picked == null ? null : Path.of(picked);
        }
    }

    static Path pasted(String clipboardText) throws IOException, InterruptedException {
        if (clipboardText != null) {
            String text = clipboardText.strip().replace("\"", "");
            if (!text.isEmpty() && text.length() < 1024) {
                try {
                    Path path = Path.of(text.startsWith("file://") ? text.substring(7) : text);
                    if (Files.isRegularFile(path)) {
                        return path;
                    }
                } catch (InvalidPathException ignored) {
                }
            }
        }
        Path temp = Files.createTempFile("shardclient-sky", ".png");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command;
        if (os.contains("win")) {
            String target = temp.toString().replace("'", "''");
            command = List.of("powershell", "-NoProfile", "-NonInteractive", "-STA", "-Command",
                    "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; "
                            + "$i = [System.Windows.Forms.Clipboard]::GetImage(); "
                            + "if ($i -eq $null) { $f = [System.Windows.Forms.Clipboard]::GetFileDropList(); "
                            + "if ($f.Count -gt 0) { $i = [System.Drawing.Image]::FromFile($f[0]) } }; "
                            + "if ($i -eq $null) { exit 3 }; $i.Save('" + target + "', [System.Drawing.Imaging.ImageFormat]::Png)");
        } else if (os.contains("mac")) {
            String target = temp.toString().replace("\"", "\\\"");
            command = List.of("osascript", "-e", "set f to open for access POSIX file \"" + target + "\" with write permission",
                    "-e", "write (the clipboard as «class PNGf») to f", "-e", "close access f");
        } else if (System.getenv("WAYLAND_DISPLAY") != null) {
            command = List.of("sh", "-c", "wl-paste --type image/png > \"$0\"", temp.toString());
        } else {
            command = List.of("sh", "-c", "xclip -selection clipboard -t image/png -o > \"$0\"", temp.toString());
        }
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getOutputStream().close();
        process.getInputStream().close();
        if (!process.waitFor(8, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return null;
        }
        return process.exitValue() == 0 && Files.size(temp) > 0 ? temp : null;
    }

    static Image store(Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("Not an image");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        float shrink = Math.min(1f, MAX_SIZE / (float) Math.max(width, height));
        int w = Math.max(1, Math.round(width * shrink));
        int h = Math.max(1, Math.round(height * shrink));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        Path file = file();
        Files.createDirectories(file.getParent());
        File temp = file.resolveSibling("picture.png.tmp").toFile();
        ImageIO.write(scaled, "png", temp);
        Files.move(temp.toPath(), file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return pixels(scaled);
    }

    private static Image pixels(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        return new Image(w, h, image.getRGB(0, 0, w, h, null, 0, w));
    }
}
