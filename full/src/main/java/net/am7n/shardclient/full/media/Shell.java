package net.am7n.shardclient.full.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class Shell {
    private Shell() {
    }

    static String run(List<String> command, long timeoutMillis) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        process.getOutputStream().close();
        InputStream out = process.getInputStream();
        CompletableFuture<byte[]> text = CompletableFuture.supplyAsync(() -> {
            try {
                return out.readAllBytes();
            } catch (IOException ex) {
                return new byte[0];
            }
        });
        if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IOException(command.get(0) + " timed out");
        }
        if (process.exitValue() != 0) {
            throw new IOException(command.get(0) + " exited with " + process.exitValue());
        }
        try {
            return new String(text.get(timeoutMillis, TimeUnit.MILLISECONDS), StandardCharsets.UTF_8);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IOException(command.get(0) + " output unreadable");
        }
    }

    static boolean available(String program) {
        try {
            Process process = new ProcessBuilder("which", program).start();
            process.getOutputStream().close();
            return process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
