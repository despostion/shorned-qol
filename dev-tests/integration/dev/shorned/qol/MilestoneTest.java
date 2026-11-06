package dev.shorned.qol;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.WidgetConfig;
import dev.shorned.qol.waypoint.*;
import java.nio.file.*;
import java.util.*;

public final class MilestoneTest {
  private static int checks;

  private static void check(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
    System.out.println("ok: " + message);
  }

  private interface Job {
    void run() throws Exception;
  }

  private static void reject(Job j, String name) throws Exception {
    boolean rejected = false;
    try {
      j.run();
    } catch (Exception e) {
      rejected = true;
    }
    check(rejected, name);
  }

  public static void main(String[] args) throws Exception {
    var w = new Waypoint();
    w.world = "test.example";
    w.x = 3;
    w.y = 4;
    w.name = "Caf\u00e9 mining";
    w.validate();
    check(w.distance(0, 0, 0) == 5, "3D distance");
    w.x = 0;
    w.z = 10;
    check(w.bearing(0, 0, 0) == 0, "south is forward at yaw zero");
    w.x = 10;
    w.z = 0;
    check(w.bearing(0, 0, 0) == -90, "east is left at yaw zero");
    check(w.bearing(0, 0, 270) == 0, "yaw wrapping");
    check(w.matches("test.example", "minecraft:overworld", "any"), "blank area permits any area");
    check(!w.matches("other", "minecraft:overworld", "any"), "world isolation");
    check(!w.matches("test.example", "minecraft:the_nether", "any"), "dimension isolation");
    w.area = "Mines";
    check(!w.matches("test.example", "minecraft:overworld", "Hub"), "area isolation");
    var d = new WaypointStore.Data();
    d.waypoints.add(w);
    d.active = w.id;
    var copy = WaypointStore.decode(WaypointStore.JSON.toJson(d));
    check(
        copy.waypoints.getFirst().name.equals(w.name) && copy.active.equals(w.id),
        "native serialization round trip");
    var dir = Files.createTempDirectory("shorned-tests");
    var file = dir.resolve("waypoints.json");
    try {
      WaypointStore.save(file, d);
      check(WaypointStore.load(file).waypoints.getFirst().name.equals(w.name), "UTF-8 persistence");
      String before = Files.readString(file);
      var broken = WaypointStore.copy(d);
      broken.waypoints.getFirst().x = Double.NaN;
      reject(() -> WaypointStore.save(file, broken), "non-finite coordinate rejected");
      check(Files.readString(file).equals(before), "invalid save leaves file intact");
      Files.writeString(file, "bad json");
      reject(() -> WaypointStore.load(file), "invalid JSON rejected");
      check(Files.readString(file).equals("bad json"), "invalid file preserved");
    } finally {
      Files.deleteIfExists(file);
      Files.deleteIfExists(dir);
    }
    reject(() -> WaypointStore.decode("null"), "null document");
    reject(() -> WaypointStore.decode("{\"version\":99}"), "unknown version");
    var duplicate = WaypointStore.copy(d);
    duplicate.waypoints.add(duplicate.waypoints.getFirst());
    reject(() -> WaypointStore.validate(duplicate), "duplicate IDs rejected");
    var outOfBounds = WaypointStore.copy(d);
    outOfBounds.waypoints.getFirst().x = 30000001;
    reject(() -> WaypointStore.validate(outOfBounds), "coordinate bounds");
    d.hiddenGroups.add(w.group);
    check(!WaypointStore.visible(d, w, w.world, w.dimension, w.area), "group hides member");
    var merged = WaypointStore.append(d, d);
    check(
        merged.waypoints.size() == 2
            && !merged.waypoints.get(0).id.equals(merged.waypoints.get(1).id),
        "import appends with new IDs");
    check(
        !merged.waypoints.get(1).enabled && merged.active.equals(d.active),
        "import preserves hidden source and active selection");
    d.hiddenGroups.clear();
    String exchange = WaypointExchange.skyblocker(d);
    var imported = WaypointExchange.read(exchange, w.world, w.dimension, w.area);
    check(
        imported.waypoints.size() == 1 && imported.waypoints.getFirst().color == w.color,
        "Skyblocker V1 color/coordinate conversion");
    check(imported.waypoints.getFirst().source != null, "compatibility source metadata retained");
    check(imported.waypoints.getFirst().name.equals(w.name), "Unicode compatibility name");
    reject(
        () -> WaypointExchange.read(WaypointExchange.PREFIX + "bad", w.world, w.dimension, w.area),
        "invalid compressed input");
    reject(
        () ->
            WaypointExchange.read(
                "x".repeat(WaypointStore.MAX_BYTES + 1), w.world, w.dimension, w.area),
        "input size bounded");
    w.x = .5;
    reject(() -> WaypointExchange.skyblocker(d), "lossy decimal export rejected");
    w.x = 10;
    var old =
        WaypointStore.JSON.fromJson(
            "{\"configVersion\":2,\"hud\":{\"combinedX\":91},\"fishing\":{\"spawnPatterns\":[\"Squid\"]}}",
            Config.class);
    old.sanitize();
    check(
        old.configVersion == 3 && !old.modularHud && old.hud.combinedX == 91,
        "legacy migration keeps exact layout");
    check(old.fishing.spawnPatterns.contains("Squid"), "migration keeps recognition patterns");
    old.modularHud = true;
    old.sanitize();
    check(old.modularHud, "migration is idempotent");
    var c = new WidgetConfig();
    c.x = Double.NaN;
    c.y = 2;
    c.scale = Float.POSITIVE_INFINITY;
    c.opacity = 300;
    c.padding = -1;
    c.sanitize();
    check(
        c.x == .02 && c.y == 1 && c.scale == 1 && c.opacity == 100 && c.padding == 0,
        "widget settings sanitize");
    old.widgets.put("example", c);
    var restored = WaypointStore.JSON.fromJson(WaypointStore.JSON.toJson(old), Config.class);
    restored.sanitize();
    check(restored.widgets.get("example").opacity == 100, "widget configuration round trip");
    var missing = WaypointStore.JSON.toJsonTree(d).getAsJsonObject();
    missing.getAsJsonArray("waypoints").get(0).getAsJsonObject().remove("x");
    reject(
        () -> WaypointStore.decode(missing.toString()),
        "missing coordinate rejected instead of defaulting to zero");
    var dispatcher =
        new com.mojang.brigadier.CommandDispatcher<
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>();
    dev.shorned.qol.command.ShornedCommand.register(dispatcher, null);
    for (String command :
        List.of(
            "shorned",
            "shorned hud",
            "shorned waypoint add",
            "shorned waypoint list",
            "shorned waypoint clear",
            "shorned debug"))
      check(!dispatcher.parse(command, null).getReader().canRead(), "command route: " + command);
    var bomb = new java.io.ByteArrayOutputStream();
    try (var gzip = new java.util.zip.GZIPOutputStream(bomb)) {
      gzip.write(
          " "
              .repeat(WaypointStore.MAX_BYTES + 1)
              .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    reject(
        () ->
            WaypointExchange.read(
                WaypointExchange.PREFIX + Base64.getEncoder().encodeToString(bomb.toByteArray()),
                w.world,
                w.dimension,
                w.area),
        "gzip expansion bounded");
    System.out.println("MilestoneTest: " + checks + " checks passed");
  }
}
