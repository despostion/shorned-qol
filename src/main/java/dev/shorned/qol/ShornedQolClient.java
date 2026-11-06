package dev.shorned.qol;

import dev.shorned.qol.cmd.SqolCommand;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.HudRenderer;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.track.ChatRouter;
import dev.shorned.qol.track.Trackers;
import dev.shorned.qol.util.Msg;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/** Shorned's QoL Mod - client entrypoint. Every event is registered exactly once, here. */
public class ShornedQolClient implements ClientModInitializer {
    public static final String MOD_ID = "shorned-qol";

    /** Screen to open on the next tick (commands cannot open one directly; the chat screen closes over it). */
    public static Screen pendingScreen;

    @Override
    public void onInitializeClient() {
        Config.load();
        StatsManager.load();

        ClientReceiveMessageEvents.GAME.register(ChatRouter::onGame);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Trackers.tick(mc);
            if (pendingScreen != null) {
                Screen s = pendingScreen;
                pendingScreen = null;
                mc.gui.setScreen(s);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            Trackers.onJoin();
            if (Config.recoveredFromCorruption) {
                Config.recoveredFromCorruption = false;
                Msg.chat(mc, "Your config file was unreadable, so defaults were restored (the old file was kept as *.corrupt-*).");
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            Trackers.onLeave();
            StatsManager.save();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            StatsManager.save();
            Config.save();
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                HudRenderer::render);

        ClientCommandRegistrationCallback.EVENT.register(SqolCommand::register);
    }
}
