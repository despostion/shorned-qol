package dev.shorned.qol.track;

import dev.shorned.qol.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Sounds, on-screen banners and the optional screen flash. Pure notification, never touches gameplay. */
public final class Alerts {
    public static final String[] SOUND_NAMES = {"Orb", "Level Up", "Arrow Hit", "Anvil", "Bell"};

    public record Banner(String text, int rgb, long start, long durationMs, boolean big) {}

    private static Banner bite;   // centre-screen, large
    private static Banner info;   // smaller, below it
    private static long flashStart;

    private Alerts() {}

    private static SoundEvent sound(int idx) {
        return switch (idx) {
            case 1 -> SoundEvents.PLAYER_LEVELUP;
            case 2 -> SoundEvents.ARROW_HIT_PLAYER;
            case 3 -> SoundEvents.ANVIL_LAND;
            case 4 -> SoundEvents.BELL_BLOCK;
            default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
    }

    public static void play(int soundIdx) {
        float vol = Config.get().notifications.volume / 100f;
        if (vol <= 0f) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound(soundIdx), 1.0f, vol));
    }

    public static void bite() {
        Config.Notifications n = Config.get().notifications;
        if (n.biteSound) play(n.biteSoundType);
        if (n.biteBanner) bite = new Banner("BITE!", 0xFF5555, System.currentTimeMillis(), 1200, true);
        if (n.screenFlash) flashStart = System.currentTimeMillis();
    }

    public static void ability(String name) {
        Config.Notifications n = Config.get().notifications;
        if (n.abilitySound) play(1);
        if (n.abilityBanner) info = new Banner(name + " ready", 0x55FF55, System.currentTimeMillis(), 2500, false);
    }

    public static void commission(String name) {
        Config.Notifications n = Config.get().notifications;
        if (n.commissionSound) play(1);
        if (n.commissionBanner) info = new Banner("Commission complete: " + name, 0xFFD24D, System.currentTimeMillis(), 3000, false);
    }

    /** Fired by the config screen's "Test" button. */
    public static void test() {
        bite();
        info = new Banner("Test notification", 0x55FF55, System.currentTimeMillis(), 2000, false);
    }

    public static Banner biteBanner() { return live(bite); }
    public static Banner infoBanner() { return live(info); }

    private static Banner live(Banner b) {
        return b != null && System.currentTimeMillis() - b.start() < b.durationMs() ? b : null;
    }

    /** 0..1 opacity of a banner: quick fade-in, slow fade-out. */
    public static float fade(Banner b, boolean animate) {
        if (!animate) return 1f;
        long t = System.currentTimeMillis() - b.start();
        float in = Math.min(1f, t / 150f);
        float out = Math.min(1f, (b.durationMs() - t) / 400f);
        return Math.max(0f, Math.min(in, out));
    }

    /** 0..1 strength of the red screen flash (fades over 350 ms). */
    public static float flash() {
        long t = System.currentTimeMillis() - flashStart;
        return t < 0 || t > 350 ? 0f : 1f - t / 350f;
    }
}
