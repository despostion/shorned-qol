package dev.shorned.qol.util;

import dev.shorned.qol.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public final class Hypixel {
    private Hypixel() {}

    /** True when tracking is allowed: either "only on Hypixel" is off, or we are connected to a Hypixel address. */
    public static boolean allowed(Minecraft mc) {
        if (!Config.get().general.onlyOnHypixel) return true;
        ServerData sd = mc.getCurrentServer();
        return sd != null && sd.ip != null && sd.ip.toLowerCase(Locale.ROOT).contains("hypixel");
    }
}
