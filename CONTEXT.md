# Working context

Updated: 2026-11-06

## Standing instructions

- Pull and push all work through https://github.com/despostion/shorned-qol (origin).
- Maintain this file to avoid repeating discovery. Read AUDIT.md for findings and the prioritized plan.
- Act on safe maintenance work, test, commit and push; PR or ordinary merge to main is authorized.
- Never force-push, rewrite history, downgrade pinned versions or claim unperformed in-game tests.
- Windows/PowerShell; use the included Gradle wrapper with Java 25.

## Current state

- Workspace: E:\shorned. Work branch: maintenance/0.2.2-audit, based on source root 371eb06.
- Initial local main at 1adc142 was README-only. Remote history had already been replaced when fetched.
  Preserve the old local branch; do not reset or force-push it.
- Inspected all production/test sources. Restored wrapper, untracked generated bin/main and local VS Code
  settings without deleting local copies, updated documentation, added shared stubs/Windows runner,
  integrated tests into Gradle, fixed tab sorting/name fallback/help, malformed powder parsing and stale baselines.
- Final `.\gradlew.bat clean build` passed: 16 + 43 checks. Standalone PowerShell runner also passed.
- JAR metadata/entrypoint checked; test stubs absent. Artifact: build/libs/shorned-qol-0.2.2.jar.
- 0.2.2 remains a development build, not tested in-game. No release tag.
- Next required evidence: full real `/sqol debug tab export` captures and an in-game smoke test.
- Fixture is a 59-line partial anonymized transcription; 80-entry export test is synthetic.
- Further mining/fishing features are deferred behind correctness work in AUDIT.md.

## Continuity

Verify Git status/remotes before editing. Keep decisions, validation and remaining work here, while
putting detailed findings in AUDIT.md. Do not store credentials or private game captures in this file.

## GitHub delivery

- Write access confirmed on 2026-11-06. The earlier HTTP 403 is resolved.
- Commits 2130cb8 (build/tests), 6d1755f (fixes/docs), and 2c180e8 (historical handoff)
  were pushed successfully to both main and maintenance/0.2.2-audit with an atomic, fast-forward push.
- Remote main now contains the verified maintenance work. No force-push or release tag was used.
- Transfer artifacts under build/handoff are historical backups; use GitHub for current source.
