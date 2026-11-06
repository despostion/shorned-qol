# Shorned's QoL Mod

Made by **Shorned**. A client-side Fabric mod for Hypixel SkyBlock (Minecraft 26.2, Fabric Loader 0.19.3+).
Information only: it reads chat, the tab list and nearby entity labels, then shows stats and alerts.
It never clicks, moves, or plays for you.

## Install
1. Install Fabric Loader 0.19.3+ for Minecraft 26.2 and Fabric API.
2. Put `shorned-qol-<version>.jar` in `.minecraft/mods`.

## Build
**0.2.2 development build: compiled and automatically tested on Windows with Java 25. Not yet tested in-game.**
The user reports that 0.2.1 compiled and passed an in-game smoke test; that does not validate 0.2.2.

Install JDK 25 and make sure `java -version` reports 25. From the repository root in PowerShell:

```powershell
.\gradlew.bat clean build
```

The complete wrapper is included; no global Gradle installation is needed. The first build requires network
access. Pinned versions: Minecraft 26.2, Loader 0.19.3, Fabric API 0.161.0+26.2, Loom 1.17.17,
Gradle 9.6.1. Do not replace these versions merely to work around build failures.
Output: `build/libs/shorned-qol-0.2.2.jar` (the `-sources.jar` is for development, not installation).
The build runs the dependency-free parser/tracker/export checks as part of `check`.

## Required Hypixel setup (important)
Powder totals and commissions come from Hypixel's tab-list widgets, so enable them in SkyBlock:
`/widgets` (or SkyBlock Menu > Settings > Personal > User Interface > Player List Info) and turn on
**Powders** and **Commissions**. The reader uses network-listed player entries, not pixels from the tab overlay.
If data is missing, compare an export with other tab-mod features disabled to diagnose interference. Without the widgets the HUD says "no data" instead of showing zeros.

## Commands
| Command | What it does |
|---|---|
| `/sqol` | Version and on/off status |
| `/sqol help` | Command list |
| `/sqol config` | Open the settings screen |
| `/sqol hud` | Toggle the HUD |
| `/sqol hud edit` | Drag-to-move editor with sample data (preview mode) |
| `/sqol hud mode combined\|separate` | One panel, or separate Mining and Fishing panels |
| `/sqol hud scale <0.5-3>` / `opacity <0-100>` / `reset` | Appearance and positions |
| `/sqol mining` / `/sqol fishing` | Toggle a tracker |
| `/sqol mining reset` / `/sqol fishing reset` | Reset that tracker's session |
| `/sqol stats [session\|lifetime]` | Print statistics |
| `/sqol reset [session\|mining\|fishing]` | Reset session stats |
| `/sqol reset lifetime confirm` | Erase lifetime stats |
| `/sqol pos <x> <y>` / `pos mining\|fishing <x> <y>` | Set HUD position |
| `/sqol bite` / `/sqol ability` | Toggle those alerts |
| `/sqol fishing recent` | Chat lines seen in the 30 s after your last bite |
| `/sqol fishing pattern add spawn\|kill <regex>` | Add a sea creature pattern |
| `/sqol fishing pattern list` / `clear spawn\|kill` | Manage patterns |
| `/sqol debug tab` | Print up to 60 tab lines in chat |
| `/sqol debug tab export` | Save every tab line to `config/shorned-qol/debug/tab-list-<timestamp>.txt` |
| `/sqol debug tab parsed` | Show what the parser extracted (area, profile, powders, commissions, forges) |

## Features
**Fishing:** bite detection (counted once per "!!!" label, only near *your* bobber), configurable sound
(5 choices) and volume, BITE! banner, optional screen flash, bites and bites/hr, sea creature spawn and kill
counters from your own chat patterns.
**Mining:** Mithril/Gemstone/Glacite Powder gained and per hour (from the tab Powder widget, chat fallback),
commission progress bars and completion alerts (tab Commissions widget), pickaxe ability ready/used status.
**General:** combined or separate HUD, scale, opacity, rounded corners, border, timers, animations, drag editor,
lifetime statistics saved across restarts, optional session persistence, atomic saves, corrupt files quarantined not crashed.

## Files
`config/shornedqol.json` (settings) and `config/shornedqol-stats.json` (statistics).
Settings: General (enabled, only on Hypixel, idle pause, keep session), HUD (enabled, mode, scale, opacity,
rounded, border, timers, animations, section toggles, positions), Mining, Fishing (incl. `spawnPatterns`,
`killPatterns`, `maxBobberDistance`), Notifications (sounds, volume, banners, flash). All are also in the
in-game screen except the pattern lists, which use the `/sqol fishing pattern` commands.

## Setting up the sea creature counter
The counters stay hidden until you add patterns. Catch a sea creature, run `/sqol fishing recent`, then add a
regex for the line, e.g. `/sqol fishing pattern add spawn ^You caught a Squid`. Use `kill` for kill messages.

## Time tracking
`/sqol stats` shows *active* and *elapsed (connected)* time separately; the HUD timer is active time and shows
"(idle)" when idle-pause has stopped it. With idle pause off (default) active time runs whenever you are connected.
Timers count *active* time only: not while paused, disconnected, or (if "Idle pause" is set) after N seconds
with no tracked event. Per-hour rates show `--` for the first 30 seconds.

## Tests
`./gradlew.bat clean build` runs all 59 plain-Java checks alongside compilation. To run only these checks
without downloading Minecraft dependencies, use `powershell -ExecutionPolicy Bypass -File .\dev-tests\run.ps1`
on Windows, or `bash dev-tests/run.sh` on Linux/macOS with Bash. Both runners use shared test stubs
(Java 21+); the mod itself requires Java 25. These tests do not simulate a running Minecraft client.

The existing Dwarven Mines fixture is a partial, anonymized transcription with 59 lines, not a complete
80-entry export. It includes the reported powder/commission rows. The 80-line export test is synthetic.
Please provide a new `/sqol debug tab export` capture to validate real entry ordering and additional formats.
Exports keep all listed entries, blank display names and whitespace, with profile-name fallback where no
display name exists. They use vanilla 26.2 entry ordering and have no 60/80-entry cap. Component colors,
header/footer text and unlisted network players are not included. Files use UTF-8 and a local timestamp.

## NOT verified / incomplete (please read)
- **Not tested in-game on 0.2.2.** Compilation verifies API names, not rendering, events or live Hypixel behavior.
  Smoke-test configuration/HUD editing, alerts, reconnects and `/sqol debug tab export` before release.
- **Live parsing coverage is limited.** The checked-in partial sample and synthetic edge cases pass; additional
  areas, active forge slots and full real exports still need validation.
- **Chat texts are best guesses:** "X is now available!", "You used your X Pickaxe Ability!", "Commission Complete!".
- **Mining Fatigue timer: not implemented.** I found no reliable source for it, and you asked not to display
  guesses. **Ability cooldown countdown: not implemented** for the same reason; only ready/used is shown.
- **Sea creature counters need your patterns** (above); I do not know Hypixel's exact messages.
- Mod Menu integration is not included; use `/sqol config`.
