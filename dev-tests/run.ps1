$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'build/parser-tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$sources = @(
    "$repo/src/main/java/dev/shorned/qol/track/TabParser.java"
    "$repo/src/main/java/dev/shorned/qol/track/MiningTracker.java"
    "$repo/src/main/java/dev/shorned/qol/util/TabExport.java"
)
$sources += (Get-ChildItem "$PSScriptRoot/stubs", "$PSScriptRoot/dev" -Recurse -Filter '*.java').FullName
& java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Parser test compilation failed' }
& java -cp $out dev.shorned.qol.track.ParserTest
if ($LASTEXITCODE -ne 0) { throw 'ParserTest failed' }
& java -cp $out dev.shorned.qol.track.TabParserSuite "$PSScriptRoot/resources/dwarven-mines-sample.txt"
if ($LASTEXITCODE -ne 0) { throw 'TabParserSuite failed' }
