package dev.shorned.qol.gui;

import dev.shorned.qol.track.MiningTracker;
import dev.shorned.qol.util.*;
import dev.shorned.qol.waypoint.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;

public final class WaypointExchangeScreen extends HubScreen {
  public WaypointExchangeScreen(Screen parent) {
    super(parent, "Waypoint import / export");
  }

  @Override
  protected void content() {
    lines.add("Native JSON or Skyblocker V1. Imports append only, after confirmation.");
    lines.add("Skyblocker imports bind to your current world, dimension and area.");
    action("Import clipboard", () -> importText(minecraft.keyboardHandler.getClipboard()));
    action(
        "Import file from mod imports folder",
        () -> {
          var m = new LinkedHashMap<String, String>();
          m.put("Filename only", "waypoints.json");
          open(
              new FormScreen(
                  this,
                  "Import from config/shorned-qol/imports",
                  m,
                  v -> {
                    String name = v.get("Filename only");
                    if (!name.matches("[A-Za-z0-9_.-]{1,100}")
                        || name.equals(".")
                        || name.equals(".."))
                      throw new IllegalArgumentException("Use a filename without directories");
                    DebugTools.task(
                        minecraft,
                        () -> {
                          Path base = DebugTools.root().resolve("imports");
                          Files.createDirectories(base);
                          Path f = base.resolve(name);
                          if (!f.toRealPath().startsWith(DebugTools.root().toRealPath())
                              || Files.size(f) > WaypointStore.MAX_BYTES)
                            throw new java.io.IOException("Invalid import path or size");
                          String text = Files.readString(f);
                          minecraft.execute(() -> importText(text));
                          return "Read " + name;
                        });
                  }));
        });
    action(
        "Copy native JSON",
        () ->
            minecraft.keyboardHandler.setClipboard(
                WaypointStore.JSON.toJson(WaypointManager.data())));
    action(
        "Export native JSON file",
        () -> {
          String text = WaypointStore.JSON.toJson(WaypointManager.data());
          DebugTools.task(
              minecraft,
              () -> {
                var dir = DebugTools.root().resolve("exports");
                Files.createDirectories(dir);
                Path file = Files.createTempFile(dir, "waypoints-", ".json");
                Files.writeString(file, text);
                return "Exported " + file;
              });
        });
    action(
        "Copy Skyblocker V1 (integer coordinates)",
        () -> {
          try {
            minecraft.keyboardHandler.setClipboard(
                WaypointExchange.skyblocker(WaypointManager.data()));
          } catch (Exception e) {
            Msg.chat(minecraft, e.getMessage());
          }
        });
  }

  private void importText(String text) {
    String world = WaypointManager.world(),
        dimension = WaypointManager.dimension(),
        area = MiningTracker.area();
    DebugTools.task(
        minecraft,
        () -> {
          var incoming = WaypointExchange.read(text, world, dimension, area);
          // Through-wall visibility is never silently enabled by an imported document.
          for (var w : incoming.waypoints) w.throughWalls = false;
          minecraft.execute(() ->
              open(
                  new ConfirmScreen(
                      this,
                      "Append " + incoming.waypoints.size() + " waypoints? Through-walls disabled.",
                      () ->
                          WaypointManager.commit(
                              WaypointStore.append(WaypointManager.data(), incoming)))));
          return "Import validated; confirm in the interface.";
        });
  }
}
