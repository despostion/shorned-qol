package dev.shorned.qol.hud;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.gui.ConfigScreen;
import dev.shorned.qol.stats.Counters;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.track.Alerts;
import dev.shorned.qol.track.FishingTracker;
import dev.shorned.qol.track.MiningTracker;
import dev.shorned.qol.track.TabParser;
import dev.shorned.qol.track.Trackers;
import dev.shorned.qol.util.Fmt;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the HUD. Row text and layout are rebuilt ~4 times a second (Trackers.tick -> rebuild) and cached,
 * so a frame only draws; no string formatting or measuring happens per frame.
 */
public final class HudRenderer {
    public enum Panel { COMBINED, MINING, FISHING }
    private enum Kind { HEADER, SECTION, STAT, BAR, TEXT }

    private record Row(Kind kind, String left, String right, int rgb, float progress) {
        int height() {
            return switch (kind) { case HEADER, SECTION -> 13; case BAR -> 14; default -> 10; };
        }
    }

    private static final class Model {
        final List<Row> rows = new ArrayList<>();
        int w, h;
    }

    private static final int BRAND = 0xFFB347, MITHRIL = 0x4FD5E0, GEMSTONE = 0xE05AE0, GLACITE = 0x9AD7FF,
            BITES = 0xFFD84F, SEA = 0x55FF55, KILLS = 0xFF7755, LABEL = 0xAAAAAA, WHITE = 0xFFFFFF,
            GOOD = 0x55FF55, WARN = 0xFFAA33;

    private static final EnumMap<Panel, Model> LIVE = new EnumMap<>(Panel.class);
    private static final EnumMap<Panel, int[]> BOUNDS = new EnumMap<>(Panel.class);
    private static final Map<String, Float> shownProgress = new HashMap<>();

    private HudRenderer() {}

    // ------------------------------------------------------------------ panels & positions

    public static List<Panel> editablePanels() {
        return Config.get().hud.mode == Config.HudMode.COMBINED
                ? List.of(Panel.COMBINED) : List.of(Panel.MINING, Panel.FISHING);
    }

    private static List<Panel> visiblePanels() {
        Config cfg = Config.get();
        boolean m = cfg.mining.enabled && cfg.hud.showMining;
        boolean f = cfg.fishing.enabled && cfg.hud.showFishing;
        List<Panel> out = new ArrayList<>(2);
        if (cfg.hud.mode == Config.HudMode.COMBINED) {
            if (m || f) out.add(Panel.COMBINED);
        } else {
            if (m) out.add(Panel.MINING);
            if (f) out.add(Panel.FISHING);
        }
        return out;
    }

    public static int[] bounds(Panel p) { return BOUNDS.get(p); }

    public static void setPos(Panel p, int x, int y) {
        Config.Hud h = Config.get().hud;
        switch (p) {
            case COMBINED -> { h.combinedX = x; h.combinedY = y; }
            case MINING -> { h.miningX = x; h.miningY = y; }
            case FISHING -> { h.fishingX = x; h.fishingY = y; }
        }
    }

    public static void resetPositions() {
        Config.Hud h = Config.get().hud;
        h.combinedX = 6; h.combinedY = 6; h.miningX = 6; h.miningY = 6; h.fishingX = 6; h.fishingY = 90;
        Config.save();
    }

    private static int[] pos(Panel p) {
        Config.Hud h = Config.get().hud;
        return switch (p) {
            case COMBINED -> new int[]{h.combinedX, h.combinedY};
            case MINING -> new int[]{h.miningX, h.miningY};
            case FISHING -> new int[]{h.fishingX, h.fishingY};
        };
    }

    // ------------------------------------------------------------------ model building

    /** Rebuilds the cached rows from live statistics. */
    public static void rebuild() {
        LIVE.clear();
        if (shownProgress.size() > 64) shownProgress.clear();
        for (Panel p : visiblePanels()) LIVE.put(p, build(p, StatsManager.session(), false));
    }

    private static Counters sample() {
        Counters c = new Counters();
        c.fishingMs = 18 * 60_000L; c.miningMs = 25 * 60_000L;
        c.bites = 42; c.seaCreatures = 7; c.seaKills = 6;
        c.mithril = 18_400; c.gemstone = 52_300; c.glacite = 3_100;
        return c;
    }

    private static Model build(Panel p, Counters c, boolean preview) {
        Config cfg = Config.get();
        boolean showM = preview || (cfg.mining.enabled && cfg.hud.showMining);
        boolean showF = preview || (cfg.fishing.enabled && cfg.hud.showFishing);
        Model m = new Model();
        switch (p) {
            case COMBINED -> {
                m.rows.add(new Row(Kind.HEADER, "Shorned's QoL", "", BRAND, 0));
                if (showM) miningRows(m.rows, c, true, preview);
                if (showF) fishingRows(m.rows, c, true, preview);
            }
            case MINING -> {
                m.rows.add(new Row(Kind.HEADER, "Shorned's QoL - Mining",
                        cfg.hud.showTimer ? timer(c.miningMs, Trackers.miningIdle(), preview) : "", BRAND, 0));
                miningRows(m.rows, c, false, preview);
            }
            case FISHING -> {
                m.rows.add(new Row(Kind.HEADER, "Shorned's QoL - Fishing",
                        cfg.hud.showTimer ? timer(c.fishingMs, Trackers.fishingIdle(), preview) : "", BRAND, 0));
                fishingRows(m.rows, c, false, preview);
            }
        }
        measure(m);
        return m;
    }

    /** Active-tracking time, flagged when the idle-pause setting has currently paused that clock. */
    private static String timer(long activeMs, boolean idle, boolean preview) {
        return Fmt.time(activeMs) + (idle && !preview ? " (idle)" : "");
    }

    private static void miningRows(List<Row> rows, Counters c, boolean section, boolean preview) {
        Config.Mining cfg = Config.get().mining;
        if (section) rows.add(new Row(Kind.SECTION, "Mining",
                Config.get().hud.showTimer ? timer(c.miningMs, Trackers.miningIdle(), preview) : "", MITHRIL, 0));

        boolean live = preview || MiningTracker.powderTracked();
        boolean hasSession = c.mithril + c.gemstone + c.glacite > 0;
        if (!live && !hasSession) {
            rows.add(new Row(Kind.TEXT, "Powder: no data (enable Powder widget)", "", LABEL, 0));
        } else {
            if (cfg.showMithril) rows.add(powder("Mithril", c.mithril, c.miningMs, MITHRIL, cfg.showPerHour));
            if (cfg.showGemstone) rows.add(powder("Gemstone", c.gemstone, c.miningMs, GEMSTONE, cfg.showPerHour));
            if (cfg.showGlacite && (preview || c.glacite > 0)) rows.add(powder("Glacite", c.glacite, c.miningMs, GLACITE, cfg.showPerHour));
            if (!live) rows.add(new Row(Kind.TEXT, "Powder widget not visible - totals paused", "", WARN, 0));
        }

        if (cfg.showCommissions) {
            List<TabParser.Commission> list = preview
                    ? List.of(new TabParser.Commission("Titanium Miner", 62, false),
                              new TabParser.Commission("Goblin Raid Slayer", 100, true))
                    : MiningTracker.commissions();
            for (TabParser.Commission cm : list) {
                String name = cm.name().length() > 20 ? cm.name().substring(0, 19) + "." : cm.name();
                rows.add(new Row(Kind.BAR, name, cm.done() ? "DONE" : Math.round(cm.percent()) + "%",
                        cm.done() ? GOOD : BRAND, (float) (cm.percent() / 100.0)));
            }
        }

        if (cfg.showAbility && (preview || MiningTracker.ability() != MiningTracker.Ability.UNKNOWN)) {
            boolean ready = preview || MiningTracker.ability() == MiningTracker.Ability.READY;
            String name = preview ? "Mining Speed Boost" : MiningTracker.abilityName();
            rows.add(new Row(Kind.TEXT, name + ": " + (ready ? "READY" : "on cooldown"), "", ready ? GOOD : WARN, 0));
        }
    }

    private static Row powder(String label, long gained, long ms, int rgb, boolean perHour) {
        String v = Fmt.num(gained) + (perHour ? "  " + Fmt.rate(gained, ms) : "");
        return new Row(Kind.STAT, label, v, rgb, 0);
    }

    private static void fishingRows(List<Row> rows, Counters c, boolean section, boolean preview) {
        Config.Fishing cfg = Config.get().fishing;
        if (section) rows.add(new Row(Kind.SECTION, "Fishing",
                Config.get().hud.showTimer ? timer(c.fishingMs, Trackers.fishingIdle(), preview) : "", BITES, 0));
        if (cfg.showBites) {
            String v = Fmt.num(c.bites) + (cfg.showPerHour ? "  " + Fmt.rate(c.bites, c.fishingMs) : "");
            rows.add(new Row(Kind.STAT, "Bites", v, BITES, 0));
        }
        boolean hasSea = preview || !cfg.spawnPatterns.isEmpty() || c.seaCreatures > 0;
        if (cfg.showSeaCreatures && hasSea) rows.add(new Row(Kind.STAT, "Sea creatures", Fmt.num(c.seaCreatures), SEA, 0));
        boolean hasKill = preview || !cfg.killPatterns.isEmpty() || c.seaKills > 0;
        if (cfg.showKills && hasKill) rows.add(new Row(Kind.STAT, "Kills", Fmt.num(c.seaKills), KILLS, 0));
    }

    private static void measure(Model m) {
        Font f = Minecraft.getInstance().font;
        int w = 90, h = 8;
        for (Row r : m.rows) {
            int rw = switch (r.kind()) {
                case TEXT -> f.width(r.left());
                case BAR -> Math.max(f.width(r.left()) + 12 + f.width(r.right()), 100);
                default -> f.width(r.left()) + 14 + f.width(r.right());
            };
            w = Math.max(w, rw);
            h += r.height();
        }
        m.w = w + 10;
        m.h = h;
    }

    // ------------------------------------------------------------------ drawing

    /** Registered HUD element. */
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = Config.get();
        if (!cfg.general.enabled || mc.gui.hud.isHidden() || mc.player == null) return;
        if (mc.gui.screen() instanceof HudEditorScreen) return;           // the editor draws its own preview

        if (cfg.hud.enabled) {
            BOUNDS.clear();
            for (Map.Entry<Panel, Model> e : LIVE.entrySet()) drawModel(g, mc, e.getKey(), e.getValue());
        }
        drawAlerts(g, mc);
    }

    /** Used by the HUD editor: draws every panel with sample numbers so positions can be judged. */
    public static void renderPreview(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        BOUNDS.clear();
        for (Panel p : editablePanels()) drawModel(g, mc, p, build(p, sample(), true));
    }

    private static void drawModel(GuiGraphicsExtractor g, Minecraft mc, Panel p, Model model) {
        Config cfg = Config.get();
        Font f = mc.font;
        float s = cfg.hud.scale;
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int pw = Math.round(model.w * s), ph = Math.round(model.h * s);
        int[] pos = pos(p);
        int x = Math.max(0, Math.min(pos[0], Math.max(0, sw - pw)));
        int y = Math.max(0, Math.min(pos[1], Math.max(0, sh - ph)));
        BOUNDS.put(p, new int[]{x, y, pw, ph});

        int bgAlpha = cfg.hud.opacity * 255 / 100;
        int bg = Draw.argb(bgAlpha, 0x101018);
        int border = Draw.argb(Math.min(255, bgAlpha + 50), 0x6E5A3A);

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);
        Draw.panel(g, model.w, model.h, bg, border, cfg.hud.rounded, cfg.hud.border);

        int ry = 4;
        int right = model.w - 5;
        for (Row r : model.rows) {
            int color = 0xFF000000 | r.rgb();
            switch (r.kind()) {
                case HEADER, SECTION -> {
                    g.text(f, r.left(), 5, ry, color, true);
                    if (!r.right().isEmpty()) g.text(f, r.right(), right - f.width(r.right()), ry, 0xFFAAAAAA, true);
                    g.fill(5, ry + 10, right, ry + 11, 0x33FFFFFF);
                }
                case STAT -> {
                    g.text(f, r.left(), 5, ry, 0xFFAAAAAA, true);
                    g.text(f, r.right(), right - f.width(r.right()), ry, color, true);
                }
                case TEXT -> g.text(f, r.left(), 5, ry, color, true);
                case BAR -> {
                    g.text(f, r.left(), 5, ry, 0xFFFFFFFF, true);
                    g.text(f, r.right(), right - f.width(r.right()), ry, color, true);
                    float target = Math.max(0f, Math.min(1f, r.progress()));
                    float shown = target;
                    if (cfg.hud.animations) {
                        String key = p.name() + r.left();
                        shown = shownProgress.getOrDefault(key, target);
                        shown += (target - shown) * 0.15f;
                        shownProgress.put(key, shown);
                    }
                    int barW = right - 5;
                    g.fill(5, ry + 10, right, ry + 12, 0x55000000);
                    g.fill(5, ry + 10, 5 + Math.round(barW * shown), ry + 12, 0xFF000000 | r.rgb());
                }
            }
            ry += r.height();
        }
        g.pose().popMatrix();
    }

    private static void drawAlerts(GuiGraphicsExtractor g, Minecraft mc) {
        Config cfg = Config.get();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();

        float flash = Alerts.flash();
        if (flash > 0f) {
            int a = (int) (flash * 70);
            if (a >= 4) g.fill(0, 0, sw, sh, Draw.argb(a, 0xFF2020));
        }
        Alerts.Banner bite = Alerts.biteBanner();
        if (bite != null) drawBanner(g, mc, bite, sw / 2, sh / 2 - 50, cfg.hud.animations);
        Alerts.Banner info = Alerts.infoBanner();
        if (info != null) drawBanner(g, mc, info, sw / 2, sh / 2 - 20, cfg.hud.animations);
    }

    private static void drawBanner(GuiGraphicsExtractor g, Minecraft mc, Alerts.Banner b, int cx, int y, boolean animate) {
        float alpha = Alerts.fade(b, animate);
        int a = (int) (alpha * 255);
        if (a < 5) return;
        float scale = b.big() ? 2.5f : 1.3f;
        float slide = animate ? (1f - alpha) * -6f : 0f;
        Font f = mc.font;
        g.pose().pushMatrix();
        g.pose().translate(cx, y + slide);
        g.pose().scale(scale, scale);
        g.text(f, b.text(), -f.width(b.text()) / 2, 0, Draw.argb(a, b.rgb()), true);
        g.pose().popMatrix();
    }

    /** Opens the config screen on the next tick (opening it inside a command is swallowed by the chat screen closing). */
    public static void openConfigLater() {
        dev.shorned.qol.ShornedQolClient.pendingScreen = new ConfigScreen(null);
    }
}
