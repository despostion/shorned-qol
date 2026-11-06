package dev.shorned.qol.gui;

import dev.shorned.qol.config.Config;
import dev.shorned.qol.hud.HudEditorScreen;
import dev.shorned.qol.stats.StatsManager;
import dev.shorned.qol.track.*;
import dev.shorned.qol.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Shared, keyboard-accessible paginated navigation; keeps controls within the viewport. */
public class HubScreen extends Screen {
    protected final Screen parent;
    protected final String section;
    protected int page;
    protected final List<String> lines = new ArrayList<>();
    protected final List<Action> actions = new ArrayList<>();
    protected record Action(String name, Runnable run) {}
    public HubScreen(Screen parent, String section) {
        super(Component.literal("Shorned's QoL / " + section)); this.parent=parent; this.section=section;
    }
    protected void action(String text, Runnable run) { actions.add(new Action(text,run)); }
    protected void open(Screen screen) { minecraft.gui.setScreen(screen); }
    protected void content() {
        var s=StatsManager.session();
        if (section.equals("Overview")) {
            lines.add("Area: " + available(MiningTracker.area()) + " | Profile: " + available(MiningTracker.profile()));
            lines.add("Session " + Fmt.time(s.elapsedMs) + " | Bites " + s.bites + " | Commissions " + s.commissionsDone);
            for (String name : List.of("Mining","Fishing","Settings","Debug & Tools"))
                action(name, () -> open(new HubScreen(this,name)));
            action("Waypoints", () -> open(new WaypointScreen(this)));
            action("HUD Editor", () -> open(new HudEditorScreen(this)));
            action("Modular HUD: " + Config.get().modularHud, () -> {Config.get().modularHud=!Config.get().modularHud;Config.save();rebuildWidgets();});
        } else if (section.equals("Mining") || section.equals("Fishing")) {
            boolean mining=section.equals("Mining");
            lines.add(mining ? "Powder gained: " + s.mithril + " / " + s.gemstone + " / " + s.glacite : "Bites: " + s.bites + " | Sea creatures: " + s.seaCreatures + " | Kills: " + s.seaKills);
            lines.add("Active time: " + Fmt.time(mining ? s.miningMs : s.fishingMs));
            action("Tracker: " + (mining ? Config.get().mining.enabled : Config.get().fishing.enabled), () -> {
                if(mining) Config.get().mining.enabled=!Config.get().mining.enabled;
                else Config.get().fishing.enabled=!Config.get().fishing.enabled;
                Config.save(); rebuildWidgets();
            });
            action("Module settings", () -> open(new ConfigScreen(this, section)));
            action("Reset session (confirm)", () -> open(new ConfirmScreen(this, "Reset " + section + " session?", () -> {
                if(mining) StatsManager.resetMiningSession(); else StatsManager.resetFishingSession();
            })));
        } else if(section.equals("Settings")) {
            lines.add("Settings save immediately. Existing tracker and notification settings are preserved.");
            action("Settings & notifications", () -> open(new ConfigScreen(this)));
        } else {
            var r=TabParser.parse(TabReader.read(minecraft));
            lines.add("Version " + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("shorned-qol").orElseThrow().getMetadata().getVersion());
            lines.add("Java " + System.getProperty("java.version") + " | Minecraft 26.2");
            lines.add("Area: " + available(r.area()) + " | Powder types: " + r.powders().size() + " | Commissions: " + r.commissions().size());
            action("Export full tab list", () -> DebugTools.exportTab(minecraft));
            action("Export diagnostics", () -> DebugTools.exportDiagnostics(minecraft));
            action("Open mod config folder", () -> DebugTools.openFolder(minecraft, false));
            action("Open debug/export folder", () -> DebugTools.openFolder(minecraft, true));
            for(String line: FishingTracker.recentLines()) lines.add(line);
        }
    }
    private static String available(String s) { return s==null || s.isEmpty() ? "unavailable" : s; }
    @Override protected void init() {
        lines.clear(); actions.clear(); content();
        int w=Math.min(420,Math.max(100,width-24)), x=(width-w)/2;
        int per=Math.max(1,(height-106)/24);
        int total=lines.size()+actions.size();
        page=Math.max(0,Math.min(page,Math.max(0,(total-1)/per)));
        for(int i=page*per;i<Math.min(total,(page+1)*per);i++) {
            int y=44+(i-page*per)*24;
            if(i>=lines.size()) { Action a=actions.get(i-lines.size());
                addRenderableWidget(Button.builder(Component.literal(a.name()), b->a.run().run()).bounds(x,y,w,20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(x,height-28,w/3-4,20).build());
        var prev=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;rebuildWidgets();}).bounds(x+w/3,height-28,w/3-4,20).build()); prev.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;rebuildWidgets();}).bounds(x+2*w/3,height-28,w/3-4,20).build()); next.active=(page+1)*per<total;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt) {
        g.fill(0,0,width,height,0xF0101520); g.fill(0,0,width,3,0xFFFFB347);
        super.extractRenderState(g,mx,my,dt);
        g.text(font,"SHORNED'S QoL / " + section,12,14,0xFFFFB347,true);
        int per=Math.max(1,(height-106)/24), x=(width-Math.min(420,Math.max(100,width-24)))/2;
        for(int i=page*per;i<Math.min(lines.size(),(page+1)*per);i++)
            g.text(font,font.plainSubstrByWidth(lines.get(i),width-24),x,48+(i-page*per)*24,0xFFE0E5EE,true);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
