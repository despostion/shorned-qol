package dev.shorned.qol.waypoint;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.util.Hypixel;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.*;

/**
 * Extracts independent render snapshots; drawing never accesses mutable world or waypoint state.
 */
public final class WaypointRenderer {
  private record Frame(DrawableGizmoPrimitives depth, DrawableGizmoPrimitives through) {}

  private static final RenderStateDataKey<Frame> KEY =
      RenderStateDataKey.create(() -> "shorned-waypoints");

  public static void register() {
    LevelExtractionEvents.END_EXTRACTION.register(ctx -> {
      ctx.levelState().setData(KEY, null);
      var mc = Minecraft.getInstance();
      if (mc.player == null
          || !Config.get().general.enabled
          || !Hypixel.allowed(mc)
          || !WaypointManager.data().renderWorld) return;
      var depth = new DrawableGizmoPrimitives();
      var through = new DrawableGizmoPrimitives();
      var collector = new SimpleGizmoCollector();
      int count = 0;
      try (var ignored = Gizmos.withCollector(collector)) {
        for (var w : WaypointManager.visible()) {
          double dist = w.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ());
          if (dist > WaypointManager.data().maxDistance || count++ >= 128) continue;
          int color = 0xFF000000 | w.color;
          var pos = new Vec3(w.x, w.y, w.z);
          GizmoProperties marker =
              switch (w.marker) {
                case "POINT" -> Gizmos.point(pos, color, 8);
                case "RING" -> Gizmos.circle(pos, 0.75f, GizmoStyle.stroke(color, 2));
                default ->
                    Gizmos.cuboid(
                        new AABB(w.x - .4, w.y, w.z - .4, w.x + .4, w.y + .8, w.z + .4),
                        GizmoStyle.stroke(color, 2));
              };
          if (w.throughWalls) marker.setAlwaysOnTop();
          if (w.beam) {
            var beam = Gizmos.line(pos, pos.add(0, 32, 0), color, 3);
            if (w.throughWalls) beam.setAlwaysOnTop();
          }
          String label = w.name + " [" + Math.round(dist) + "m]";
          if (dist < 32)
            label += " " + Math.round(w.x) + ", " + Math.round(w.y) + ", " + Math.round(w.z);
          var text =
              Gizmos.billboardText(
                  label, pos.add(0, 1.3, 0), TextGizmo.Style.forColorAndCentered(color));
          if (w.throughWalls) text.setAlwaysOnTop();
        }
      }
      for (var g : collector.getGizmos()) g.gizmo().emit(g.isAlwaysOnTop() ? through : depth, 1);
      ctx.levelState().setData(KEY, new Frame(depth, through));
    });
    LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
      var frame = ctx.levelState().getData(KEY);
      if (frame != null) {
        frame.depth.submit(ctx.submitNodeCollector(), ctx.levelState().cameraRenderState, false);
        frame.through.submit(ctx.submitNodeCollector(), ctx.levelState().cameraRenderState, true);
      }
    });
  }
}
