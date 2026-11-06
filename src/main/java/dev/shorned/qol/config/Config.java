package dev.shorned.qol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.shorned.qol.util.JsonFiles;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * All user settings, stored in config/shornedqol.json. Missing fields keep their defaults,
 * out-of-range values are clamped, and an unreadable file is renamed to *.corrupt-<time> and
 * replaced with defaults instead of crashing the game.
 */
public class Config {
  public enum HudMode {
    COMBINED,
    SEPARATE
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static Path file() {
    return FabricLoader.getInstance().getConfigDir().resolve("shornedqol.json");
  }

  private static Config instance = new Config();

  /** Bumped on every save so cached regexes/HUD models know to rebuild. */
  public static volatile int generation = 0;

  public static boolean recoveredFromCorruption = false;

  public int configVersion = 2;
  public boolean modularHud = true;
  public java.util.Map<String, dev.shorned.qol.hud.WidgetConfig> widgets =
      new java.util.LinkedHashMap<>();
  public General general = new General();
  public Hud hud = new Hud();
  public Mining mining = new Mining();
  public Fishing fishing = new Fishing();
  public Notifications notifications = new Notifications();

  public static class General {
    public boolean enabled = true;
    public boolean onlyOnHypixel = true;

    /** Pause a tracker's clock after this many seconds without a tracked event. 0 = never pause. */
    public int idleTimeoutSeconds = 0;

    /** Keep the current session's numbers across game restarts. Lifetime stats always persist. */
    public boolean persistSession = false;
  }

  public static class Hud {
    public boolean enabled = true;
    public HudMode mode = HudMode.COMBINED;
    public float scale = 1.0f;
    public int opacity = 60; // background opacity 0-100
    public boolean rounded = true;
    public boolean border = true;
    public boolean showTimer = true;
    public boolean animations = true;
    public boolean showMining = true;
    public boolean showFishing = true;
    public int combinedX = 6, combinedY = 6;
    public int miningX = 6, miningY = 6;
    public int fishingX = 6, fishingY = 90;
  }

  public static class Mining {
    public boolean enabled = true;
    public boolean showMithril = true;
    public boolean showGemstone = true;
    public boolean showGlacite = true;
    public boolean showPerHour = true;
    public boolean showCommissions = true;
    public boolean showAbility = true;

    /** Count "+N X Powder" chat lines when the tab-list Powder widget is not available. */
    public boolean chatFallback = true;
  }

  public static class Fishing {
    public boolean enabled = true;
    public boolean biteDetection = true;
    public boolean showBites = true;
    public boolean showPerHour = true;
    public boolean showSeaCreatures = true;
    public boolean showKills = true;

    /** A "!!!" label only counts if it is within this many blocks of YOUR bobber. */
    public double maxBobberDistance = 3.0;

    /** Regexes matched against chat lines. A match counts one sea creature spawn. */
    public List<String> spawnPatterns = new ArrayList<>();

    /** Regexes matched against chat lines. A match counts one sea creature kill. */
    public List<String> killPatterns = new ArrayList<>();
  }

  public static class Notifications {
    public boolean biteSound = true;
    public int biteSoundType = 0; // index into Alerts.SOUND_NAMES
    public int volume = 100; // 0-100
    public boolean biteBanner = true;
    public boolean screenFlash = false;
    public boolean abilitySound = true;
    public boolean abilityBanner = true;
    public boolean commissionSound = true;
    public boolean commissionBanner = true;
  }

  public static Config get() {
    return instance;
  }

  public static void load() {
    if (!Files.exists(file())) {
      instance = new Config();
      instance.configVersion = 3;
      save();
      return;
    }
    try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
      Config c = GSON.fromJson(r, Config.class);
      instance = c == null ? new Config() : c;
      instance.sanitize();
    } catch (Exception e) {
      System.err.println("[Shorned's QoL] config unreadable, using defaults: " + e);
      JsonFiles.quarantine(file());
      instance = new Config();
      recoveredFromCorruption = true;
    }
    save();
  }

  public static void save() {
    generation++;
    try {
      JsonFiles.writeAtomic(GSON, file(), instance);
    } catch (Exception e) {
      System.err.println("[Shorned's QoL] could not save config: " + e);
    }
  }

  /** Repairs nulls and out-of-range numbers (hand-edited or older files). */
  public void sanitize() {
    if (general == null) general = new General();
    if (hud == null) hud = new Hud();
    if (mining == null) mining = new Mining();
    if (fishing == null) fishing = new Fishing();
    if (notifications == null) notifications = new Notifications();
    if (hud.mode == null) hud.mode = HudMode.COMBINED;
    if (fishing.spawnPatterns == null) fishing.spawnPatterns = new ArrayList<>();
    if (fishing.killPatterns == null) fishing.killPatterns = new ArrayList<>();
    fishing.spawnPatterns.removeIf(java.util.Objects::isNull);
    fishing.killPatterns.removeIf(java.util.Objects::isNull);
    if (!Float.isFinite(hud.scale)) hud.scale = 1;
    if (!Double.isFinite(fishing.maxBobberDistance)) fishing.maxBobberDistance = 3;
    hud.scale = Math.max(0.5f, Math.min(3.0f, hud.scale));
    hud.opacity = Math.max(0, Math.min(100, hud.opacity));
    notifications.volume = Math.max(0, Math.min(100, notifications.volume));
    notifications.biteSoundType = Math.max(0, Math.min(4, notifications.biteSoundType));
    general.idleTimeoutSeconds = Math.max(0, Math.min(3600, general.idleTimeoutSeconds));
    fishing.maxBobberDistance = Math.max(1.0, Math.min(10.0, fishing.maxBobberDistance));
    if (widgets == null) widgets = new java.util.LinkedHashMap<>();
    widgets.values().removeIf(java.util.Objects::isNull);
    widgets.values().forEach(dev.shorned.qol.hud.WidgetConfig::sanitize);
    // Retain the exact legacy layout for existing installations until they opt into widgets.
    if (configVersion < 3) modularHud = false;
    configVersion = 3;
  }
}
