package com.spacekeeperfx.model;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight, sortable-ish ID generator with a time component.
 *
 * Format:  TTTTTTTTT-RRRRRR  (base36, uppercased)
 *  - T: currentTimeMillis in base36, left-padded to 9 chars
 *  - R: 6 random base36 chars
 *
 * Lexicographic order reflects time; good enough for UI sorting and filenames.
 * Use newIdWithPrefix("rec_") for type-tagged IDs.
 */
public final class IdGenerator {
    private static final int TIME_WIDTH = 9;
    private static final int RAND_WIDTH = 6;
    private static final char PAD = '0';

    private IdGenerator() {}

    public static String newId() {
        long now = System.currentTimeMillis();
        String t = toBase36Padded(now, TIME_WIDTH);
        String r = randomBase36(RAND_WIDTH);
        return (t + r).toUpperCase(Locale.ROOT);
    }

    public static String newIdWithPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return newId();
        return prefix + newId();
    }

    private static String toBase36Padded(long value, int width) {
        String s = Long.toString(value, 36);
        if (s.length() >= width) return s.substring(s.length() - width);
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(PAD);
        sb.append(s);
        return sb.toString();
    }

    private static String randomBase36(int width) {
        // Use ThreadLocalRandom by default; fallback to SecureRandom if needed
        long bound = (long) Math.pow(36, Math.min(width, 10)); // numeric bound for up to 10 digits
        long n = ThreadLocalRandom.current().nextLong(bound);
        String s = Long.toString(n, 36);
        // left pad
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(PAD);
        sb.append(s);
        return sb.toString();
    }
}
