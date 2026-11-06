# Source audit (Phase 1) - v0.2.0 -> 0.2.1

Verified by reading all 19 Java files, build.gradle, gradle.properties, settings.gradle, fabric.mod.json.
Architecture matches the handoff: config (Config), stats (Counters/StatsManager), track (Trackers orchestrates
FishingTracker, MiningTracker, TabReader, ChatRouter, Alerts), hud (HudRenderer, Draw, HudEditorScreen), gui
(ConfigScreen), cmd (SqolCommand), util. The reported features are present in code; none could be run.

## Build status: UNVERIFIED
No compile was possible: the sandbox had no network (maven.fabricmc.net unreachable), only a Java 21 runtime
(Java 25 required), and no Gradle. Unverified guesses that need a real build to confirm:
- Loom plugin id `net.fabricmc.fabric-loom` / version `1.17-SNAPSHOT`, Fabric API `0.161.0+26.2`
- Every Minecraft 26.2 API call (`GuiGraphicsExtractor.text/fill/pose`, `HudElementRegistry`, `MouseButtonEvent`,
  `Player.fishing`, `Level.entitiesForRendering`, `PlayerInfo.getTeam/getProfile().name()`, `ServerData.ip`,
  `SimpleSoundInstance.forUI`, `Screen.extractRenderState`). I did not change any of them.
- The Gradle wrapper is only partly present (properties file; no jar/scripts).

## Findings and status
| Area | Finding | Status |
|---|---|---|
| MiningTracker | `powderTracked()` was true forever after first reading | Fixed (10 s freshness, per type, tested) |
| MiningTracker | Widget returning after absence produced a giant "gain" | Fixed (re-baseline, tested) |
| MiningTracker | Chat fallback decided globally, not per powder type | Fixed |
| TabReader/Parser | No normalisation of codes/whitespace; duplicates; `0` vs missing | Fixed in TabParser (tested) |
| TabReader/Parser | Formats still inferred, not observed | Open: needs `/sqol debug tab` output from SkyBlock |
| FishingTracker | Own-bobber only: already enforced via `player.fishing` + distance check | Kept |
| FishingTracker | No cooldown against label re-creation | Fixed (0.8 s) |
| Timers | Idle timeout defaults to 0, so both clocks run whenever connected, even if you only mine/fish. "Active time" is really connected time by default | Partly: elapsed added, "(idle)" shown; default left unchanged. Consider a 120 s default |
| Stats | `Counters` ints can overflow only after ~2 billion events; corrupted file is quarantined | OK |
| Commissions | If a finished commission vanishes from the widget before it reads DONE, completion is missed and chat is ignored while the widget is fresh | Open |
| HUD | Positions stored in GUI-scaled pixels, so they shift if GUI scale changes (clamped on draw) | Open |
| Commands | Brigadier rejects bad numeric args with usage errors; regex is validated; no crashes found by reading | Not run in-game |
| Packaging | No LICENSE file though metadata says MIT; no icon | Open |
