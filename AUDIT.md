# 0.3.0 development audit

Implementation branch: `development/0.3.0-interface-waypoints`, based on `12fc673`.
Five phased implementation commits preserve existing history and trackers. No dependency versions
were downgraded. No Skyblocker runtime dependency or copied implementation was introduced.

## Verification

- Java 25, Minecraft 26.2, Loader 0.19.3, API 0.161.0+26.2, Loom 1.17.17, Gradle 9.6.1.
- `clean build`: successful. 16 + 43 existing checks and 40 new production-model checks pass.
- New checks execute through `milestoneTest`; the JUnit `test` task has no sources by design.
- Formatting: Google Java Format applied to modified/new Java files.
- Build verification does not establish in-game rendering or UI correctness. No in-game test performed.

## Architecture / changed files

- `command/ShornedCommand`: new router, with existing `cmd/SqolCommand` retained.
- `gui/HubScreen`, `FormScreen`, `ConfirmScreen`, `ModuleSettingsScreen`: shared navigation/forms.
- `hud/HudManager`, `HudWidget`, `WidgetConfig`, `WidgetSettingsScreen`, `HudEditorScreen`:
  modular HUD; old editor retained as `LegacyHudEditorScreen`.
- `waypoint/Waypoint`, `WaypointStore`, `WaypointManager`: independent model/storage/lifecycle.
- `WaypointRenderer`: Fabric `LevelExtractionEvents.END_EXTRACTION` and
  `LevelRenderEvents.COLLECT_SUBMITS`; extracted gizmo primitives attached to level render state.
- `WaypointNavigation`: direction/distance/arrival; never changes active waypoint.
- `WaypointExchange`, GUI manager/import screens: bounded native/Skyblocker conversion.
- `util/DebugTools`, `Config`, client initialization and Gradle checks updated.

## Review findings and safeguards

- Native decode requires explicit identity, coordinate and scope fields; invalid numbers do not become 0.
- Save validates before atomic replacement; load failure preserves the original and disables writes.
- Imported points append with regenerated IDs, preserve active selection and disable through-wall rendering.
- Both compressed input and expanded JSON are capped, and no import may exceed 500 total points.
- Scope filters require server/world, dimension and optional exact area. Hidden groups suppress navigation
  and world markers. A saved active point remains selected when outside scope.
- Draw events consume extracted primitives, not live world objects. Markers are range/count capped.
- HUD lines and measurements cache on tracker refresh; editor previews intentionally rebuild on render.
- Existing config layout remains available. Version-2 migration is idempotent and retains legacy fields.
- Confirmation callbacks and GUI validation errors are caught in-screen. Draft edits cancel without mutation.
- Directory opening uses validated real paths and argument arrays on a worker thread; no scripts/uploads.

## Known limitations / release blockers

1. No in-game validation: GUI input/resize, default placement density, widget clipping, rendering depth,
   translucency, third-person camera, large-distance labels and third-party renderer compatibility are unverified.
2. At extremely small viewports, large widgets can exceed available screen space; positions clamp but the
   text is not automatically rescaled. Overlap rejection is for manual drops, not a full layout solver.
3. Existing installs retain legacy HUD mode until explicitly switched. Some legacy screens retain their old
   fixed layout; the new primary navigation/forms/settings are paginated and support wheel paging.
4. Singleplayer scope is world-name-based. Identically named saves can collide. Hypixel area is fresh tab
   data, not a dungeon/server-instance identity; users should scope temporary locations carefully.
5. Imported Skyblocker groups bind to the current scope, not an inferred island mapping. Ordered routes,
   complex name components and all third-party render types are not reproduced. Original source metadata
   is retained in native JSON; decimal coordinates cannot be exported to Skyblocker V1.
6. No catch or Double Hook data source exists; only bites/recognized creature events are shown. No invented
   timers/profit. Full client log viewing and smooth navigation-arrow art are not implemented.
7. Legacy mining/profile freshness and chat matching issues listed in the historical audit remain future work.
8. MIT metadata still lacks an owner-confirmed LICENSE file. Resolve before release.

## Required manual tests

- Start 26.2 with the development JAR; exercise all `/shorned` routes and `/sqol` compatibility commands.
- Test tab/shift-tab, Enter/Escape, mouse-wheel paging, form resize/cancel and low/high GUI scales.
- Migrate a copied 0.2.2 config: legacy placement and patterns retained; new widget settings survive restart.
- Select/drag/scale/toggle/reset widgets, test overlap cancellation and F1/editor exclusion.
- Create/edit/search/sort/group points, test cancellation/confirmation and persistence after restart.
- Test markers in first/third person, behind terrain (hidden by default), through-wall opt-in, pillars,
  range cap, dimension/server/area changes and disabled groups.
- Navigate to 3-block radius, verify one arrival message and unchanged selection, leave 8 blocks and return.
- Test actual Skyblocker V1 shares, malformed files, native round trip, repeated imports, saved selection
  preservation, file collision avoidance and invalid existing storage.
- Export complete Hypixel tab samples; test diagnostics/folder actions and unsupported desktop fallback.

## Reference inspection

Public format inspected from upstream main on 2026-11-06:
- https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/waypoint/Waypoints.java
- https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/utils/waypoint/WaypointGroup.java
- https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/utils/waypoint/NamedWaypoint.java

Rendering APIs inspected in the exact resolved Fabric rendering source artifact
`fabric-rendering-v1:25.3.3+515ac5339e`, and API signatures from resolved Minecraft 26.2.
Older cached rendering sources were rejected as incompatible before implementation.

---

## Historical 0.2.2 audit

# Repository audit — 0.2.2 development

Audit date: 2026-11-06. Read all 21 production Java files, resources, build configuration,
documentation, test sources, fixture and tracked-file inventory before editing.

## Repository history and hygiene

The live `origin/main` contains root commit `371eb06` (added source to actual github.).
The previous local checkout was root commit `1adc142` (README only). Fetch reported an upstream
history replacement; this maintenance work starts from `371eb06` and does not rewrite history.
Work branch: `maintenance/0.2.2-audit`.

- Removed 39 generated class/resource files under `bin/main` from version control. They are editor
  build output duplicating `src`, not source dependencies. Local copies remain on disk.
- Untracked `.vscode/settings.json` (only `java.compile.nullAnalysis.mode=automatic`); local file retained.
- Added ignores and line-ending rules, including executable Unix wrapper and LF shell scripts.
- Restored official Gradle 9.6.1 wrapper scripts/JAR. Wrapper JAR SHA-256 matches Gradle's published value:
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`.
  Distribution SHA-256 is pinned in wrapper properties.
- Metadata declares MIT, but no LICENSE exists. Resolve copyright ownership/license text before release;
  this audit does not invent an attribution. No icon is supplied (not a build blocker).

## Verification performed

| Status | Evidence |
|---|---|
| Compiled successfully | `.\gradlew.bat clean build`: BUILD SUCCESSFUL, 9 tasks executed |
| Versions preserved | Minecraft 26.2, Loader 0.19.3, API 0.161.0+26.2, Loom 1.17.17, Gradle 9.6.1, Microsoft Java 25.0.4.1 |
| Tested automatically | 16 ParserTest checks + 43 TabParserSuite checks pass; wired into Gradle `check` |
| Windows standalone tests | `.\dev-tests\run.ps1` passes without Minecraft dependencies |
| JAR verified | `build/libs/shorned-qol-0.2.2.jar`: version 0.2.2, client environment, expected dependencies/entrypoint, no test stubs |
| In-game | Not performed for 0.2.2. User reports successful 0.2.1 build/smoke test; not independently reproduced |
| Unix runner | Updated to shared stubs and failure propagation; not executed on this Windows host |

The existing API replacements (`gui.setScreen`, `gui.screen`, `gui.hud.isHidden`,
`sendSystemMessage`) compile against the actual resolved Minecraft 26.2 dependencies. No additional
speculative Minecraft API substitutions were needed. Gradle's ordinary `test` task has no JUnit
sources; the separately named `parserTest` and `tabParserSuite` tasks actually execute the tests.

## Tab export and parser findings

`/sqol debug tab export` is registered and calls `TabReader.readAll` on the client thread.
The snapshot is written on a worker thread; success/failure feedback returns to the client thread.
Disconnected and empty-list cases return a message. Writes use UTF-8, local timestamps and CREATE_NEW
with collision suffixes (up to 99 attempts), so existing exports are not overwritten.

The command is implemented and compiled; writer tests verify real disk output, directory creation,
collisions, ordering, blank lines, round trips and a synthetic 80-entry list. It has no 60-line limit.
Only `/sqol debug tab` chat preview is capped at 60. This is not an in-game command execution test.

Fixed reader sorting to include server list order and spectator status ahead of team/name, checked
against the resolved 26.2 PlayerTabOverlay comparator. Missing display names now fall back to profile
names; explicitly blank display names remain blank. Added export/parsed commands to `/sqol help`.

Scope: all **listed player entries**, not tab header/footer components or unlisted players. Text
whitespace survives, but Component styling/colors and team decoration on fallback names do not.
Real ordering and interaction with other tab mods still need a live capture/smoke test.

The checked-in fixture is 59 lines of partial, anonymized Dwarven Mines data, not the complete
80-entry capture. Tests verify reported powder totals, four commissions and empty forge rows.
No new synthetic input is represented as a real capture. A new malformed-suffix regression failed
on the old parser; anchoring the full powder number fixed it. A long polling-gap regression now
verifies that stale powder baselines cannot generate a false jump when updates resume.

## Prioritized development plan

1. **Completed in this pass:** repository cleanup, wrapper restoration, honest documentation,
   compile verification, Windows/Gradle test integration, narrow export/parser/tracker fixes.
2. **Next: live 0.2.2 validation.** Test settings/HUD editor, F1 visibility, notifications,
   disconnect/reconnect, `/sqol debug tab`, `parsed`, and two rapid exports. Obtain complete exports
   from Dwarven Mines and additional mining areas, then add anonymized captured regression fixtures.
3. **Mining correctness before new features:** separate area/profile freshness (currently shared),
   re-baseline on profile changes, reconcile commissions that disappear before DONE is observed,
   harden oversized/malformed chat powder numbers, and constrain broad ability-ready chat matching.
   Use deterministic clock tests and real chat/tab evidence. Forge text is exposed but not a timer/HUD feature.
4. **Configuration and lifecycle robustness:** sanitize null regex entries/non-finite numbers,
   tighten Hypixel hostname matching (currently substring matching), clear stale HUD/alert state when
   tracking is disallowed, and verify HUD position behavior across GUI-scale changes.
5. **Fishing improvements after reliable samples:** study Feesh behavior and configuration patterns,
   then add original recognition/timing changes with tests. Obtain actual catch/chat examples first.
   Feesh/SkyBlocker/SkyHanni reference review is deferred to feature work; no code copied or dependencies added.
6. **Release gate:** in-game smoke test, license clarification, final metadata/build verification,
   then tag/publish. No release tag created in this pass. Profit estimates, waypoints, Double Powder,
   Pickobulus/corpse/excavation and Double Hook features remain future work, not implemented claims.
