package dev.shorned.qol.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Every chat line the mod prints goes through here so the prefix is always identical. */
public final class Msg {
    public static final String PREFIX = "[Shorned's QoL] ";

    private Msg() {}

    public static Component of(String text) {
        MutableComponent c = Component.literal(PREFIX).withStyle(ChatFormatting.GOLD);
        return c.append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    public static void chat(Minecraft mc, String text) {
        if (mc.player != null) mc.player.sendSystemMessage(of(text));
    }
}
