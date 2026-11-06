package dev.shorned.qol.track;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.stats.StatsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Fishing statistics.
 *
 * Bite detection: Hypixel floats a "!!!" label near your bobber on a bite. A bite is counted once per label
 * entity (by entity id), and only if the label is within a few blocks of YOUR bobber, so other players'
 * bites and a label that lingers for many ticks never double count.
 *
 * Sea creatures: matched against chat with user-supplied regexes (see /sqol fishing).
 */
public final class FishingTracker {
    private static final Set<Integer> seen = new HashSet<>();
    private static final Set<Integer> present = new HashSet<>();
    private static final ArrayDeque<String> recent = new ArrayDeque<>();
    private static long recentUntil = 0;
    public static long lastActivity = System.currentTimeMillis();
    /** Two "!!!" labels closer together than this are one bite (label re-created / entity id churn). */
    private static final long BITE_COOLDOWN_MS = 800;
    private static long lastBiteAt = 0;

    private static int compiledGen = -1;
    private static List<Pattern> spawn = List.of();
    private static List<Pattern> kill = List.of();

    private FishingTracker() {}

    public static void resetTransient() {
        seen.clear();
        present.clear();
        recent.clear();
        lastBiteAt = 0;
        lastActivity = System.currentTimeMillis();
    }

    public static List<String> recentLines() { return new ArrayList<>(recent); }

    /** Runs every client tick, but only does work while you actually have a bobber out. */
    public static void tick(Minecraft mc) {
        Config.Fishing cfg = Config.get().fishing;
        if (!cfg.enabled || !cfg.biteDetection || mc.level == null || mc.player == null) return;

        var hook = mc.player.fishing;           // your own bobber, null when not fishing
        if (hook == null) { seen.clear(); return; }

        double maxSq = cfg.maxBobberDistance * cfg.maxBobberDistance;
        present.clear();
        for (Entity e : mc.level.entitiesForRendering()) {
            Component name = e.getCustomName();
            if (name == null || !"!!!".equals(name.getString().strip())) continue;
            if (hook.distanceToSqr(e) > maxSq) continue;
            present.add(e.getId());
            if (seen.add(e.getId()) && System.currentTimeMillis() - lastBiteAt >= BITE_COOLDOWN_MS) onBite();
        }
        seen.retainAll(present);                // forget labels that disappeared
    }

    private static void onBite() {
        lastBiteAt = System.currentTimeMillis();
        StatsManager.apply(c -> c.bites++);
        lastActivity = System.currentTimeMillis();
        recentUntil = lastActivity + 30_000;
        recent.clear();
        Alerts.bite();
    }

    public static void onChat(String s) {
        Config.Fishing cfg = Config.get().fishing;
        if (!cfg.enabled) return;
        long now = System.currentTimeMillis();

        if (now < recentUntil) {                // remember what chat said right after a bite (for /sqol fishing recent)
            if (recent.size() >= 20) recent.removeFirst();
            recent.addLast(s);
        }

        ensurePatterns();
        for (Pattern p : spawn) {
            if (p.matcher(s).find()) {
                StatsManager.apply(c -> c.seaCreatures++);
                lastActivity = now;
                break;
            }
        }
        for (Pattern p : kill) {
            if (p.matcher(s).find()) {
                StatsManager.apply(c -> c.seaKills++);
                lastActivity = now;
                break;
            }
        }
    }

    private static void ensurePatterns() {
        if (compiledGen == Config.generation) return;
        spawn = compile(Config.get().fishing.spawnPatterns);
        kill = compile(Config.get().fishing.killPatterns);
        compiledGen = Config.generation;
    }

    private static List<Pattern> compile(List<String> regexes) {
        List<Pattern> out = new ArrayList<>();
        for (String r : regexes) {
            try { out.add(Pattern.compile(r)); } catch (PatternSyntaxException ignored) { /* bad regex in config */ }
        }
        return out;
    }
}
