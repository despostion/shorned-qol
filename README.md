# Shorned's QoL — 0.3.0 development

A client-side, information-only Fabric mod for Hypixel SkyBlock. No gameplay automation.
This branch introduces a unified interface, independently configurable HUD widgets and an original
waypoint system. **Development build: compiled and automatically tested, not tested in-game.**

## Build and install

Pinned environment: Minecraft **26.2**, Fabric Loader **0.19.3**, Fabric API **0.161.0+26.2**,
Loom **1.17.17**, Gradle **9.6.1**, Java **25**. The complete Gradle wrapper is included.

```powershell
java -version
.\gradlew.bat clean build
```

Install Fabric and Fabric API for this Minecraft version, then put
`build/libs/shorned-qol-0.3.0-dev.jar` in your mods folder. Do not install the sources JAR.
The first build needs network access. Do not downgrade dependencies to bypass a build failure.

## Interface and commands

| Command | Action |
|---|---|
| `/shorned` | Overview and navigation hub |
| `/shorned mining` / `fishing` | Statistics, module toggle, settings and confirmed session reset |
| `/shorned hud` | Visual widget editor |
| `/shorned waypoint` | Waypoint manager |
| `/shorned waypoint add` | Prefilled creation form at current player coordinates; coordinates are editable |
| `/shorned waypoint list` | List saved waypoints |
| `/shorned waypoint clear` | Open a deletion confirmation |
| `/shorned settings` | General settings, notifications and legacy settings |
| `/shorned debug` | Parser/tracker status, exports, folder tools and captured fishing chat |
| `/shorned help` | Command summary |

`/sqol` and its existing commands remain available, including stats/reset, fishing recognition patterns,
HUD appearance controls and `/sqol debug tab [export|parsed]`. Legacy appearance controls affect the
legacy HUD. Use the new editor for modular widgets.

The new hub and forms use consistent dark/gold styling, Back/Cancel controls, keyboard-focusable
Minecraft widgets, Previous/Next buttons and mouse-wheel paging. Form drafts survive paging and
resize; Cancel discards them. Settings changes save immediately. Overview values are snapshots when
opened; gameplay HUD values refresh approximately four times per second.

## Modular HUD

The hub's **Modular HUD** toggle selects the new widget system. Existing version-2 configurations
retain the exact old layout initially; enable the new mode when ready. Old configuration fields,
tracker settings and pattern lists are retained. New installations enable the modular system.

Widgets: Mithril/Gemstone/Glacite gained, powder rates, commissions, raw forge status, mining session,
pickaxe ability status, fishing bites/rate, sea creature counts, fishing session, coordinates, area,
connected session, current notifications and active waypoint navigation. Bites are **not catches**.
There are no invented catch, Double Hook, profit or forge countdown statistics.

In the editor, select by clicking a widget or cycling Select. Drag on a 4-pixel grid; a drop overlapping
an enabled widget is cancelled. Disabled widgets appear grey for preview and can be re-enabled through
Appearance/visibility. Each widget has its own scale, opacity, RGB text/background colors, background,
border, padding, spacing, normalized position and reset action. Positions clamp to the viewport.
The editor itself is excluded from the gameplay HUD. The legacy editor remains available.

Alerts and legacy tracker functionality remain intact. Enable Hypixel's Powder and Commissions tab
widgets for mining data. Per-hour rates use active tracker time and show unavailable for the first
30 seconds. Sea creature recognition still requires patterns; the fishing settings GUI can add/remove
those patterns. Missing forge/area/navigation data is labeled unavailable rather than fabricated.

## Waypoints

Independent of Skyblocker; no dependency added. Each saved point has a UUID, name, coordinates, RGB
color, BOX/POINT/RING marker, visibility, description, group, world/server, dimension and optional area.
Create/edit in the GUI, search names/groups, sort by name or distance, hide groups, select navigation,
stop navigation and confirm deletion. Maximum: 500 saved points. A world must be joined to prefill a
new point. Native records require world/dimension binding; blank area means any area within that scope.

World rendering uses Fabric 26.2 extraction and submit events with independent render snapshots.
Markers show distance and nearby coordinate labels; an optional 32-block pillar is available.
Depth-tested rendering is the default. Through-wall mode requires explicitly enabling it for a point.
The default range is 512 blocks, configurable from 16 to 4096, with a cap of 128 submitted points per
frame. The navigation widget shows name, distance, coordinates and relative direction in degrees.
Optional arrival messages fire within 3 blocks, re-arm beyond 8 blocks and never change selection.

**Rendering, occlusion, camera behavior and interactions with other renderers still need in-game tests.**
Area binding depends on fresh tab data. Singleplayer scope currently uses the saved world's name;
worlds with identical names should use distinct names or manually distinct scope identifiers.

## Import/export

Native format: versioned UTF-8 JSON in `config/shorned-qol/waypoints.json`. Native exports retain all
fields. Files are written atomically; invalid existing storage is preserved and saving is blocked rather
than replacing it. Imports are validated first, then appended after confirmation with fresh UUIDs.
They never overwrite existing points or change the selected waypoint. Imported through-wall mode is
always disabled. Imports are limited to 1 MB expanded data and 500 total points.

The manager's Import/export screen accepts native JSON or **Skyblocker V1** clipboard data and files
from `config/shorned-qol/imports/` (filename only). Native exports create new files under `exports/`.
Skyblocker export copies a share string to the clipboard; decimal coordinates are rejected instead of
silently rounded.

Compatibility was checked against Skyblocker's public `Waypoints`, `WaypointGroup` and `NamedWaypoint`
codecs: `[Skyblocker-Waypoint-Data-V1]` + Base64 + gzip + JSON group array. Conversion is original code.
Names, integer coordinates, RGB colors, enabled state and group names are shared. Imported groups bind
to the **current world/dimension/area**; the original island and unsupported source fields remain in native
metadata. Ordered routes are not executed; group render types are mapped to this mod's markers. Plain
names and literal text components are supported; translated names are rejected, and rich text styling
is not displayed. Skyblocker export uses independent one-point groups, no automatic route order, and
`unknown` island for points without imported island metadata. Legacy/Skytils/Coleweight formats are not
supported. No compatibility is claimed with uninspected future versions.

## Debug and files

- Existing config: `config/shornedqol.json`; statistics: `config/shornedqol-stats.json`.
- Waypoints and new tools: `config/shorned-qol/`.
- Tab exports: `config/shorned-qol/debug/`, all listed entries, no 60/80-entry cap.
- Exports retain text order/whitespace/blank names; Component styling and tab header/footer are excluded.
- Diagnostics stay local. No uploads. Folder actions accept no arbitrary path and validate real paths
  within the designated mod directory before opening Explorer or a supported platform fallback.
- Recent fishing chat is a capture around bites, **not a viewer for the complete Minecraft log**.

## Validation and remaining work

`clean build` runs 16 original parser/tracker checks, 43 extended parser/export checks and 40 milestone
checks (99 total). Milestone tests cover native persistence, invalid data, scope/group behavior, distance,
bearing, import conversion/limits, config migration, widget round trips and command parsing. Standalone
legacy parser tests remain available through `powershell -ExecutionPolicy Bypass -File .\dev-tests\run.ps1`.

No in-game validation or release tag yet. See [AUDIT.md](AUDIT.md) for the manual test matrix and known
limitations, [CHANGELOG.md](CHANGELOG.md) for changes, and [CONTEXT.md](CONTEXT.md) for maintainer continuity.
