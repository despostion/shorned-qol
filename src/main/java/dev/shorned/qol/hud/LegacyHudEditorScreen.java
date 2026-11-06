package dev.shorned.qol.hud;

import dev.shorned.qol.config.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag-to-position editor. Panels show sample numbers so you can judge size and placement. */
public class LegacyHudEditorScreen extends Screen {
    private final Screen parent;
    private HudRenderer.Panel dragging;
    private int offX, offY;

    public LegacyHudEditorScreen(Screen parent) {
        super(Component.literal("Shorned's QoL - HUD editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        Config.Hud hud = Config.get().hud;
        this.addRenderableWidget(Button.builder(Component.literal("Mode: " + hud.mode), b -> {
            hud.mode = hud.mode == Config.HudMode.COMBINED ? Config.HudMode.SEPARATE : Config.HudMode.COMBINED;
            Config.save();
            HudRenderer.rebuild();
            this.rebuildWidgets();
        }).bounds(cx - 100, this.height - 54, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset positions"), b -> {
            HudRenderer.resetPositions();
        }).bounds(cx - 100, this.height - 30, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(cx + 5, this.height - 30, 95, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        HudRenderer.renderPreview(g);
        String hint = "Drag a panel to move it. Scale, opacity and sections are in /sqol config.";
        g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 74, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (HudRenderer.Panel p : HudRenderer.editablePanels()) {
                int[] b = HudRenderer.bounds(p);
                if (b != null && event.x() >= b[0] && event.x() <= b[0] + b[2]
                        && event.y() >= b[1] && event.y() <= b[1] + b[3]) {
                    dragging = p;
                    offX = (int) event.x() - b[0];
                    offY = (int) event.y() - b[1];
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            HudRenderer.setPos(dragging, Math.max(0, (int) event.x() - offX), Math.max(0, (int) event.y() - offY));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        Config.save();
        HudRenderer.rebuild();
        this.minecraft.gui.setScreen(parent);
    }
}
