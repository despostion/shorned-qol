package dev.shorned.qol.hud;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.stats.*;
import dev.shorned.qol.track.*;
import dev.shorned.qol.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
import java.util.function.Function;

public final class HudManager {
    public static final Map<String,HudWidget> WIDGETS=new LinkedHashMap<>();
    private static final Map<String,List<String>> cache=new LinkedHashMap<>();
    public static final Map<String,int[]> bounds=new LinkedHashMap<>();
    static {
        add("mithril",p->List.of("Mithril Powder",Fmt.num(StatsManager.session().mithril)+" gained"));
        add("gemstone",p->List.of("Gemstone Powder",Fmt.num(StatsManager.session().gemstone)+" gained"));
        add("glacite",p->List.of("Glacite Powder",Fmt.num(StatsManager.session().glacite)+" gained"));
        add("powder_rates",p->{var s=StatsManager.session();return List.of("Powder / hour", "Mithril " + Fmt.rate(s.mithril,s.miningMs),"Gemstone " + Fmt.rate(s.gemstone,s.miningMs),"Glacite " + Fmt.rate(s.glacite,s.miningMs));});
        add("commissions",p->{var a=new ArrayList<String>();a.add("Commissions");for(var c:MiningTracker.commissions())a.add(c.name()+": "+Math.round(c.percent())+"%");if(a.size()==1)a.add(p?"Preview: Titanium 50%":"No fresh commission data");return a;});
        add("forges",p->{var a=new ArrayList<String>();a.add("Forge status");for(var f:MiningTracker.forges())a.add(f.slot()+") "+f.state());if(a.size()==1)a.add("No fresh forge data");return a;});
        add("mining_session",p->List.of("Mining session",Fmt.time(StatsManager.session().miningMs),"Commissions: "+StatsManager.session().commissionsDone));
        add("ability",p->List.of("Pickaxe ability", MiningTracker.ability()==MiningTracker.Ability.UNKNOWN?"Unknown":MiningTracker.abilityName()+": "+MiningTracker.ability()));
        add("bites",p->List.of("Fishing bites",StatsManager.session().bites+" | "+Fmt.rate(StatsManager.session().bites,StatsManager.session().fishingMs)));
        add("sea_creatures",p->List.of("Sea creatures",StatsManager.session().seaCreatures+" spawns | "+StatsManager.session().seaKills+" kills"));
        add("fishing_session",p->List.of("Fishing session",Fmt.time(StatsManager.session().fishingMs)));
        add("coordinates",p->{var v=Minecraft.getInstance().player;return List.of("Coordinates",v==null?"No world":v.blockPosition().toShortString());});
        add("area",p->List.of("Area",MiningTracker.area().isEmpty()?"Unavailable":MiningTracker.area()));
        add("session",p->List.of("Connected session",Fmt.time(StatsManager.session().elapsedMs)));
    }
    public static void add(String id,Function<Boolean,List<String>> lines){WIDGETS.put(id,new HudWidget(){public String id(){return id;}public List<String> lines(boolean p){return lines.apply(p);}});}
    public static WidgetConfig config(String id){return Config.get().widgets.computeIfAbsent(id,k->defaults(WIDGETS.keySet().stream().toList().indexOf(id)));}
    public static WidgetConfig defaults(int i){var c=new WidgetConfig();c.x=0.02+(i/6)*0.32;c.y=0.03+(i%6)*0.15;return c;}
    public static void refresh(){cache.clear();for(var w:WIDGETS.values())cache.put(w.id(),List.copyOf(w.lines(false)));}
    public static void render(GuiGraphicsExtractor g,boolean preview){
        var mc=Minecraft.getInstance();bounds.clear();
        if(!preview && (!Config.get().general.enabled || !Config.get().hud.enabled || !Hypixel.allowed(mc)))return;
        if(cache.isEmpty())refresh();
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        for(var w:WIDGETS.values()){
            var c=config(w.id());if(!c.enabled&&!preview)continue;
            List<String> text=preview?w.lines(true):cache.getOrDefault(w.id(),List.of(w.id()));
            int width=40;for(String s:text)width=Math.max(width,mc.font.width(s));width+=c.padding*2;
            int h=c.padding*2+text.size()*(9+c.spacing);
            int bw=Math.round(width*c.scale),bh=Math.round(h*c.scale);
            int x=(int)(c.x*Math.max(0,sw-bw)),y=(int)(c.y*Math.max(0,sh-bh));
            bounds.put(w.id(),new int[]{x,y,bw,bh});
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(c.scale,c.scale);
            Draw.panel(g,width,h,c.background?Draw.argb(c.opacity*255/100,c.backgroundColor):0,0xFFB38A4B,false,c.border);
            int yy=c.padding;for(String s:text){g.text(mc.font,s,c.padding,yy,c.enabled?0xFF000000|c.textColor:0xFF777777,true);yy+=9+c.spacing;}
            g.pose().popMatrix();
        }
    }
}
