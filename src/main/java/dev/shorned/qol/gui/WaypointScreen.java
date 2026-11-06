package dev.shorned.qol.gui;

import dev.shorned.qol.waypoint.*;
import java.util.*;
import net.minecraft.client.gui.screens.Screen;

public final class WaypointScreen extends HubScreen {
  private String query = "";
  private boolean byDistance;

  public WaypointScreen(Screen parent) {
    super(parent, "Waypoints");
  }

  @Override
  protected void content() {
    lines.add(
        WaypointManager.data().waypoints.size()
            + " saved | Active: "
            + (WaypointManager.active() == null ? "none" : WaypointManager.active().name));
    action(
        "Create at current coordinates / enter manually",
        () -> {
          if (minecraft.player != null) open(editor(this, WaypointManager.draft(), true));
        });
    action(
        "Search name / group: " + query,
        () -> {
          var m = new LinkedHashMap<String, String>();
          m.put("Search", query);
          open(new FormScreen(this, "Filter waypoints", m, v -> query = v.get("Search")));
        });
    action(
        "Sort: " + (byDistance ? "Distance" : "Name"),
        () -> {
          byDistance = !byDistance;
          rebuildWidgets();
        });
    action(
        "Clear all (confirm)",
        () ->
            open(
                new ConfirmScreen(
                    this,
                    "Delete all waypoints?",
                    () ->
                        WaypointManager.edit(d -> {
                          d.waypoints.clear();
                          d.active = "";
                        }))));
    action("Import / export", () -> open(new WaypointExchangeScreen(this)));
    action(
        "Stop navigation",
        () -> {
          WaypointManager.edit(d -> d.active = "");
          rebuildWidgets();
        });
    action(
        "World markers: " + WaypointManager.data().renderWorld,
        () -> {
          WaypointManager.edit(d -> d.renderWorld = !d.renderWorld);
          rebuildWidgets();
        });
    action(
        "Arrival notifications: " + WaypointManager.data().arrivalNotifications,
        () -> {
          WaypointManager.edit(d -> d.arrivalNotifications = !d.arrivalNotifications);
          rebuildWidgets();
        });
    action(
        "Render distance: " + WaypointManager.data().maxDistance,
        () -> {
          var m = new LinkedHashMap<String, String>();
          m.put("Blocks (16-4096)", "" + WaypointManager.data().maxDistance);
          open(
              new FormScreen(
                  this,
                  "Marker range",
                  m,
                  v ->
                      WaypointManager.edit(
                          d -> d.maxDistance = Integer.parseInt(v.get("Blocks (16-4096)")))));
        });
    var groups = new TreeSet<String>();
    for (var w : WaypointManager.data().waypoints) groups.add(w.group);
    for (String group : groups)
      action(
          "Group " + group + ": " + !WaypointManager.data().hiddenGroups.contains(group),
          () -> {
            WaypointManager.edit(d -> {
              if (!d.hiddenGroups.remove(group)) d.hiddenGroups.add(group);
            });
            rebuildWidgets();
          });
    var list = new ArrayList<>(WaypointManager.data().waypoints);
    list.sort(
        byDistance && minecraft.player != null
            ? Comparator.comparingDouble(w ->
                w.distance(
                    minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()))
            : Comparator.comparing(w -> w.name, String.CASE_INSENSITIVE_ORDER));
    for (var w : list)
      if ((w.name + " " + w.group)
          .toLowerCase(Locale.ROOT)
          .contains(query.toLowerCase(Locale.ROOT)))
        action(
            (w.enabled ? "[+] " : "[-] ") + w.name + " / " + w.group,
            () -> open(new Detail(this, w.id)));
  }

  public static Screen editor(Screen parent, Waypoint original, boolean create) {
    var v = new LinkedHashMap<String, String>();
    v.put("Name", original.name);
    v.put("X", "" + original.x);
    v.put("Y", "" + original.y);
    v.put("Z", "" + original.z);
    v.put("Color hex RGB", String.format("%06X", original.color));
    v.put("Marker BOX / POINT / RING", original.marker);
    v.put("Group", original.group);
    v.put("Description", original.description);
    v.put("World / server", original.world);
    v.put("Dimension", original.dimension);
    v.put("Area (blank = any)", original.area);
    return new FormScreen(
        parent,
        create ? "Create waypoint" : "Edit waypoint",
        v,
        m -> {
          var w = WaypointStore.JSON.fromJson(WaypointStore.JSON.toJson(original), Waypoint.class);
          w.name = m.get("Name");
          w.x = Double.parseDouble(m.get("X"));
          w.y = Double.parseDouble(m.get("Y"));
          w.z = Double.parseDouble(m.get("Z"));
          w.color = Integer.parseInt(m.get("Color hex RGB"), 16);
          w.marker = m.get("Marker BOX / POINT / RING").toUpperCase(Locale.ROOT);
          w.group = m.get("Group");
          w.description = m.get("Description");
          w.world = m.get("World / server");
          w.dimension = m.get("Dimension");
          w.area = m.get("Area (blank = any)");
          w.validate();
          WaypointManager.edit(d -> {
            if (create && d.waypoints.size() >= WaypointStore.LIMIT)
              throw new IllegalArgumentException("Maximum 500 waypoints");
            d.waypoints.removeIf(old -> old.id.equals(w.id));
            d.waypoints.add(w);
          });
        });
  }

  private static final class Detail extends HubScreen {
    private final String id;

    Detail(Screen p, String id) {
      super(p, "Waypoint details");
      this.id = id;
    }

    @Override
    protected void content() {
      var w =
          WaypointManager.data().waypoints.stream()
              .filter(v -> v.id.equals(id))
              .findFirst()
              .orElse(null);
      if (w == null) return;
      lines.add(w.name + " | " + w.x + ", " + w.y + ", " + w.z);
      lines.add(w.description);
      action(
          "Navigate",
          () -> {
            WaypointManager.edit(d -> d.active = id);
            onClose();
          });
      action("Edit", () -> open(editor(this, w, false)));
      action(
          "Enabled: " + w.enabled,
          () -> {
            WaypointManager.edit(d ->
                d.waypoints.stream()
                    .filter(v -> v.id.equals(id))
                    .forEach(v -> v.enabled = !v.enabled));
            rebuildWidgets();
          });
      action(
          "Pillar: " + w.beam,
          () -> {
            WaypointManager.edit(d ->
                d.waypoints.stream().filter(v -> v.id.equals(id)).forEach(v -> v.beam = !v.beam));
            rebuildWidgets();
          });
      action(
          "Through walls: " + w.throughWalls,
          () ->
              open(
                  new ConfirmScreen(
                      this,
                      "Toggle through-wall rendering?",
                      () ->
                          WaypointManager.edit(d ->
                              d.waypoints.stream()
                                  .filter(v -> v.id.equals(id))
                                  .forEach(v -> v.throughWalls = !v.throughWalls)))));
      action(
          "Delete (confirm)",
          () ->
              open(
                  new ConfirmScreen(
                      parent,
                      "Delete " + w.name + "?",
                      () ->
                          WaypointManager.edit(d -> {
                            d.waypoints.removeIf(v -> v.id.equals(id));
                            if (d.active.equals(id)) d.active = "";
                          }))));
    }
  }
}
