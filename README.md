# Shorned's QoL Mod

Made by **Shorned**. A client-side Fabric mod for Hypixel SkyBlock (Minecraft 26.2, Fabric Loader 0.20.1+).
Information only: it reads chat, the tab list and nearby entity labels, then shows stats and alerts.
It never clicks, moves, or plays for you.

## Install
1. Install Fabric Loader 0.20.1+ for Minecraft 26.2 and Fabric API.
2. Put `shorned-qol-<version>.jar` in `.minecraft/mods`.

## Build
**Status: v0.2.1 has not been compiled.** See AUDIT.md. Needs JDK 25. The Gradle wrapper is incomplete (only
`gradle-wrapper.properties`; the jar and scripts could not be generated offline).
Create it once with Gradle 9.x installed, or copy `gradlew`, `gradlew.bat` and `gradle/wrapper/` from the
Fabric example mod for 26.2:

    gradle wrapper
    ./gradlew build        # jar -> build/libs/

Versions are in `gradle.properties` (`fabric_version`, `loom_version`). If Gradle cannot resolve them,
take the newest 26.2 values from https://fabricmc.net/develop .

## Required Hypixel setup (important)
Powder totals and commissions come from Hypixel's tab-list widgets, so enable them in SkyBlock:
`/widgets` (or SkyBlock Menu > Settings > Personal > User Interface > Player List Info) and turn on
**Powders** and **Commissions**. If you use another mod's "fancy tab", disable it, because this mod
needs the default tab list. Without the widgets the HUD says "no data" instead of showing zeros.

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
session and lifetime statistics saved across restarts, atomic saves, corrupt files quarantined not crashed.

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
`dev-tests/run.sh` runs plain-Java tests of the tab parser and powder/commission freshness logic (Java 21+, no Minecraft).

## NOT verified / incomplete (please read)
- **Not compiled against Minecraft 26.2.** The code was syntax-checked and the regexes tested, but I had no
  network or Minecraft jars. Expect to fix a few API-name errors on first build; send them to me.
  Highest-risk calls: `GuiGraphicsExtractor.text/fill/pose()`, `MouseButtonEvent` mouse handlers, `ServerData.ip`,
  `PlayerInfo.getTeam()/getProfile().name()`, `Player.fishing`, `SimpleSoundInstance.forUI`, `SoundEvents.*`.
- **Tab line formats are inferred** from how other mods describe the Powder and Commissions widgets. If numbers
  stay at "no data", run `/sqol debug tab` and send me the output to fix the patterns.
- **Chat texts are best guesses:** "X is now available!", "You used your X Pickaxe Ability!", "Commission Complete!".
- **Mining Fatigue timer: not implemented.** I found no reliable source for it, and you asked not to display
  guesses. **Ability cooldown countdown: not implemented** for the same reason; only ready/used is shown.
- **Sea creature counters need your patterns** (above); I do not know Hypixel's exact messages.
- Mod Menu integration is not included; use `/sqol config`.
