# Working context — 0.3.0 development

Updated: 2026-11-06. Repository: https://github.com/despostion/shorned-qol.
Workspace E:\shorned; Windows/PowerShell; Java 25. Read AUDIT.md/README.md for current behavior.

## Current instruction / scope

Implement the user's five-phase interface/HUD/waypoint milestone on
`development/0.3.0-interface-waypoints`. **Do not merge this untested development implementation into main.**
Preserve versions/history/data; no force pushes. Keep `/sqol` compatibility and use `/shorned` as primary.

## Completed

- Phase 1: `8b33024` router, hub, debug/tools.
- Phase 2: `92dfc9b` modular widgets/editor and legacy migration.
- Phase 3: `504cd50` independent waypoint model/persistence/manager.
- Phase 4: `9be047b` world render snapshots and HUD navigation.
- Phase 5: compatibility conversion, paged settings, safe imports, tests/docs and review polish.
- Final clean build passes; 99 checks (16+43+40). No in-game test, no release tag.
- Artifact: build/libs/shorned-qol-0.3.0-dev.jar. API versions unchanged.
- Whole brief preserved in user attachment; implementation details and manual test matrix in AUDIT.md.

## Next work

In-game smoke tests and real captured imports/tab data, then fix observed UI/rendering issues before release.
Known limitations include small viewport clipping, world-name singleplayer scope, partial Skyblocker field
mapping and pre-existing tracker issues. No catch/Double Hook/profit figures are fabricated.

GitHub write access is working. Push only the development branch, and use a draft PR against main.
The old local main at 1adc142 is historical and diverges from origin/main; do not reset or force-push it.

## Delivery

- Phase 5 commit: 2ba3704. All five implementation commits pushed to the development branch.
- Draft PR: https://github.com/despostion/shorned-qol/pull/1 (do not merge before manual validation).
- Main was not changed by this milestone.
- Verified development JAR SHA-256: 91eccb2d1f01d2db35fae17f1e35515cb19bee9741e5ca745cd7ea4490ee9a08.
