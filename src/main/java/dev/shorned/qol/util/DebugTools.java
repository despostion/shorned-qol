package dev.shorned.qol.util;

import dev.shorned.qol.track.TabReader;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class DebugTools {
  public static Path root() {
    return FabricLoader.getInstance()
        .getConfigDir()
        .resolve("shorned-qol")
        .toAbsolutePath()
        .normalize();
  }

  public static void exportTab(Minecraft mc) {
    var lines = TabReader.readAll(mc);
    if (lines.isEmpty()) {
      Msg.chat(mc, "No tab data available.");
      return;
    }
    task(mc, () -> "Saved " + TabExport.write(root().resolve("debug"), LocalDateTime.now(), lines));
  }

  public static void exportDiagnostics(Minecraft mc) {
    var lines =
        List.of(
            "Shorned diagnostics",
            "Java: " + System.getProperty("java.version"),
            "Minecraft: 26.2",
            "Connected: " + (mc.getConnection() != null),
            "Tab entries: " + TabReader.readAll(mc).size());
    task(
        mc,
        () -> {
          var dir = root().resolve("debug");
          Files.createDirectories(dir);
          var p = Files.createTempFile(dir, "diagnostics-", ".txt");
          Files.write(p, lines);
          return "Saved " + p;
        });
  }

  public static void openFolder(Minecraft mc, boolean debug) {
    task(
        mc,
        () -> {
          Path base = root();
          Files.createDirectories(base);
          Path target = debug ? base.resolve("debug") : base;
          Files.createDirectories(target);
          Path realBase = base.toRealPath(), realTarget = target.toRealPath();
          if (!realTarget.startsWith(realBase)
              || !realBase.startsWith(FabricLoader.getInstance().getConfigDir().toRealPath()))
            throw new java.io.IOException("Folder outside mod configuration directory");
          if (System.getProperty("os.name").startsWith("Windows"))
            new ProcessBuilder("explorer.exe", realTarget.toString()).start();
          else if (java.awt.Desktop.isDesktopSupported())
            java.awt.Desktop.getDesktop().open(realTarget.toFile());
          else return "Open this folder manually: " + realTarget;
          return "Opened " + realTarget;
        });
  }

  public interface Job {
    String run() throws Exception;
  }

  public static void task(Minecraft mc, Job job) {
    CompletableFuture.runAsync(() -> {
      String message;
      try {
        message = job.run();
      } catch (Exception e) {
        message = "Action failed: " + e.getMessage();
      }
      String result = message;
      mc.execute(() -> Msg.chat(mc, result));
    });
  }
}
