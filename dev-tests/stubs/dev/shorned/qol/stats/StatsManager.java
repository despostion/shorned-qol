package dev.shorned.qol.stats;
import java.util.function.Consumer;
public class StatsManager { public static void apply(Consumer<Counters> c) { c.accept(dev.shorned.qol.track.StubStats.c); } }
