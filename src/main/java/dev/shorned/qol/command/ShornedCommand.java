package dev.shorned.qol.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import dev.shorned.qol.ShornedQolClient;
import dev.shorned.qol.gui.HubScreen;
import dev.shorned.qol.hud.HudEditorScreen;
import dev.shorned.qol.util.Msg;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.commands.CommandBuildContext;

public final class ShornedCommand {
  public static void register(
      CommandDispatcher<FabricClientCommandSource> d, CommandBuildContext ctx) {
    var root = literal("shorned").executes(c -> open("Overview"));
    for (String name : new String[] {"mining", "fishing", "settings", "debug"})
      root.then(
          literal(name)
              .executes(c ->
                  open(
                      name.equals("debug")
                          ? "Debug & Tools"
                          : Character.toUpperCase(name.charAt(0)) + name.substring(1))));
    root.then(
        literal("hud")
            .executes(c -> {
              ShornedQolClient.pendingScreen = new HudEditorScreen(new HubScreen(null, "Overview"));
              return 1;
            }));
    root.then(
        literal("help")
            .executes(c -> {
              c.getSource()
                  .sendFeedback(
                      Msg.of(
                          "/shorned [mining|fishing|hud|waypoint [add|list|clear]|settings|debug]."
                              + " Legacy commands remain under /sqol."));
              return 1;
            }));
    root.then(
        literal("waypoint")
            .executes(c -> {
              ShornedQolClient.pendingScreen =
                  new dev.shorned.qol.gui.WaypointScreen(new HubScreen(null, "Overview"));
              return 1;
            })
            .then(
                literal("add")
                    .executes(c -> {
                      if (c.getSource().getPlayer() == null) return 0;
                      ShornedQolClient.pendingScreen =
                          dev.shorned.qol.gui.WaypointScreen.editor(
                              new dev.shorned.qol.gui.WaypointScreen(null),
                              dev.shorned.qol.waypoint.WaypointManager.draft(),
                              true);
                      return 1;
                    }))
            .then(
                literal("list")
                    .executes(c -> {
                      for (var w : dev.shorned.qol.waypoint.WaypointManager.data().waypoints)
                        c.getSource()
                            .sendFeedback(
                                Msg.of(
                                    w.name + " / " + w.group + ": " + w.x + ", " + w.y + ", "
                                        + w.z));
                      return 1;
                    }))
            .then(
                literal("clear")
                    .executes(c -> {
                      ShornedQolClient.pendingScreen =
                          new dev.shorned.qol.gui.ConfirmScreen(
                              new dev.shorned.qol.gui.WaypointScreen(null),
                              "Delete all waypoints?",
                              () ->
                                  dev.shorned.qol.waypoint.WaypointManager.edit(v -> {
                                    v.waypoints.clear();
                                    v.active = "";
                                  }));
                      return 1;
                    })));
    d.register(root);
  }

  private static int open(String section) {
    ShornedQolClient.pendingScreen = new HubScreen(null, section);
    return 1;
  }
}
