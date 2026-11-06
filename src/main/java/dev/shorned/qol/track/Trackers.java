package dev.shorned.qol.track;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.HudRenderer;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.util.Hypixel;
import net.minecraft.client.Minecraft;

/** Per-tick orchestration: active-time clocks, bite scan, tab parsing, HUD cache refresh, autosave. */
public final class Trackers {
    private static long lastNanos = 0;
    private static int ticks = 0;

    private Trackers() {}

    public static void onJoin() {
        MiningTracker.resetTransient();
        FishingTracker.resetTransient();
        lastNanos = 0;
    }

    public static void onLeave() {
        lastNanos = 0;                                       // so the time spent disconnected is never counted
        MiningTracker.resetTransient();
        FishingTracker.resetTransient();
    }

    public static void tick(Minecraft mc) {
        long nowNs = System.nanoTime();
        long dt = lastNanos == 0 ? 0 : Math.min((nowNs - lastNanos) / 1_000_000L, 100);
        lastNanos = nowNs;

        StatsManager.autosave();

        Config cfg = Config.get();
        boolean active = cfg.general.enabled && mc.level != null && mc.player != null
                && !mc.isPaused() && Hypixel.allowed(mc);
        if (!active) return;

        ticks++;
        FishingTracker.tick(mc);

        if (ticks % 10 == 0) MiningTracker.updateTab(TabReader.read(mc));

        if (dt > 0) {
            StatsManager.apply(c -> c.elapsedMs += dt);
            if (cfg.fishing.enabled && !idle(FishingTracker.lastActivity)) StatsManager.apply(c -> c.fishingMs += dt);
            if (cfg.mining.enabled && !idle(MiningTracker.lastActivity)) StatsManager.apply(c -> c.miningMs += dt);
        }

        if (ticks % 5 == 0) HudRenderer.rebuild();
    }

    public static boolean miningIdle() { return idle(MiningTracker.lastActivity); }
    public static boolean fishingIdle() { return idle(FishingTracker.lastActivity); }

    private static boolean idle(long lastActivity) {
        int timeout = Config.get().general.idleTimeoutSeconds;
        return timeout > 0 && System.currentTimeMillis() - lastActivity > timeout * 1000L;
    }
}
