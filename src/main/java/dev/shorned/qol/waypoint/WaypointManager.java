package dev.shorned.qol.waypoint;
import dev.shorned.qol.util.*;
import dev.shorned.qol.track.MiningTracker;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.util.*;
public final class WaypointManager {
    private static WaypointStore.Data data=new WaypointStore.Data();
    private static boolean writable=true;
    public static Path file(){return DebugTools.root().resolve("waypoints.json");}
    public static WaypointStore.Data data(){return data;}
    public static void load(){try{data=WaypointStore.load(file());}catch(Exception e){writable=false;System.err.println("[Shorned] Waypoints unreadable, original preserved: "+e);}}
    public static void commit(WaypointStore.Data next){
        if(!writable)throw new IllegalStateException("Waypoint file unreadable; original preserved. Repair it before saving.");
        try{WaypointStore.save(file(),next);data=next;}catch(java.io.IOException e){throw new IllegalStateException("Could not save waypoints: "+e.getMessage(),e);}
    }
    public static void edit(java.util.function.Consumer<WaypointStore.Data> change){var next=WaypointStore.copy(data);change.accept(next);commit(next);}
    public static Waypoint active(){return data.waypoints.stream().filter(w->w.id.equals(data.active)).findFirst().orElse(null);}
    public static String world(){var mc=Minecraft.getInstance();return mc.getCurrentServer()!=null?mc.getCurrentServer().ip.toLowerCase(Locale.ROOT):mc.getSingleplayerServer()!=null?"local:"+mc.getSingleplayerServer().getWorldData().getLevelName():"";}
    public static String dimension(){var l=Minecraft.getInstance().level;return l==null?"":l.dimension().identifier().toString();}
    public static List<Waypoint> visible(){String world=world(),dim=dimension(),area=MiningTracker.area();return data.waypoints.stream().filter(w->WaypointStore.visible(data,w,world,dim,area)).toList();}
    public static Waypoint draft(){var mc=Minecraft.getInstance();if(mc.player==null)throw new IllegalStateException("Join a world first");var w=new Waypoint();w.x=mc.player.getX();w.y=mc.player.getY();w.z=mc.player.getZ();w.world=world();w.dimension=dimension();w.area=MiningTracker.area();return w;}
}
