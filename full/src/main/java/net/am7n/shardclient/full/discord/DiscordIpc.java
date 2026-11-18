package net.am7n.shardclient.full.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.am7n.shardclient.shared.ShardClientRuntime;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class DiscordIpc implements Closeable {
    private static final int HANDSHAKE = 0;
    private static final int FRAME = 1;

    private interface Pipe extends Closeable {
        void write(byte[] bytes) throws IOException;

        void readFully(byte[] into) throws IOException;
    }

    private final Pipe pipe;

    private DiscordIpc(Pipe pipe) {
        this.pipe = pipe;
    }

    static DiscordIpc connect(String applicationId) throws IOException {
        IOException last = new IOException("Discord is not running");
        for (int i = 0; i < 10; i++) {
            Pipe pipe;
            try {
                pipe = open(i);
            } catch (IOException ex) {
                last = ex;
                continue;
            }
            DiscordIpc ipc = new DiscordIpc(pipe);
            try {
                JsonObject handshake = new JsonObject();
                handshake.addProperty("v", 1);
                handshake.addProperty("client_id", applicationId);
                ipc.send(HANDSHAKE, handshake);
                JsonObject ready = ipc.receive();
                if (ready.has("evt") && ready.get("evt").isJsonPrimitive()
                        && "READY".equals(ready.get("evt").getAsString())) {
                    return ipc;
                }
                last = new IOException("Discord refused the connection: " + ready);
            } catch (IOException | RuntimeException ex) {
                last = ex instanceof IOException io ? io : new IOException(ex);
            }
            ipc.close();
        }
        throw last;
    }

    void setActivity(JsonObject activity) throws IOException {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);
        JsonObject command = new JsonObject();
        command.addProperty("cmd", "SET_ACTIVITY");
        command.add("args", args);
        command.addProperty("nonce", UUID.randomUUID().toString());
        send(FRAME, command);
        JsonObject answer = receive();
        if (answer.has("evt") && answer.get("evt").isJsonPrimitive()
                && "ERROR".equals(answer.get("evt").getAsString())) {
            ShardClientRuntime.LOGGER.warn("Discord rejected the presence: {}", answer.get("data"));
        }
    }

    private void send(int opcode, JsonObject json) throws IOException {
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(opcode).putInt(body.length).put(body);
        pipe.write(frame.array());
    }

    private JsonObject receive() throws IOException {
        byte[] header = new byte[8];
        pipe.readFully(header);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buffer.getInt();
        int length = buffer.getInt();
        if (length < 0 || length > 1 << 20) {
            throw new IOException("Unexpected Discord frame length " + length);
        }
        byte[] body = new byte[length];
        pipe.readFully(body);
        if (opcode == 2) {
            throw new IOException("Discord closed the connection: " + new String(body, StandardCharsets.UTF_8));
        }
        return JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Override
    public void close() {
        try {
            pipe.close();
        } catch (IOException ignored) {
        }
    }

    private static Pipe open(int index) throws IOException {
        String name = "discord-ipc-" + index;
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            RandomAccessFile file = new RandomAccessFile("\\\\.\\pipe\\" + name, "rw");
            return new Pipe() {
                @Override
                public void write(byte[] bytes) throws IOException {
                    file.write(bytes);
                }

                @Override
                public void readFully(byte[] into) throws IOException {
                    file.readFully(into);
                }

                @Override
                public void close() throws IOException {
                    file.close();
                }
            };
        }
        for (Path dir : unixDirectories()) {
            Path socket = dir.resolve(name);
            if (!Files.exists(socket)) {
                continue;
            }
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(socket));
            return new Pipe() {
                @Override
                public void write(byte[] bytes) throws IOException {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }

                @Override
                public void readFully(byte[] into) throws IOException {
                    ByteBuffer buffer = ByteBuffer.wrap(into);
                    while (buffer.hasRemaining()) {
                        if (channel.read(buffer) < 0) {
                            throw new IOException("Discord closed the connection");
                        }
                    }
                }

                @Override
                public void close() throws IOException {
                    channel.close();
                }
            };
        }
        throw new IOException("No Discord socket " + name);
    }

    private static List<Path> unixDirectories() {
        List<Path> dirs = new ArrayList<>();
        for (String variable : List.of("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                Path base = Path.of(value);
                dirs.add(base);
                dirs.add(base.resolve("app/com.discordapp.Discord"));
                dirs.add(base.resolve("snap.discord"));
            }
        }
        dirs.add(Path.of("/tmp"));
        return dirs;
    }
}
