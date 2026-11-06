package dev.shorned.qol.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.shorned.qol.ShornedQolClient;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.HudEditorScreen;
import dev.shorned.qol.hud.HudRenderer;
import dev.shorned.qol.stats.Counters;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.track.FishingTracker;
import dev.shorned.qol.track.TabReader;
import dev.shorned.qol.util.Fmt;
import dev.shorned.qol.track.TabParser;
import dev.shorned.qol.util.Msg;
import dev.shorned.qol.util.TabExport;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** /sqol and its subcommands. Every command answers with a [Shorned's QoL] chat line. */
public final class SqolCommand {
    private SqolCommand() {}

    private static LiteralArgumentBuilder<FabricClientCommandSource> lit(String name) { return ClientCommands.literal(name); }

    private static int say(CommandContext<FabricClientCommandSource> c, String text) {
        c.getSource().sendFeedback(Msg.of(text));
        return 1;
    }

    private static int fail(CommandContext<FabricClientCommandSource> c, String text) {
        c.getSource().sendError(Msg.of(text));
        return 0;
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    /** Save settings and refresh the cached HUD. */
    private static void commit() {
        Config.save();
        HudRenderer.rebuild();
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> d, CommandBuildContext ctx) {
        d.register(lit("sqol")
                .executes(SqolCommand::status)
                .then(lit("help").executes(SqolCommand::help))
                .then(lit("config").executes(c -> { HudRenderer.openConfigLater(); return say(c, "Opening settings..."); }))
                .then(hud()).then(mining()).then(fishing()).then(reset()).then(stats()).then(pos()).then(debug())
                .then(lit("bite").executes(c -> {
                    Config.Notifications n = Config.get().notifications;
                    n.biteSound = !n.biteSound;
                    commit();
                    return say(c, "Bite sound " + onOff(n.biteSound));
                }))
                .then(lit("ability").executes(c -> {
                    Config.Notifications n = Config.get().notifications;
                    n.abilitySound = n.abilityBanner = !n.abilitySound;
                    commit();
                    return say(c, "Ability alerts " + onOff(n.abilitySound));
                })));
    }

    // ------------------------------------------------------------------ info

    private static int status(CommandContext<FabricClientCommandSource> c) {
        Config cfg = Config.get();
        String v = FabricLoader.getInstance().getModContainer(ShornedQolClient.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
        say(c, "Shorned's QoL Mod v" + v + " by Shorned");
        say(c, "HUD " + onOff(cfg.hud.enabled) + " | Mining " + onOff(cfg.mining.enabled)
                + " | Fishing " + onOff(cfg.fishing.enabled) + "  -  /sqol help for commands");
        return 1;
    }

    private static int help(CommandContext<FabricClientCommandSource> c) {
        String[] lines = {
                "/sqol config - open the settings screen",
                "/sqol hud [edit | mode combined|separate | scale <n> | opacity <0-100> | reset]",
                "/sqol mining [reset]   /sqol fishing [reset | recent | pattern ...]",
                "/sqol stats [session|lifetime]",
                "/sqol reset [session|mining|fishing|lifetime confirm]",
                "/sqol pos <x> <y>   (or: pos mining|fishing <x> <y>)",
                "/sqol bite | ability - toggle those alerts",
                "/sqol debug tab - preview up to 60 tab lines",
                "/sqol debug tab export - save all listed entries to a text file",
                "/sqol debug tab parsed - show extracted mining data"};
        for (String l : lines) say(c, l);
        return 1;
    }

    // ------------------------------------------------------------------ hud

    private static LiteralArgumentBuilder<FabricClientCommandSource> hud() {
        return lit("hud")
                .executes(c -> {
                    Config.Hud h = Config.get().hud;
                    h.enabled = !h.enabled;
                    commit();
                    return say(c, "HUD " + onOff(h.enabled));
                })
                .then(lit("edit").executes(c -> {
                    ShornedQolClient.pendingScreen = new HudEditorScreen(null);
                    return say(c, "Opening HUD editor...");
                }))
                .then(lit("mode")
                        .then(lit("combined").executes(c -> setMode(c, Config.HudMode.COMBINED)))
                        .then(lit("separate").executes(c -> setMode(c, Config.HudMode.SEPARATE))))
                .then(lit("scale").then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.5f, 3.0f)).executes(c -> {
                    Config.get().hud.scale = FloatArgumentType.getFloat(c, "value");
                    commit();
                    return say(c, "HUD scale " + Config.get().hud.scale + "x");
                })))
                .then(lit("opacity").then(ClientCommands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    Config.get().hud.opacity = IntegerArgumentType.getInteger(c, "percent");
                    commit();
                    return say(c, "HUD opacity " + Config.get().hud.opacity + "%");
                })))
                .then(lit("reset").executes(c -> { HudRenderer.resetPositions(); HudRenderer.rebuild(); return say(c, "HUD positions reset"); }));
    }

    private static int setMode(CommandContext<FabricClientCommandSource> c, Config.HudMode mode) {
        Config.get().hud.mode = mode;
        commit();
        return say(c, "HUD layout: " + mode);
    }

    // ------------------------------------------------------------------ mining / fishing

    private static LiteralArgumentBuilder<FabricClientCommandSource> mining() {
        return lit("mining")
                .executes(c -> {
                    Config.Mining m = Config.get().mining;
                    m.enabled = !m.enabled;
                    commit();
                    return say(c, "Mining tracker " + onOff(m.enabled));
                })
                .then(lit("reset").executes(c -> { StatsManager.resetMiningSession(); HudRenderer.rebuild(); return say(c, "Mining session reset"); }));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> fishing() {
        return lit("fishing")
                .executes(c -> {
                    Config.Fishing f = Config.get().fishing;
                    f.enabled = !f.enabled;
                    commit();
                    return say(c, "Fishing tracker " + onOff(f.enabled));
                })
                .then(lit("reset").executes(c -> { StatsManager.resetFishingSession(); HudRenderer.rebuild(); return say(c, "Fishing session reset"); }))
                .then(lit("recent").executes(c -> {
                    List<String> lines = FishingTracker.recentLines();
                    if (lines.isEmpty()) return say(c, "No chat captured yet. Get a bite, then run this within 30 s.");
                    say(c, "Chat after your last bite (copy a line into a pattern):");
                    for (String l : lines) say(c, "  " + l);
                    return 1;
                }))
                .then(lit("pattern")
                        .then(lit("list").executes(c -> {
                            Config.Fishing f = Config.get().fishing;
                            say(c, "Spawn patterns: " + (f.spawnPatterns.isEmpty() ? "(none)" : f.spawnPatterns));
                            say(c, "Kill patterns: " + (f.killPatterns.isEmpty() ? "(none)" : f.killPatterns));
                            return 1;
                        }))
                        .then(lit("add")
                                .then(lit("spawn").then(ClientCommands.argument("regex", StringArgumentType.greedyString())
                                        .executes(c -> addPattern(c, true))))
                                .then(lit("kill").then(ClientCommands.argument("regex", StringArgumentType.greedyString())
                                        .executes(c -> addPattern(c, false)))))
                        .then(lit("clear")
                                .then(lit("spawn").executes(c -> { Config.get().fishing.spawnPatterns.clear(); commit(); return say(c, "Spawn patterns cleared"); }))
                                .then(lit("kill").executes(c -> { Config.get().fishing.killPatterns.clear(); commit(); return say(c, "Kill patterns cleared"); }))));
    }

    private static int addPattern(CommandContext<FabricClientCommandSource> c, boolean spawn) {
        String regex = StringArgumentType.getString(c, "regex");
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return fail(c, "Invalid regex: " + e.getDescription());
        }
        Config.Fishing f = Config.get().fishing;
        (spawn ? f.spawnPatterns : f.killPatterns).add(regex);
        commit();
        return say(c, "Added " + (spawn ? "spawn" : "kill") + " pattern: " + regex);
    }

    // ------------------------------------------------------------------ stats / reset / pos / debug

    private static LiteralArgumentBuilder<FabricClientCommandSource> stats() {
        return lit("stats")
                .executes(c -> { printStats(c, "Session", StatsManager.session()); printStats(c, "Lifetime", StatsManager.lifetime()); return 1; })
                .then(lit("session").executes(c -> { printStats(c, "Session", StatsManager.session()); return 1; }))
                .then(lit("lifetime").executes(c -> { printStats(c, "Lifetime", StatsManager.lifetime()); return 1; }));
    }

    private static void printStats(CommandContext<FabricClientCommandSource> c, String title, Counters s) {
        say(c, title + ": active mining " + Fmt.time(s.miningMs) + ", active fishing " + Fmt.time(s.fishingMs)
                + ", elapsed (connected) " + Fmt.time(s.elapsedMs));
        say(c, "  Mithril " + Fmt.num(s.mithril) + " (" + Fmt.rate(s.mithril, s.miningMs) + "), Gemstone "
                + Fmt.num(s.gemstone) + " (" + Fmt.rate(s.gemstone, s.miningMs) + "), Glacite " + Fmt.num(s.glacite));
        say(c, "  Bites " + Fmt.num(s.bites) + " (" + Fmt.rate(s.bites, s.fishingMs) + "), sea creatures "
                + Fmt.num(s.seaCreatures) + ", kills " + Fmt.num(s.seaKills) + ", commissions " + Fmt.num(s.commissionsDone));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> reset() {
        return lit("reset")
                .executes(c -> { StatsManager.resetSession(); HudRenderer.rebuild(); return say(c, "Session reset"); })
                .then(lit("session").executes(c -> { StatsManager.resetSession(); HudRenderer.rebuild(); return say(c, "Session reset"); }))
                .then(lit("mining").executes(c -> { StatsManager.resetMiningSession(); HudRenderer.rebuild(); return say(c, "Mining session reset"); }))
                .then(lit("fishing").executes(c -> { StatsManager.resetFishingSession(); HudRenderer.rebuild(); return say(c, "Fishing session reset"); }))
                .then(lit("lifetime")
                        .executes(c -> say(c, "This erases ALL lifetime stats. Run /sqol reset lifetime confirm to proceed."))
                        .then(lit("confirm").executes(c -> { StatsManager.resetLifetime(); return say(c, "Lifetime statistics reset"); })));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> pos() {
        return lit("pos")
                .then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(c -> {
                            HudRenderer.setPos(HudRenderer.Panel.COMBINED, IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"));
                            commit();
                            return say(c, "HUD moved");
                        })))
                .then(lit("mining").then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(c -> {
                            HudRenderer.setPos(HudRenderer.Panel.MINING, IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"));
                            commit();
                            return say(c, "Mining HUD moved");
                        }))))
                .then(lit("fishing").then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(c -> {
                            HudRenderer.setPos(HudRenderer.Panel.FISHING, IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"));
                            commit();
                            return say(c, "Fishing HUD moved");
                        }))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> debug() {
        return lit("debug").then(lit("tab")
                .executes(c -> {
                    List<String> lines = TabReader.read(Minecraft.getInstance());
                    say(c, "Tab list: " + lines.size() + " lines (showing up to 60; /sqol debug tab export saves all)");
                    for (int i = 0; i < Math.min(60, lines.size()); i++) say(c, "[" + i + "] " + lines.get(i));
                    return 1;
                })
                .then(lit("export").executes(SqolCommand::exportTab))
                .then(lit("parsed").executes(SqolCommand::printParsed)));
    }

    /** Reads the tab list on the client thread, writes the file on a worker thread, reports back on the client thread. */
    private static int exportTab(CommandContext<FabricClientCommandSource> c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) {
            return fail(c, "Join a world or server first - there is no tab list to export.");
        }
        List<String> lines = TabReader.readAll(mc);
        if (lines.isEmpty()) return fail(c, "The tab list is empty, nothing to export.");

        Path dir = FabricLoader.getInstance().getConfigDir().resolve("shorned-qol").resolve("debug");
        LocalDateTime now = LocalDateTime.now();
        say(c, "Exporting " + lines.size() + " tab-list lines...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return TabExport.write(dir, now, lines);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((file, err) -> mc.execute(() -> {
            if (err != null) {
                Throwable cause = err.getCause() != null ? err.getCause() : err;
                Msg.chat(mc, "Tab export failed: " + cause.getMessage());
            } else {
                Msg.chat(mc, "Saved " + lines.size() + " lines to config/shorned-qol/debug/" + file.getFileName());
                Msg.chat(mc, "Full path: " + file.toAbsolutePath());
            }
        }));
        return 1;
    }

    private static int printParsed(CommandContext<FabricClientCommandSource> c) {
        TabParser.Result r = TabParser.parse(TabReader.read(Minecraft.getInstance()));
        say(c, "Area: " + (r.area() == null ? "(none)" : r.area()) + " | Profile: " + (r.profile() == null ? "(none)" : r.profile()));
        say(c, "Powders: " + (r.powders().isEmpty() ? "(none)" : r.powders().toString()));
        say(c, "Commissions: " + (r.commissions().isEmpty() ? "(none)" : r.commissions().toString()));
        say(c, "Forges: " + (r.forges().isEmpty() ? "(none)" : r.forges().toString()));
        return 1;
    }
}
