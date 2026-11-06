package dev.shorned.qol.stats;

/** One set of statistics. The mod keeps two: the current session and lifetime. */
public class Counters {
    public long fishingMs, miningMs;               // ACTIVE tracking time (excludes pause/disconnect/idle)
    public long elapsedMs;                         // wall-clock time while connected and not paused
    public int bites, seaCreatures, seaKills;
    public int commissionsDone, abilityUses;
    public long mithril, gemstone, glacite;        // powder GAINED, not totals
}
