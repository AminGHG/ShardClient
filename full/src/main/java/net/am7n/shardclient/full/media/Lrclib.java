package net.am7n.shardclient.full.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.am7n.shardclient.shared.ShardClientRuntime;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class Lrclib {
    private static final String API = "https://lrclib.net/api";
    private static final int CACHE_SIZE = 64;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, Lyrics> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Lyrics> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public synchronized Lyrics find(NowPlaying track) {
        String key = (track.artist() + "\u0000" + track.title()).toLowerCase();
        Lyrics cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Lyrics found;
        try {
            found = exact(track).or(() -> search(track)).orElse(Lyrics.NONE);
        } catch (RuntimeException ex) {
            ShardClientRuntime.LOGGER.debug("Lyrics lookup failed: {}", ex.toString());
            return Lyrics.NONE;
        }
        cache.put(key, found);
        return found;
    }

    private Optional<Lyrics> exact(NowPlaying track) {
        StringBuilder query = new StringBuilder("/get?track_name=").append(encode(track.title()))
                .append("&artist_name=").append(encode(track.artist()));
        if (!track.album().isBlank()) {
            query.append("&album_name=").append(encode(track.album()));
        }
        if (track.durationMillis() > 0) {
            query.append("&duration=").append(Math.round(track.durationMillis() / 1000.0));
        }
        return get(query.toString()).flatMap(json -> json instanceof JsonObject object ? synced(object) : Optional.empty());
    }

    private Optional<Lyrics> search(NowPlaying track) {
        String query = "/search?track_name=" + encode(track.title()) + "&artist_name=" + encode(track.artist());
        Optional<JsonElement> result = get(query);
        if (result.isEmpty() || !(result.get() instanceof JsonArray list)) {
            return Optional.empty();
        }
        Lyrics best = null;
        long bestDistance = Long.MAX_VALUE;
        for (JsonElement element : list) {
            if (!(element instanceof JsonObject object)) {
                continue;
            }
            Optional<Lyrics> lyrics = synced(object);
            if (lyrics.isEmpty()) {
                continue;
            }
            long distance = 0;
            JsonElement d = object.get("duration");
            if (track.durationMillis() > 0 && d != null && d.isJsonPrimitive()) {
                distance = Math.abs(Math.round(d.getAsDouble() * 1000) - track.durationMillis());
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = lyrics.get();
            }
        }

        return best != null && bestDistance <= 10_000 ? Optional.of(best) : Optional.empty();
    }

    private static Optional<Lyrics> synced(JsonObject object) {
        if (object.get("syncedLyrics") instanceof JsonPrimitive lrc) {
            Lyrics lyrics = Lyrics.parseLrc(lrc.getAsString());
            return lyrics.isEmpty() ? Optional.empty() : Optional.of(lyrics);
        }
        return Optional.empty();
    }

    private Optional<JsonElement> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ShardClient/" + ShardClientRuntime.get().version()
                        + " (https://github.com/AminGHG/ShardClient)")
                .GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return Optional.of(JsonParser.parseString(response.body()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex.toString());
        }
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
