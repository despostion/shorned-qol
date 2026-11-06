#!/usr/bin/env bash
# Dependency-free parser/tracker/export tests; same sources as run.ps1.
set -euo pipefail
cd "$(dirname "$0")"
OUT=../build/parser-tests
mkdir -p "$OUT"
mapfile -t SOURCES < <(find stubs dev -name '*.java')
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d "$OUT" \
  ../src/main/java/dev/shorned/qol/track/TabParser.java \
  ../src/main/java/dev/shorned/qol/track/MiningTracker.java \
  ../src/main/java/dev/shorned/qol/util/TabExport.java "${SOURCES[@]}"
java -cp "$OUT" dev.shorned.qol.track.ParserTest
java -cp "$OUT" dev.shorned.qol.track.TabParserSuite resources/dwarven-mines-sample.txt
