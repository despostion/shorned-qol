package dev.shorned.qol.gui;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.HudEditorScreen;
import dev.shorned.qol.hud.HudRenderer;
import dev.shorned.qol.stats.Counters;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.track.Alerts;
import dev.shorned.qol.util.Fmt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** In-game settings. Every click saves immediately, so nothing is lost if the game closes. */
public class ConfigScreen extends Screen {
    private enum Cat {
        GENERAL("General"), HUD("HUD"), MINING("Mining"), FISHING("Fishing"),
        NOTIFICATIONS("Notifications"), STATISTICS("Statistics");
        final String label;
        Cat(String label) { this.label = label; }
    }

    private static Cat current = Cat.GENERAL;
    private final Screen parent;
    private int slot, left, colW, top;
    private boolean confirmLifetime;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Shorned's QoL Mod"));
        this.parent = parent;
    }

    public ConfigScreen(Screen parent, String category) { this(parent); current = Cat.valueOf(category.toUpperCase(java.util.Locale.ROOT)); }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        slot = 0;
        left = 130;
        colW = Math.max(100, Math.min(165, (this.width - left - 14) / 2));
        top = current == Cat.STATISTICS ? 150 : 40;

        int y = 36;
        for (Cat cat : Cat.values()) {
            final Cat target = cat;
            Button b = Button.builder(Component.literal((cat == current ? "> " : "") + cat.label), btn -> {
                current = target;
                confirmLifetime = false;
                this.rebuildWidgets();
            }).bounds(10, y, 110, 20).build();
            this.addRenderableWidget(b);
            y += 24;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(10, this.height - 28, 110, 20).build());

        Config c = Config.get();
        switch (current) {
            case GENERAL -> {
                toggle("Mod enabled", () -> c.general.enabled, v -> c.general.enabled = v);
                toggle("Only on Hypixel", () -> c.general.onlyOnHypixel, v -> c.general.onlyOnHypixel = v);
                cycleInt("Idle pause", new int[]{0, 60, 120, 300}, () -> c.general.idleTimeoutSeconds,
                        v -> c.general.idleTimeoutSeconds = v, v -> v == 0 ? "Off" : v + "s");
                toggle("Keep session on restart", () -> c.general.persistSession, v -> c.general.persistSession = v);
            }
            case HUD -> {
                toggle("HUD", () -> c.hud.enabled, v -> c.hud.enabled = v);
                action("Layout: " + c.hud.mode, () -> {
                    c.hud.mode = c.hud.mode == Config.HudMode.COMBINED ? Config.HudMode.SEPARATE : Config.HudMode.COMBINED;
                });
                cycleScale();
                cycleInt("Opacity", new int[]{0, 25, 40, 60, 80, 100}, () -> c.hud.opacity, v -> c.hud.opacity = v, v -> v + "%");
                toggle("Rounded corners", () -> c.hud.rounded, v -> c.hud.rounded = v);
                toggle("Border", () -> c.hud.border, v -> c.hud.border = v);
                toggle("Show timers", () -> c.hud.showTimer, v -> c.hud.showTimer = v);
                toggle("Animations", () -> c.hud.animations, v -> c.hud.animations = v);
                toggle("Mining section", () -> c.hud.showMining, v -> c.hud.showMining = v);
                toggle("Fishing section", () -> c.hud.showFishing, v -> c.hud.showFishing = v);
                plain("Move / preview HUD", () -> this.minecraft.gui.setScreen(new HudEditorScreen(this)));
            }
            case MINING -> {
                toggle("Mining tracker", () -> c.mining.enabled, v -> c.mining.enabled = v);
                toggle("Mithril Powder", () -> c.mining.showMithril, v -> c.mining.showMithril = v);
                toggle("Gemstone Powder", () -> c.mining.showGemstone, v -> c.mining.showGemstone = v);
                toggle("Glacite Powder", () -> c.mining.showGlacite, v -> c.mining.showGlacite = v);
                toggle("Per-hour rates", () -> c.mining.showPerHour, v -> c.mining.showPerHour = v);
                toggle("Commissions", () -> c.mining.showCommissions, v -> c.mining.showCommissions = v);
                toggle("Pickaxe ability", () -> c.mining.showAbility, v -> c.mining.showAbility = v);
                toggle("Chat powder fallback", () -> c.mining.chatFallback, v -> c.mining.chatFallback = v);
            }
            case FISHING -> {
                toggle("Fishing tracker", () -> c.fishing.enabled, v -> c.fishing.enabled = v);
                toggle("Bite detection", () -> c.fishing.biteDetection, v -> c.fishing.biteDetection = v);
                toggle("Show bites", () -> c.fishing.showBites, v -> c.fishing.showBites = v);
                toggle("Per-hour rates", () -> c.fishing.showPerHour, v -> c.fishing.showPerHour = v);
                toggle("Sea creatures", () -> c.fishing.showSeaCreatures, v -> c.fishing.showSeaCreatures = v);
                toggle("Sea creature kills", () -> c.fishing.showKills, v -> c.fishing.showKills = v);
                cycleDouble("Bobber range", new double[]{2, 3, 4, 6}, () -> c.fishing.maxBobberDistance,
                        v -> c.fishing.maxBobberDistance = v);
            }
            case NOTIFICATIONS -> {
                Config.Notifications n = c.notifications;
                toggle("Bite sound", () -> n.biteSound, v -> n.biteSound = v);
                action("Sound: " + Alerts.SOUND_NAMES[n.biteSoundType], () -> {
                    n.biteSoundType = (n.biteSoundType + 1) % Alerts.SOUND_NAMES.length;
                    Alerts.play(n.biteSoundType);
                });
                cycleInt("Volume", new int[]{0, 25, 50, 75, 100}, () -> n.volume, v -> n.volume = v, v -> v + "%");
                toggle("Bite banner", () -> n.biteBanner, v -> n.biteBanner = v);
                toggle("Screen flash", () -> n.screenFlash, v -> n.screenFlash = v);
                toggle("Ability sound", () -> n.abilitySound, v -> n.abilitySound = v);
                toggle("Ability banner", () -> n.abilityBanner, v -> n.abilityBanner = v);
                toggle("Commission sound", () -> n.commissionSound, v -> n.commissionSound = v);
                toggle("Commission banner", () -> n.commissionBanner, v -> n.commissionBanner = v);
                plain("Test notification", Alerts::test);
            }
            case STATISTICS -> {
                plain("Reset session", () -> { StatsManager.resetSession(); changed(); });
                plain("Reset mining session", () -> { StatsManager.resetMiningSession(); changed(); });
                plain("Reset fishing session", () -> { StatsManager.resetFishingSession(); changed(); });
                plain(confirmLifetime ? "Click again to CONFIRM" : "Reset lifetime...", () -> {
                    if (confirmLifetime) { StatsManager.resetLifetime(); confirmLifetime = false; }
                    else confirmLifetime = true;
                    changed();
                });
            }
        }
    }

    // ------------------------------------------------------------------ widget helpers

    private void addEntry(String text, Runnable onClick) {
        int col = slot % 2, row = slot / 2;
        slot++;
        this.addRenderableWidget(Button.builder(Component.literal(text), b -> onClick.run())
                .bounds(left + col * (colW + 6), top + row * 24, colW, 20).build());
    }

    private void changed() {
        Config.save();
        HudRenderer.rebuild();
        this.rebuildWidgets();
    }

    private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        addEntry(label + ": " + (get.getAsBoolean() ? "ON" : "OFF"), () -> { set.accept(!get.getAsBoolean()); changed(); });
    }

    /** A button whose label already reflects state; the click mutates config then refreshes. */
    private void action(String label, Runnable mutate) {
        addEntry(label, () -> { mutate.run(); changed(); });
    }

    /** A button that just runs something (no config change). */
    private void plain(String label, Runnable run) {
        addEntry(label, run);
    }

    private void cycleInt(String label, int[] values, IntSupplier get, IntConsumer set, IntFunction<String> fmt) {
        addEntry(label + ": " + fmt.apply(get.getAsInt()), () -> {
            int idx = 0;
            for (int i = 0; i < values.length; i++) if (values[i] == get.getAsInt()) idx = i;
            set.accept(values[(idx + 1) % values.length]);
            changed();
        });
    }

    private void cycleDouble(String label, double[] values, DoubleSupplier get, Consumer<Double> set) {
        addEntry(label + ": " + (int) get.getAsDouble() + " blocks", () -> {
            int idx = 0;
            for (int i = 0; i < values.length; i++) if (values[i] == get.getAsDouble()) idx = i;
            set.accept(values[(idx + 1) % values.length]);
            changed();
        });
    }

    private void cycleScale() {
        float[] values = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        Config.Hud h = Config.get().hud;
        addEntry("Scale: " + h.scale + "x", () -> {
            int idx = 0;
            for (int i = 0; i < values.length; i++) if (Math.abs(values[i] - h.scale) < 0.01f) idx = i;
            h.scale = values[(idx + 1) % values.length];
            changed();
        });
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.text(this.font, "Shorned's QoL Mod", 10, 10, 0xFFFFB347, true);
        g.text(this.font, "by Shorned", 10, 21, 0xFFAAAAAA, true);
        g.text(this.font, current.label, left, 24, 0xFFFFFFFF, true);
        if (current == Cat.STATISTICS) drawStats(g);
    }

    private void drawStats(GuiGraphicsExtractor g) {
        Counters s = StatsManager.session(), l = StatsManager.lifetime();
        int x2 = left + colW + 6;
        g.text(this.font, "Session", left, 40, 0xFFFFD24D, true);
        g.text(this.font, "Lifetime", x2, 40, 0xFFFFD24D, true);
        String[] names = {"Mining time", "Fishing time", "Bites", "Sea creatures", "Kills",
                "Mithril Powder", "Gemstone Powder", "Glacite Powder", "Commissions"};
        String[] a = {Fmt.time(s.miningMs), Fmt.time(s.fishingMs), Fmt.num(s.bites), Fmt.num(s.seaCreatures), Fmt.num(s.seaKills),
                Fmt.num(s.mithril), Fmt.num(s.gemstone), Fmt.num(s.glacite), Fmt.num(s.commissionsDone)};
        String[] b = {Fmt.time(l.miningMs), Fmt.time(l.fishingMs), Fmt.num(l.bites), Fmt.num(l.seaCreatures), Fmt.num(l.seaKills),
                Fmt.num(l.mithril), Fmt.num(l.gemstone), Fmt.num(l.glacite), Fmt.num(l.commissionsDone)};
        for (int i = 0; i < names.length; i++) {
            int y = 52 + i * 10;
            g.text(this.font, names[i] + ": " + a[i], left, y, 0xFFFFFFFF, true);
            g.text(this.font, names[i] + ": " + b[i], x2, y, 0xFFAAAAAA, true);
        }
    }

    @Override
    public void onClose() {
        Config.save();
        this.minecraft.gui.setScreen(parent);
    }
}
