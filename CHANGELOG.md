# Changelog

## 0.2.2 (tests pass outside Minecraft; the Minecraft-facing code is NOT compiled or tested in game)
- `/sqol debug tab export`: writes every tab-list entry (order and blank lines kept, plain text) to
  `config/shorned-qol/debug/tab-list-<yyyy-MM-dd-HHmmss>.txt`. File I/O runs on a worker thread; the chat message
  with the path is posted back on the client thread. Same-second exports get a `-2`, `-3` suffix, never overwrite.
- `/sqol debug tab parsed`: prints what the parser extracted (area, profile, powders, commissions, forges).
- `/sqol debug tab` is unchanged.
- TabParser now reads Area, Profile and Forges rows, treats "Info" / "Players (N)" as column titles that close a
  section, and rejects malformed numbers ("1,23", "1.5.2", negatives) instead of half-reading them.
- MiningTracker exposes area/profile/forges with the same 10 s freshness rule, and skips re-parsing when the tab
  list has not changed between refreshes.
- Tests: 16 existing (unchanged) + 40 new, see dev-tests/.
- Build config set to the versions you reported (Loader 0.19.3, Loom 1.17.17, Gradle 9.6.1 wrapper URL).

## 0.2.1 (unreleased, NOT build-verified against Minecraft 26.2)
Reliability pass on tracking. See AUDIT.md for the full findings.
- Powder: `powderTracked()` now means "read from the tab list in the last 10 s", tracked per powder type. When the
  widget disappears the baseline is dropped, so powder earned while it was hidden is never counted as one jump
  when it returns. HUD shows "Powder widget not visible - totals paused" instead of silently presenting stale data.
- Tab parsing moved to a dependency-free `TabParser`: strips formatting codes, non-breaking/zero-width spaces and
  repeated whitespace, merges duplicate commission names, clamps percentages, and distinguishes a real `0` from a
  missing line. Line formats are still inferred (not confirmed against live Hypixel).
- Chat powder fallback is now decided per powder type instead of globally.
- Fishing: 0.8 s bite cooldown on top of the per-entity de-duplication; label text is trimmed before matching.
- Timers: added wall-clock `elapsedMs` (connected, unpaused) next to active time; `/sqol stats` prints both;
  the HUD marks a clock "(idle)" when idle-pause has stopped it.
- HUD editor: dragging can no longer store negative positions.
- Added `dev-tests/` (plain-Java parser and freshness tests, run on Java 21 without Minecraft).

## 0.2.0
Modular rewrite of the prototype (see README).
