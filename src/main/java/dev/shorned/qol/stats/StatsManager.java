package dev.shorned.qol.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.util.JsonFiles;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Session + lifetime statistics, persisted to config/shornedqol-stats.json. */
public final class StatsManager {
    public static class Data {
        public int version = 1;
        public Counters session = new Counters();
        public Counters lifetime = new Counters();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shornedqol-stats.json");
    private static Data data = new Data();
    private static boolean dirty;
    private static long lastSave = System.currentTimeMillis();

    private StatsManager() {}

    public static Counters session() { return data.session; }
    public static Counters lifetime() { return data.lifetime; }

    /** Applies a change to both the session and lifetime counters. */
    public static void apply(Consumer<Counters> change) {
        change.accept(data.session);
        change.accept(data.lifetime);
        dirty = true;
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                if (d.session == null) d.session = new Counters();
                if (d.lifetime == null) d.lifetime = new Counters();
                data = d;
            }
        } catch (Exception e) {
            System.err.println("[Shorned's QoL] stats file unreadable, starting fresh: " + e);
            JsonFiles.quarantine(FILE);
        }
        if (!Config.get().general.persistSession) data.session = new Counters();
    }

    public static void save() {
        if (!dirty) return;
        try {
            JsonFiles.writeAtomic(GSON, FILE, data);
            dirty = false;
        } catch (Exception e) {
            System.err.println("[Shorned's QoL] could not save stats: " + e);
        }
        lastSave = System.currentTimeMillis();
    }

    /** Called every tick; writes to disk at most once a minute. */
    public static void autosave() {
        if (dirty && System.currentTimeMillis() - lastSave > 60_000) save();
    }

    public static void resetSession() { data.session = new Counters(); dirty = true; }

    public static void resetMiningSession() {
        Counters c = data.session;
        c.miningMs = 0; c.mithril = c.gemstone = c.glacite = 0; c.commissionsDone = 0; c.abilityUses = 0;
        dirty = true;
    }

    public static void resetFishingSession() {
        Counters c = data.session;
        c.fishingMs = 0; c.bites = c.seaCreatures = c.seaKills = 0;
        dirty = true;
    }

    public static void resetLifetime() { data.lifetime = new Counters(); dirty = true; }
}
