package dev.shorned.qol.track;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the Hypixel tab list as plain text lines, in display order.
 * SkyBlock's tab "widgets" (Powders, Commissions, ...) are fake player entries sorted by team name,
 * so we sort the same way vanilla's player list does.
 */
public final class TabReader {
    private static final Comparator<PlayerInfo> ORDER = Comparator
            .comparing((PlayerInfo p) -> p.getTeam() == null ? "" : p.getTeam().getName())
            .thenComparing(p -> p.getProfile().name(), String.CASE_INSENSITIVE_ORDER);

    private TabReader() {}

    /** Lines with a display name, in tab order (what the parser consumes). */
    public static List<String> read(Minecraft mc) { return collect(mc, false); }

    /**
     * Every listed entry in tab order for debug export. An entry without a display name becomes an empty line,
     * so the exported line count matches the tab list instead of silently dropping entries.
     */
    public static List<String> readAll(Minecraft mc) { return collect(mc, true); }

    private static List<String> collect(Minecraft mc, boolean keepEmpty) {
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return List.of();
        List<PlayerInfo> infos = new ArrayList<>(conn.getListedOnlinePlayers());
        infos.sort(ORDER);
        List<String> out = new ArrayList<>(infos.size());
        for (PlayerInfo info : infos) {
            Component name = info.getTabListDisplayName();
            if (name != null) out.add(name.getString());
            else if (keepEmpty) out.add("");
        }
        return out;
    }
}
