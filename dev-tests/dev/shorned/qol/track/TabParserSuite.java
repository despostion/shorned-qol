package dev.shorned.qol.track;

import dev.shorned.qol.util.TabExport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tab export + parser tests (plain Java, no Minecraft). Run via dev-tests/run.sh. */
public class TabParserSuite {
    static int fails = 0, total = 0;
    static void check(boolean ok, String what) { total++; if (!ok) { fails++; System.out.println("FAIL: " + what); } else System.out.println("ok:   " + what); }
    static List<String> L(String... s) { return List.of(s); }

    public static void main(String[] args) throws Exception {
        // ---- sample
        List<String> sample = Files.readAllLines(Path.of(args[0]));
        var r = TabParser.parse(sample);
        check("Dwarven Mines".equals(r.area()), "sample: area");
        check("Lime".equals(r.profile()), "sample: profile");
        check(r.powders().equals(java.util.Map.of("Mithril", 16510L, "Gemstone", 153532L, "Glacite", 525683L)), "sample: powder with thousands separators");
        check(r.commissions().size() == 4 && r.commissions().stream().allMatch(c -> c.percent() == 0 && !c.done()), "sample: four 0% commissions");
        check(r.commissions().stream().anyMatch(c -> c.name().equals("Royal Mines Titanium")), "sample: commission names kept whole");
        check(r.forges().size() == 5 && r.forges().stream().allMatch(TabParser.Forge::empty), "sample: five EMPTY forge rows (duplicate '1)' numbers kept as seen)");
        check(r.commissions().stream().noneMatch(c -> c.name().equals("Fetchur") || c.name().equals("Puzzler")), "sample: Daily Quests lines are not commissions");
        check(r.powderHeader() && r.commissionHeader() && r.forgeHeader(), "sample: headers detected");

        // ---- export format
        LocalDateTime t = LocalDateTime.of(2026, 11, 6, 0, 21, 0);
        check(TabExport.filename(t, 1).equals("tab-list-2026-11-06-002100.txt"), "export: timestamped filename");
        check(TabExport.filename(t, 2).equals("tab-list-2026-11-06-002100-2.txt"), "export: collision suffix");
        List<String> withBlanks = L("Players (3)", "", "a", "", "", "Info");
        check(TabExport.parse(TabExport.format(withBlanks, t)).equals(withBlanks), "export: round trip keeps order and blank lines");
        check(TabExport.parse(TabExport.format(List.of(), t)).isEmpty(), "export: zero lines");
        check(TabExport.parse(TabExport.format(L(""), t)).equals(L("")), "export: single blank line");
        check(TabExport.parse(TabExport.format(L("a\nb"), t)).equals(L("a b")), "export: embedded newline cannot split an entry");
        check(TabExport.format(sample, t).contains("# Lines: " + sample.size()), "export: header line count");
        List<String> eighty = new ArrayList<>(); for (int i = 0; i < 80; i++) eighty.add(i % 7 == 0 ? "" : "line " + i);
        check(TabExport.parse(TabExport.format(eighty, t)).equals(eighty), "export: all 80 lines (not just 60)");
        check(TabParser.parse(TabExport.parse(TabExport.format(sample, t))).powders().size() == 3, "export: exported file re-parses identically");
        Path dir = Files.createTempDirectory("tabexp").resolve("nested/debug");
        Path f1 = TabExport.write(dir, t, sample), f2 = TabExport.write(dir, t, sample);
        check(!f1.equals(f2) && Files.exists(f1) && Files.exists(f2), "export: same-second exports never overwrite; directories created");
        check(TabExport.parse(Files.readString(f1)).equals(sample), "export: file on disk matches");

        // ---- parser edge cases
        check(TabParser.parse(L("", "", "")).powders().isEmpty(), "empty lines only");
        var miss = TabParser.parse(L("Powders:", "Mithril: 5"));
        check(miss.powders().size() == 1 && miss.commissions().isEmpty() && miss.forges().isEmpty() && miss.area() == null, "missing sections do not throw");
        check(TabParser.parse(L("Area:", "Profile:   ")).area() == null, "missing area/profile values ignored");
        check(TabParser.parse(L("Powders:", "Mithril:", "Gemstone: ")).powders().isEmpty(), "powder with missing value ignored");
        check(TabParser.parse(L("Powders:", "Mithril: 1,234,567")).powders().get("Mithril") == 1234567L, "multi-group commas");
        check(TabParser.parse(L("Powders:", "Mithril: 1,23")).powders().isEmpty(), "malformed grouping rejected, not half-read");
        check(TabParser.parse(L("Powders:", "Mithril: abc", "Gemstone: -5", "Glacite: 1.5.2")).powders().isEmpty(), "invalid numbers (text, negative, 1.5.2) produce no value");
        check(TabParser.parse(L("Commissions:", "A: 45.5%")).commissions().get(0).percent() == 45.5, "decimal commission percent");
        check(TabParser.parse(L("Commissions:", "A: abc%", "B: 10%")).commissions().size() == 1, "invalid commission skipped, valid kept");

        // duplicate headers + reordering
        var dup = TabParser.parse(L("Commissions:", "A: 10%", "Info", "Commissions:", "A: 30%", "B: 5%"));
        check(dup.commissions().size() == 2 && dup.commissions().get(0).percent() == 30, "duplicate Commissions sections merge, higher progress wins");
        var dupForge = TabParser.parse(L("Forges:", "1) EMPTY", "Forges:", "1) Something"));
        check(dupForge.forges().size() == 1 && !dupForge.forges().get(0).empty(), "duplicate Forges: last block wins, non-EMPTY state kept raw");
        var reordered = TabParser.parse(L("Powders:", "Glacite: 9", "Commissions:", "X: 1%", "Forges:", "1) EMPTY", "Info", "Area: Crystal Hollows"));
        check(reordered.powders().get("Glacite") == 9 && reordered.commissions().size() == 1 && reordered.forges().size() == 1 && "Crystal Hollows".equals(reordered.area()), "reordered sections");
        check(TabParser.parse(L("Powders:", "Mithril: 5", "Info", "Mithril: 99")).powders().get("Mithril") == 5, "column title closes a section (no leak into later lines)");
        check(TabParser.parse(L("Daily Quests:", "Raffle: 50%")).commissions().isEmpty(), "percent lines outside Commissions are ignored");
        check(TabParser.parse(L("Commissions:", "Raffle: 50%")).commissions().size() == 1, "same line inside Commissions is accepted");

        // ---- partial updates and stale data via the tracker
        long t0 = 5_000_000;
        MiningTracker.resetTransient();
        MiningTracker.applyTab(TabParser.parse(L("Info", "Area: Dwarven Mines", "Profile: Lime")), System.currentTimeMillis());
        check("Dwarven Mines".equals(MiningTracker.area()) && "Lime".equals(MiningTracker.profile()), "tracker: area/profile fresh");
        MiningTracker.applyTab(TabParser.parse(L("Powders:", "Mithril: 10")), System.currentTimeMillis());
        check("Dwarven Mines".equals(MiningTracker.area()), "partial update (no Info lines) keeps last area");
        MiningTracker.applyTab(TabParser.parse(L("Info", "Area:")), System.currentTimeMillis());
        check("Dwarven Mines".equals(MiningTracker.area()), "invalid/missing area does not overwrite valid one");
        MiningTracker.resetTransient();
        MiningTracker.applyTab(TabParser.parse(L("Area: Old Place", "Forges:", "1) EMPTY")), t0);   // reading from long ago
        check(MiningTracker.area().isEmpty() && MiningTracker.forges().isEmpty(), "stale area/forges are not reported as current");
        MiningTracker.applyTab(TabParser.parse(L("Commissions:", "Z: 5%")), t0);
        MiningTracker.applyTab(TabParser.parse(L()), t0 + 60_000);
        check(MiningTracker.commissions().isEmpty(), "stale commissions cleared");
        MiningTracker.applyTab(TabParser.parse(L("Commissions:")), t0 + 61_000);
        check(MiningTracker.commissions().isEmpty(), "commission header with no entries is fresh-empty, not an error");

        MiningTracker.resetTransient();
        long before = StubStats.c.mithril;
        MiningTracker.applyTab(TabParser.parse(L("Powders:", "Mithril: 100")), t0);
        MiningTracker.applyTab(TabParser.parse(L("Powders:", "Mithril: 900")), t0 + 60_000);
        check(StubStats.c.mithril == before, "tracker: long polling gap re-baselines without an intermediate empty update");
        MiningTracker.applyTab(TabParser.parse(L("Powders:", "Mithril: 910")), t0 + 61_000);
        check(StubStats.c.mithril == before + 10, "tracker: gains resume after a polling gap");

        check(TabParser.parse(L("Powders:", "Mithril: 123abc", "Gemstone: 12,", "Glacite: 1,234,56")).powders().isEmpty(),
                "parser: malformed numeric suffixes are rejected");

        System.out.println("TabParserSuite: " + (total - fails) + "/" + total + " passed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
