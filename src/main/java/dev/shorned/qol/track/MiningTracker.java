package dev.shorned.qol.track;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.stats.StatsManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mining statistics.
 *
 * Powder and commissions are read from Hypixel's tab-list widgets (the same source other community mods use),
 * which gives absolute, reliable numbers. Powder GAINED is the positive change between two readings; the first
 * reading is only a baseline, and a drop (you spent powder) just moves the baseline.
 *
 * Nothing is invented: if a widget is not enabled, the HUD says "no data" instead of showing zeros.
 */
public final class MiningTracker {
    public enum Ability { UNKNOWN, READY, COOLDOWN }

    /** A widget reading older than this is treated as gone (the widget was hidden / tab list changed). */
    static final long STALE_MS = 10_000;
    /** Chat is only trusted for a quantity while the tab widget has been missing this long. */
    private static final long CHAT_TRUST_MS = 15_000;

    // Chat fallbacks (chat texts are best guesses, see README).
    private static final Pattern POWDER_CHAT = Pattern.compile("\\+\\s*([\\d,]+)\\s+(?:\\S+\\s+)?(Mithril|Gemstone|Glacite) Powder");
    private static final Pattern ABILITY_READY = Pattern.compile("^(.+?) is now available!");
    private static final Pattern ABILITY_USED = Pattern.compile("^You used your (.+?) Pickaxe Ability!");
    private static final Pattern COMMISSION_CHAT = Pattern.compile("^Commission Complete!", Pattern.CASE_INSENSITIVE);

    private static final String[] TYPES = {"Mithril", "Gemstone", "Glacite"};
    private static final long[] lastTotal = {-1, -1, -1};          // last absolute reading per type, -1 = no baseline
    private static final long[] powderSeenAt = {0, 0, 0};          // when each type was last read from the tab
    private static long commissionsSeenAt = 0, infoSeenAt = 0, forgesSeenAt = 0;
    private static String area = "", profile = "";
    private static List<TabParser.Forge> forges = List.of();
    // Last parsed tab, so an unchanged tab list is not re-parsed every refresh.
    private static List<String> lastLines = List.of();
    private static TabParser.Result lastResult = null;
    private static List<TabParser.Commission> commissions = List.of();
    private static Map<String, Boolean> previousDone = new HashMap<>();
    private static Ability ability = Ability.UNKNOWN;
    private static String abilityName = "";
    public static long lastActivity = System.currentTimeMillis();

    private MiningTracker() {}

    public static List<TabParser.Commission> commissions() { return commissions; }
    public static Ability ability() { return ability; }
    public static String abilityName() { return abilityName; }

    /** Current area / profile from the tab list, or "" when absent or stale. */
    public static String area() { return fresh(infoSeenAt) ? area : ""; }
    public static String profile() { return fresh(infoSeenAt) ? profile : ""; }
    public static List<TabParser.Forge> forges() { return fresh(forgesSeenAt) ? forges : List.of(); }
    /** Most recent parse of the tab list (for /sqol debug tab parsed); null before the first one. */
    public static TabParser.Result lastResult() { return lastResult; }

    private static boolean fresh(long seenAt) { return seenAt > 0 && System.currentTimeMillis() - seenAt <= STALE_MS; }

    /** True only while at least one powder line was read from the tab list recently. */
    public static boolean powderTracked() {
        long now = System.currentTimeMillis();
        for (long t : powderSeenAt) if (t > 0 && now - t <= STALE_MS) return true;
        return false;
    }

    public static void resetTransient() {
        for (int i = 0; i < 3; i++) { lastTotal[i] = -1; powderSeenAt[i] = 0; }
        commissionsSeenAt = infoSeenAt = forgesSeenAt = 0;
        area = profile = "";
        forges = List.of();
        lastLines = List.of();
        lastResult = null;
        commissions = List.of();
        previousDone = new HashMap<>();
        ability = Ability.UNKNOWN;
        lastActivity = System.currentTimeMillis();
    }

    /** Called ~2x per second with the current tab list. */
    public static void updateTab(List<String> lines) {
        if (!Config.get().mining.enabled) return;
        if (lastResult == null || !lines.equals(lastLines)) {
            lastResult = TabParser.parse(lines);
            lastLines = lines;
        }
        applyTab(lastResult, System.currentTimeMillis());
    }

    /** Separated from updateTab so freshness logic can be reasoned about with an explicit clock. */
    static void applyTab(TabParser.Result r, long now) {
        for (int i = 0; i < 3; i++) {
            Long total = r.powders().get(TYPES[i]);
            if (total != null) {
                // A pause or disabled tracker can skip every intermediate missing-widget update.
                if (now - powderSeenAt[i] > STALE_MS) lastTotal[i] = -1;
                powderSeenAt[i] = now;
                onPowderTotal(i, total);
            } else if (lastTotal[i] >= 0 && now - powderSeenAt[i] > STALE_MS) {
                // Widget gone: forget the baseline so that when it comes back the first reading is a fresh baseline
                // and powder earned while it was hidden is not counted as one giant jump.
                lastTotal[i] = -1;
            }
        }

        if (r.area() != null || r.profile() != null) {
            if (r.area() != null) area = r.area();
            if (r.profile() != null) profile = r.profile();
            infoSeenAt = now;
        }
        if (r.forgeHeader()) { forges = r.forges(); forgesSeenAt = now; }

        if (r.commissionHeader() || !r.commissions().isEmpty()) {
            commissionsSeenAt = now;
            detectCompletions(r.commissions());
            commissions = r.commissions();
        } else if (now - commissionsSeenAt > STALE_MS) {
            commissions = List.of();          // stale: do not keep showing old data
            previousDone = new HashMap<>();
        }
    }

    private static void onPowderTotal(int i, long total) {
        long prev = lastTotal[i];
        lastTotal[i] = total;
        if (prev < 0 || total <= prev) return;        // baseline, or powder was spent
        addPowder(i, total - prev);
    }

    private static void addPowder(int type, long n) {
        StatsManager.apply(c -> {
            switch (type) {
                case 0 -> c.mithril += n;
                case 1 -> c.gemstone += n;
                default -> c.glacite += n;
            }
        });
        lastActivity = System.currentTimeMillis();
    }

    private static void detectCompletions(List<TabParser.Commission> now) {
        Map<String, Boolean> next = new HashMap<>();
        for (TabParser.Commission c : now) {
            Boolean was = previousDone.get(c.name());
            if (was != null && !was && c.done()) completed(c.name());
            next.put(c.name(), c.done());
        }
        previousDone = next;
    }

    private static void completed(String name) {
        StatsManager.apply(c -> c.commissionsDone++);
        lastActivity = System.currentTimeMillis();
        Alerts.commission(name);
    }

    public static void onChat(String s) {
        Config.Mining cfg = Config.get().mining;
        if (!cfg.enabled) return;
        long now = System.currentTimeMillis();

        Matcher used = ABILITY_USED.matcher(s);
        if (used.find()) {
            abilityName = used.group(1);
            ability = Ability.COOLDOWN;
            StatsManager.apply(c -> c.abilityUses++);
            lastActivity = now;
            return;
        }
        Matcher ready = ABILITY_READY.matcher(s);
        if (ready.find()) {
            abilityName = ready.group(1);
            ability = Ability.READY;
            Alerts.ability(abilityName);
            return;
        }

        if (COMMISSION_CHAT.matcher(s).find() && now - commissionsSeenAt > CHAT_TRUST_MS) {
            completed("Commission");
            return;
        }
        if (cfg.chatFallback) {
            Matcher m = POWDER_CHAT.matcher(s);
            while (m.find()) {
                int type = switch (m.group(2)) { case "Mithril" -> 0; case "Gemstone" -> 1; default -> 2; };
                // Per-type: only trust chat for a powder type the tab widget is not currently supplying.
                if (now - powderSeenAt[type] <= CHAT_TRUST_MS && powderSeenAt[type] > 0) continue;
                addPowder(type, Long.parseLong(m.group(1).replace(",", "")));
            }
        }
    }
}
