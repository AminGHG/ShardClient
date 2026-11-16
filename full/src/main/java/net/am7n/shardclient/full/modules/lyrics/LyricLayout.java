package net.am7n.shardclient.full.modules.lyrics;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

final class LyricLayout {
    static final float ROW = 10;
    private static final float BIG = 1.75f;
    private static final float MEDIUM = 1.3f;

    record Row(int start, int end, float scale, int width) {
        float height() {
            return ROW * scale;
        }
    }

    private record Key(String plain, String style, boolean words, int maxWidth) {
    }

    private static final Map<Key, List<Row>> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, List<Row>> eldest) {
            return size() > 64;
        }
    };

    private LyricLayout() {
    }

    static List<Row> rows(Component full, boolean words, int maxWidth, String style, ToIntFunction<Component> measure) {
        String plain = plain(full);
        Key key = new Key(plain, style, words, maxWidth);
        List<Row> cached = CACHE.get(key);
        if (cached == null) {
            cached = build(full, plain, words, maxWidth, measure);
            CACHE.put(key, cached);
        }
        return cached;
    }

    private static List<Row> build(Component full, String plain, boolean words, int maxWidth,
                                   ToIntFunction<Component> measure) {
        List<int[]> spans = words(plain);
        if (spans.isEmpty()) {
            return List.of(new Row(0, plain.length(), 1, measure.applyAsInt(full)));
        }
        return words ? wordsLayout(full, plain, spans, maxWidth, measure) : linesLayout(full, spans, maxWidth, measure);
    }

    static float height(List<Row> rows) {
        float total = 0;
        for (Row row : rows) {
            total += row.height();
        }
        return total;
    }

    static int width(List<Row> rows) {
        int widest = 0;
        for (Row row : rows) {
            widest = Math.max(widest, Math.round(row.width() * row.scale()));
        }
        return widest;
    }

    private static List<Row> linesLayout(Component full, List<int[]> spans, int maxWidth,
                                         ToIntFunction<Component> measure) {
        List<Row> rows = new ArrayList<>();
        int start = spans.getFirst()[0];
        int end = spans.getFirst()[1];
        for (int i = 1; i < spans.size(); i++) {
            int[] word = spans.get(i);
            if (measure.applyAsInt(slice(full, start, word[1])) > maxWidth) {
                rows.add(row(full, start, end, 1, measure));
                start = word[0];
            }
            end = word[1];
        }
        rows.add(row(full, start, end, 1, measure));
        return rows;
    }

    private static List<Row> wordsLayout(Component full, String plain, List<int[]> spans, int maxWidth,
                                         ToIntFunction<Component> measure) {
        if (spans.size() == 1) {
            int[] only = spans.getFirst();
            return List.of(fitted(full, only[0], only[1], BIG, maxWidth, measure));
        }
        int big = longest(plain, spans, 0, spans.size(), -1);
        int medium = -1;
        if (spans.size() >= 6) {
            medium = big < spans.size() / 2 ? longest(plain, spans, spans.size() / 2, spans.size(), big)
                    : longest(plain, spans, 0, spans.size() / 2, big);
        }
        int smallWidth = Math.max(40, Math.round(maxWidth * 0.8f));
        List<Row> rows = new ArrayList<>();
        int start = -1;
        int end = -1;
        int count = 0;
        for (int i = 0; i < spans.size(); i++) {
            int[] word = spans.get(i);
            if (i == big || i == medium) {
                if (start >= 0) {
                    rows.add(row(full, start, end, 1, measure));
                    start = -1;
                    count = 0;
                }
                rows.add(fitted(full, word[0], word[1], i == big ? BIG : MEDIUM, maxWidth, measure));
                continue;
            }
            if (start >= 0 && (count >= 3 || measure.applyAsInt(slice(full, start, word[1])) > smallWidth)) {
                rows.add(row(full, start, end, 1, measure));
                start = -1;
                count = 0;
            }
            if (start < 0) {
                start = word[0];
            }
            end = word[1];
            count++;
        }
        if (start >= 0) {
            rows.add(row(full, start, end, 1, measure));
        }
        return rows;
    }

    private static int longest(String plain, List<int[]> spans, int from, int to, int skip) {
        int best = -1;
        int bestLength = 3;
        for (int i = from; i < to; i++) {
            if (i == skip) {
                continue;
            }
            int[] span = spans.get(i);
            int length = 0;
            for (int c = span[0]; c < span[1]; c++) {
                if (Character.isLetterOrDigit(plain.charAt(c))) {
                    length++;
                }
            }
            if (length >= bestLength) {
                bestLength = length;
                best = i;
            }
        }
        return best;
    }

    private static Row fitted(Component full, int start, int end, float scale, int maxWidth,
                              ToIntFunction<Component> measure) {
        int width = measure.applyAsInt(slice(full, start, end));
        float fit = width <= 0 ? scale : Math.min(scale, maxWidth / (float) width);
        return new Row(start, end, Math.max(1, fit), width);
    }

    private static Row row(Component full, int start, int end, float scale, ToIntFunction<Component> measure) {
        return new Row(start, end, scale, measure.applyAsInt(slice(full, start, end)));
    }

    private static List<int[]> words(String plain) {
        List<int[]> spans = new ArrayList<>();
        int i = 0;
        while (i < plain.length()) {
            while (i < plain.length() && plain.charAt(i) == ' ') {
                i++;
            }
            int start = i;
            while (i < plain.length() && plain.charAt(i) != ' ') {
                i++;
            }
            if (i > start) {
                spans.add(new int[]{start, i});
            }
        }
        return spans;
    }

    static String plain(Component text) {
        StringBuilder out = new StringBuilder();
        text.visit((style, part) -> {
            out.append(part);
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    static Component slice(Component text, int start, int end) {
        MutableComponent out = Component.empty();
        int[] at = {0};
        text.visit((style, part) -> {
            int from = Math.max(start, at[0]);
            int to = Math.min(end, at[0] + part.length());
            if (from < to) {
                out.append(Component.literal(part.substring(from - at[0], to - at[0])).withStyle(style));
            }
            at[0] += part.length();
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
