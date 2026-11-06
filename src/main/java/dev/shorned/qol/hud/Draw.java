package dev.shorned.qol.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers. Shapes are built from non-overlapping rects so translucent colours never double-blend. */
final class Draw {
    private Draw() {}

    /** Draws a panel background at (0,0) with size w x h; call with the pose already translated/scaled. */
    static void panel(GuiGraphicsExtractor g, int w, int h, int bg, int border, boolean rounded, boolean hasBorder) {
        boolean round = rounded && w >= 8 && h >= 8;
        if (hasBorder) {
            if (round) {
                g.fill(2, 0, w - 2, 1, border);
                g.fill(2, h - 1, w - 2, h, border);
                g.fill(0, 2, 1, h - 2, border);
                g.fill(w - 1, 2, w, h - 2, border);
                g.fill(1, 1, 2, 2, border);
                g.fill(w - 2, 1, w - 1, 2, border);
                g.fill(1, h - 2, 2, h - 1, border);
                g.fill(w - 2, h - 2, w - 1, h - 1, border);
                g.fill(2, 1, w - 2, 2, bg);
                g.fill(1, 2, w - 1, h - 2, bg);
                g.fill(2, h - 2, w - 2, h - 1, bg);
            } else {
                g.fill(0, 0, w, 1, border);
                g.fill(0, h - 1, w, h, border);
                g.fill(0, 1, 1, h - 1, border);
                g.fill(w - 1, 1, w, h - 1, border);
                g.fill(1, 1, w - 1, h - 1, bg);
            }
        } else if (round) {
            g.fill(2, 0, w - 2, 1, bg);
            g.fill(1, 1, w - 1, 2, bg);
            g.fill(0, 2, w, h - 2, bg);
            g.fill(1, h - 2, w - 1, h - 1, bg);
            g.fill(2, h - 1, w - 2, h, bg);
        } else {
            g.fill(0, 0, w, h, bg);
        }
    }

    static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
