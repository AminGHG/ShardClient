package net.am7n.shardclient.full.media;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Lyrics(List<Line> lines) {
    public record Line(long startMillis, String text) {
    }

    public static final Lyrics NONE = new Lyrics(List.of());

    private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");

    public static Lyrics parseLrc(String lrc) {
        List<Line> lines = new ArrayList<>();
        if (lrc == null) {
            return NONE;
        }
        for (String raw : lrc.split("\\R")) {
            Matcher matcher = TIME.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (matcher.find() && matcher.start() == end) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                String fraction = matcher.group(3);
                long millis = 0;
                if (fraction != null) {
                    millis = Long.parseLong((fraction + "00").substring(0, 3));
                }
                times.add(minutes * 60_000 + seconds * 1000 + millis);
                end = matcher.end();
            }
            if (times.isEmpty()) {
                continue;
            }
            String text = raw.substring(end).trim();
            for (long time : times) {
                lines.add(new Line(time, text));
            }
        }
        lines.sort((a, b) -> Long.compare(a.startMillis(), b.startMillis()));
        return lines.isEmpty() ? NONE : new Lyrics(List.copyOf(lines));
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int indexAt(long position) {
        int low = 0;
        int high = lines.size() - 1;
        int found = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (lines.get(mid).startMillis() <= position) {
                found = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return found;
    }
}
