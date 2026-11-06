package dev.shorned.qol.waypoint;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Independent converter for the documented Skyblocker V1 wire schema; no runtime dependency. */
public final class WaypointExchange {
  public static final String PREFIX = "[Skyblocker-Waypoint-Data-V1]";

  public static WaypointStore.Data read(String text, String world, String dimension, String area)
      throws IOException {
    if (text.length() > WaypointStore.MAX_BYTES) throw new IOException("Import exceeds 1 MB");
    text = text.strip();
    if (!text.startsWith(PREFIX)) return WaypointStore.decode(text);
    byte[] compressed;
    try {
      compressed = Base64.getDecoder().decode(text.substring(PREFIX.length()));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid Base64", e);
    }
    byte[] bytes;
    try (var gz = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
      bytes = gz.readNBytes(WaypointStore.MAX_BYTES + 1);
    }
    if (bytes.length > WaypointStore.MAX_BYTES)
      throw new IOException("Expanded import exceeds 1 MB");
    var groups = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonArray();
    var result = new WaypointStore.Data();
    for (var element : groups) {
      var group = element.getAsJsonObject();
      for (var entry : group.getAsJsonArray("waypoints")) {
        if (result.waypoints.size() >= WaypointStore.LIMIT)
          throw new IOException("Maximum 500 waypoints");
        var source = entry.getAsJsonObject();
        var pos = source.getAsJsonArray("pos");
        var rgb = source.getAsJsonArray("colorComponents");
        if (pos.size() != 3 || rgb.size() != 3)
          throw new IOException("Expected three coordinates and RGB components");
        var w = new Waypoint();
        w.x = pos.get(0).getAsDouble();
        w.y = pos.get(1).getAsDouble();
        w.z = pos.get(2).getAsDouble();
        w.name = plainName(source.get("name"));
        w.group = group.get("name").getAsString();
        w.enabled = source.get("shouldRender").getAsBoolean();
        w.color = 0;
        for (var channel : rgb) {
          double c = channel.getAsDouble();
          if (!Double.isFinite(c) || c < 0 || c > 1) throw new IOException("Invalid RGB component");
          w.color = (w.color << 8) | (int) Math.round(c * 255);
        }
        w.world = world;
        w.dimension = dimension;
        w.area = area;
        w.description = "Imported island: " + group.get("island").getAsString();
        w.source = group.deepCopy();
        w.source.remove("waypoints");
        w.source.add("waypoint", source.deepCopy());
        w.validate();
        result.waypoints.add(w);
      }
    }
    return result;
  }

  private static String plainName(JsonElement name) throws IOException {
    if (name.isJsonPrimitive()) return name.getAsString();
    if (name.isJsonObject() && name.getAsJsonObject().has("text"))
      return name.getAsJsonObject().get("text").getAsString();
    throw new IOException("Unsupported styled/translated name; use a plain text name");
  }

  public static String skyblocker(WaypointStore.Data data) throws IOException {
    WaypointStore.validate(data);
    var groups = new JsonArray();
    // One group per entry preserves original island/group settings when available.
    for (var w : data.waypoints) {
      if (w.x != Math.rint(w.x) || w.y != Math.rint(w.y) || w.z != Math.rint(w.z))
        throw new IOException(
            "Skyblocker requires integer block coordinates; native export preserves decimals");
      var g = w.source == null ? new JsonObject() : w.source.deepCopy();
      g.remove("waypoint");
      g.addProperty("name", w.group);
      if (!g.has("island")) g.addProperty("island", "unknown");
      g.addProperty("ordered", false);
      g.addProperty("renderThroughWalls", w.throughWalls);
      g.addProperty("waypointType", "waypoint");
      var a = new JsonArray();
      var e =
          w.source != null && w.source.has("waypoint")
              ? w.source.getAsJsonObject("waypoint").deepCopy()
              : new JsonObject();
      var pos = new JsonArray();
      pos.add((int) w.x);
      pos.add((int) w.y);
      pos.add((int) w.z);
      e.add("pos", pos);
      e.addProperty("name", w.name);
      var rgb = new JsonArray();
      rgb.add(((w.color >> 16) & 255) / 255.0);
      rgb.add(((w.color >> 8) & 255) / 255.0);
      rgb.add((w.color & 255) / 255.0);
      e.add("colorComponents", rgb);
      if (!e.has("alpha")) e.addProperty("alpha", 1);
      e.addProperty("shouldRender", w.enabled && !data.hiddenGroups.contains(w.group));
      a.add(e);
      g.add("waypoints", a);
      groups.add(g);
    }
    var out = new ByteArrayOutputStream();
    try (var gz = new GZIPOutputStream(out)) {
      gz.write(WaypointStore.JSON.toJson(groups).getBytes(StandardCharsets.UTF_8));
    }
    return PREFIX + Base64.getEncoder().encodeToString(out.toByteArray());
  }
}
