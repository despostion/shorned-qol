package dev.shorned.qol.util;

import java.util.Locale;

/** Number and time formatting shared by the HUD, commands and config screen. */
public final class Fmt {
    private Fmt() {}

    public static String num(long n) {
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", n / 1_000_000.0);
        if (n >= 100_000) return String.format(Locale.ROOT, "%.0fk", n / 1000.0);
        if (n >= 10_000) return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** Per-hour rate. Returns "--" for the first 30 s so a tiny sample never shows a silly number. */
    public static String rate(long amount, long activeMs) {
        if (activeMs < 30_000) return "--/hr";
        return num(Math.round(amount / (activeMs / 3_600_000.0))) + "/hr";
    }

    public static String time(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
