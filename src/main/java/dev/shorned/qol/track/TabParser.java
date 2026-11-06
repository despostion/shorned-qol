package dev.shorned.qol.track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text parser for the SkyBlock tab list. No Minecraft dependencies, so it is unit-tested outside the game
 * (dev-tests/).
 *
 * Formats are based on one real Dwarven Mines capture plus the earlier inferred patterns. Anything not matching
 * a known shape is ignored rather than guessed. Nothing here depends on line indexes.
 *
 * Section model: a line of the form "Name:" (nothing after the colon) opens a section. The section lasts until the
 * next such header or a recognised column title ("Info", "Players (N)"). Blank lines do NOT close a section,
 * because it is not known whether blanks are real separators or column padding.
 * Key/value lines ("Area: ...", "Profile: ...") are accepted anywhere.
 */
public final class TabParser {
    public record Commission(String name, double percent, boolean done) {}

    /** One forge row. {@code state} is the raw text after "N)" - only "EMPTY" has been observed. */
    public record Forge(int slot, String state) {
        public boolean empty() { return state.equalsIgnoreCase("EMPTY"); }
    }

    /**
     * @param powders     totals for powder types actually present (absent = not showing; a present 0 is real)
     * @param area        e.g. "Dwarven Mines", or null when no Area line was found
     * @param profile     e.g. "Lime", or null when no Profile line was found
     * @param forges      forge rows in tab order (empty when the Forges section is missing)
     */
    public record Result(Map<String, Long> powders, List<Commission> commissions,
                         boolean powderHeader, boolean commissionHeader,
                         String area, String profile, List<Forge> forges, boolean forgeHeader) {}

    private static final Pattern FORMAT_CODES = Pattern.compile("\u00A7.");
    private static final Pattern ODD_SPACE = Pattern.compile("[\\u00A0\\u2007\\u202F\\u200B\\u200C\\u200D\\uFEFF]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern HEADER = Pattern.compile("^([A-Za-z' ]+):$");
    private static final Pattern COLUMN_TITLE = Pattern.compile("^(?:Info|Players \\(\\d+\\))$");
    // 16,510 | 525683 ; rejects malformed groupings such as "1,23" instead of reading part of them.
    private static final Pattern POWDER = Pattern.compile(
            "^(Mithril|Gemstone|Glacite)(?: Powder)?:\\s*(\\d{1,3}(?:,\\d{3})+|\\d+)(?![\\d,]*\\d|\\.\\d)");
    private static final Pattern COMMISSION = Pattern.compile(
            "^(.+?):\\s*(?:(\\d+(?:\\.\\d+)?)\\s*%|(DONE))\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_AREA = Pattern.compile("^Area:\\s*(\\S.*)$");
    private static final Pattern KEY_PROFILE = Pattern.compile("^Profile:\\s*(\\S.*)$");
    private static final Pattern FORGE = Pattern.compile("^(\\d{1,2})\\)\\s*(\\S.*)$");
    private static final int MAX_VALUE_LEN = 64;

    private TabParser() {}

    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = FORMAT_CODES.matcher(raw).replaceAll("");
        s = ODD_SPACE.matcher(s).replaceAll(" ");
        s = MULTI_SPACE.matcher(s.strip()).replaceAll(" ");
        return s.strip();
    }

    public static Result parse(List<String> lines) {
        Map<String, Long> powders = new LinkedHashMap<>();
        Map<String, Commission> commissions = new LinkedHashMap<>();   // de-dupes repeated names
        List<Forge> forges = new ArrayList<>();
        boolean powderHeader = false, commissionHeader = false, forgeHeader = false;
        String area = null, profile = null;
        String section = "";

        for (String raw : lines) {
            String l = normalize(raw);
            if (l.isEmpty()) continue;

            if (COLUMN_TITLE.matcher(l).matches()) { section = ""; continue; }

            Matcher h = HEADER.matcher(l);
            if (h.matches()) {
                section = h.group(1).toLowerCase(Locale.ROOT).strip();
                if (section.startsWith("powder")) powderHeader = true;
                else if (section.equals("commissions")) commissionHeader = true;
                else if (section.equals("forges")) { forgeHeader = true; forges.clear(); }   // last Forges block wins
                continue;
            }

            Matcher k = KEY_AREA.matcher(l);
            if (k.matches()) { area = clip(k.group(1)); continue; }
            k = KEY_PROFILE.matcher(l);
            if (k.matches()) { profile = clip(k.group(1)); continue; }

            Matcher p = POWDER.matcher(l);
            if (p.find() && (section.startsWith("powder") || l.contains("Powder"))) {
                try {
                    powders.put(p.group(1), Long.parseLong(p.group(2).replace(",", "")));
                } catch (NumberFormatException ignored) { /* absurdly long number: skip the line */ }
                continue;
            }

            if (section.equals("commissions")) {
                Matcher c = COMMISSION.matcher(l);
                if (c.matches()) {
                    boolean done = c.group(3) != null;
                    double pct = done ? 100 : Math.max(0, Math.min(100, Double.parseDouble(c.group(2))));
                    String name = c.group(1).strip();
                    Commission prev = commissions.get(name);
                    if (prev == null || pct > prev.percent()) commissions.put(name, new Commission(name, pct, done || pct >= 100));
                }
            } else if (section.equals("forges")) {
                Matcher f = FORGE.matcher(l);
                if (f.matches()) forges.add(new Forge(Integer.parseInt(f.group(1)), clip(f.group(2))));
            }
        }
        return new Result(powders, new ArrayList<>(commissions.values()), powderHeader, commissionHeader,
                area, profile, forges, forgeHeader);
    }

    private static String clip(String s) {
        String t = s.strip();
        return t.length() > MAX_VALUE_LEN ? t.substring(0, MAX_VALUE_LEN) : t;
    }
}
