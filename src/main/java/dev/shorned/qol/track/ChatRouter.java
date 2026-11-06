package dev.shorned.qol.track;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.util.Hypixel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Single entry point for incoming chat: filters, de-duplicates, then hands the text to the trackers. */
public final class ChatRouter {
    private static String lastMessage = "";
    private static long lastAt = 0;

    private ChatRouter() {}

    public static void onGame(Component message, boolean overlay) {
        if (overlay) return;                                  // action bar repeats itself; chat only
        if (!Config.get().general.enabled || !Hypixel.allowed(Minecraft.getInstance())) return;

        String s = message.getString();
        if (s.isBlank()) return;

        long now = System.currentTimeMillis();
        if (s.equals(lastMessage) && now - lastAt < 200) return;   // some servers deliver the same line twice
        lastMessage = s;
        lastAt = now;

        MiningTracker.onChat(s);
        FishingTracker.onChat(s);
    }
}
