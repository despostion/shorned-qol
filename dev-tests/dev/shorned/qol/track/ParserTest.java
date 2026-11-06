package dev.shorned.qol.track;

import java.util.List;

/** Plain-Java checks (no JUnit, no Minecraft). Run with dev-tests/run.sh. Exits non-zero on failure. */
public class ParserTest {
    static int fails = 0;
    static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL: " + what); } else System.out.println("ok:   " + what); }

    public static void main(String[] a) {
        var r = TabParser.parse(List.of("\u00A76\u00A7lPowders:", " \u00A7aMithril: \u00A7f12,345", "Gemstone:  0", "Glacite Powder: 7",
                "Commissions:", " Goblin Raid Slayer: 45%", "  Titanium Miner:\u00A0DONE", "Titanium Miner: 20%", "Junk line"));
        check(r.powderHeader() && r.commissionHeader(), "headers found through formatting codes");
        check(r.powders().get("Mithril") == 12345L, "mithril parsed with codes + commas");
        check(r.powders().get("Gemstone") == 0L, "present zero is distinct from absent");
        check(r.powders().get("Glacite") == 7L, "'Glacite Powder:' header-less form");
        check(r.commissions().size() == 2, "duplicate commission names merged");
        check(r.commissions().stream().anyMatch(c -> c.name().equals("Titanium Miner") && c.done()), "DONE wins over lower duplicate");
        check(TabParser.parse(List.of("Players (80)", "Mithril: 55")).powders().isEmpty(), "Mithril outside a Powder section is ignored");
        check(TabParser.parse(List.of()).powders().isEmpty(), "empty tab list is safe");
        check(TabParser.parse(List.of("Commissions:", "Bad: 250%")).commissions().get(0).percent() == 100, "percent clamped");

        // freshness
        long t = 1_000_000;
        MiningTracker.applyTab(TabParser.parse(List.of("Powders:", "Mithril: 100")), t);
        check(StubStats.mithril == 0, "first reading is baseline only");
        MiningTracker.applyTab(TabParser.parse(List.of("Powders:", "Mithril: 150")), t + 1000);
        check(StubStats.mithril == 50, "increase counted");
        MiningTracker.applyTab(TabParser.parse(List.of()), t + 5000);
        check(true, "widget missing <10s: still within freshness window");
        MiningTracker.applyTab(TabParser.parse(List.of()), t + 20_000);
        MiningTracker.applyTab(TabParser.parse(List.of("Powders:", "Mithril: 900")), t + 21_000);
        check(StubStats.mithril == 50, "reappearing widget re-baselines (no giant jump)");
        MiningTracker.applyTab(TabParser.parse(List.of("Powders:", "Mithril: 910")), t + 22_000);
        check(StubStats.mithril == 60, "tracking resumes after re-baseline");

        MiningTracker.applyTab(TabParser.parse(List.of("Commissions:", "Goblin: 90%")), t + 23_000);
        MiningTracker.applyTab(TabParser.parse(List.of("Commissions:", "Goblin: DONE")), t + 24_000);
        check(StubStats.commissions == 1, "commission completion counted once");
        MiningTracker.applyTab(TabParser.parse(List.of("Commissions:", "Goblin: DONE")), t + 25_000);
        check(StubStats.commissions == 1, "repeat DONE update not double counted");
        System.exit(fails == 0 ? 0 : 1);
    }
}
