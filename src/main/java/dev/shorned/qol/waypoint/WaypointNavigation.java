package dev.shorned.qol.waypoint;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.util.*;
import net.minecraft.client.Minecraft;
import java.util.*;
public final class WaypointNavigation {
    private static String last="";private static boolean reached;
    public static List<String> lines(boolean preview){
        var mc=Minecraft.getInstance();var w=WaypointManager.active();
        if(w==null)return List.of("Waypoint navigation","No active waypoint");
        if(mc.player==null||!WaypointManager.visible().contains(w))return List.of(w.name,"Outside scope or hidden");
        double distance=w.distance(mc.player.getX(),mc.player.getY(),mc.player.getZ());
        double angle=w.bearing(mc.player.getX(),mc.player.getZ(),mc.player.getYRot());
        String direction=Math.abs(angle)<22.5?"Ahead":Math.abs(angle)>157.5?"Behind":angle<0?"Left":"Right";
        return List.of(w.name,Math.round(distance)+" blocks | "+direction+" "+Math.round(Math.abs(angle))+" deg",Math.round(w.x)+", "+Math.round(w.y)+", "+Math.round(w.z));
    }
    public static void tick(Minecraft mc){
        if(!Config.get().general.enabled||!Hypixel.allowed(mc)||mc.player==null){last="";reached=false;return;}
        var w=WaypointManager.active();if(w==null){last="";reached=false;return;}
        if(!last.equals(w.id)){last=w.id;reached=false;}
        if(!WaypointManager.visible().contains(w))return;
        double d=w.distance(mc.player.getX(),mc.player.getY(),mc.player.getZ());
        if(d>8)reached=false;
        if(d<=3&&!reached){reached=true;if(WaypointManager.data().arrivalNotifications)Msg.chat(mc,"Reached "+w.name+". Active waypoint retained.");}
    }
}
