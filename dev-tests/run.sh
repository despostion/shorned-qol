#!/usr/bin/env bash
# Compiles TabParser + TabExport + MiningTracker against tiny stubs (no Minecraft needed) and runs both test classes.
set -e
cd "$(dirname "$0")"
OUT=$(mktemp -d)
S=$OUT/stubs/dev/shorned/qol; mkdir -p $S/config $S/stats $S/track
cat > $S/config/Config.java <<'J'
package dev.shorned.qol.config;
public class Config { public static class Mining { public boolean enabled = true, chatFallback = true; } public Mining mining = new Mining();
  static Config i = new Config(); public static Config get() { return i; } }
J
cat > $S/stats/StatsManager.java <<'J'
package dev.shorned.qol.stats;
import java.util.function.Consumer;
public class StatsManager { public static void apply(Consumer<Counters> c) { c.accept(dev.shorned.qol.track.StubStats.c); } }
J
cat > $S/stats/Counters.java <<'J'
package dev.shorned.qol.stats;
public class Counters { public long mithril, gemstone, glacite; public int commissionsDone, abilityUses; }
J
cat > $S/track/Alerts.java <<'J'
package dev.shorned.qol.track;
public class Alerts { static void commission(String n) {} static void ability(String n) {} }
J
cat > $S/track/StubStats.java <<'J'
package dev.shorned.qol.track;
public class StubStats { public static dev.shorned.qol.stats.Counters c = new dev.shorned.qol.stats.Counters();
  public static long mithril; public static int commissions; }
J
# keep the two convenience fields in sync via a thin wrapper
sed -i 's/public static long mithril; public static int commissions;/public static long mithril() { return c.mithril; }/' $S/track/StubStats.java
SRC=../src/main/java/dev/shorned/qol/track
sed 's/StubStats\.mithril/StubStats.c.mithril/g;s/StubStats\.commissions/StubStats.c.commissionsDone/g' dev/shorned/qol/track/ParserTest.java > $OUT/ParserTest.java
java -m jdk.compiler/com.sun.tools.javac.Main -d $OUT/classes $SRC/TabParser.java $SRC/MiningTracker.java ../src/main/java/dev/shorned/qol/util/TabExport.java $OUT/ParserTest.java dev/shorned/qol/track/TabParserSuite.java $(find $OUT/stubs -name '*.java')
java -cp $OUT/classes dev.shorned.qol.track.ParserTest | tee $OUT/a.txt
echo "ParserTest: $(grep -c '^ok' $OUT/a.txt) ok, $(grep -c '^FAIL' $OUT/a.txt) failed"
java -cp $OUT/classes dev.shorned.qol.track.TabParserSuite resources/dwarven-mines-sample.txt
