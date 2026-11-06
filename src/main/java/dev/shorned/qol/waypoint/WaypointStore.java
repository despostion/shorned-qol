package dev.shorned.qol.waypoint;
import com.google.gson.*;
import dev.shorned.qol.util.JsonFiles;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Versioned, bounded native format. Import validates everything before mutating existing data. */
public final class WaypointStore {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static final int LIMIT=500,MAX_BYTES=1_000_000;
    public static class Data {
        public int version=1;
        public List<Waypoint> waypoints=new ArrayList<>();
        public Set<String> hiddenGroups=new HashSet<>();
        public String active="";
        public boolean arrivalNotifications=true,renderWorld=true;
        public int maxDistance=512;
    }
    public static Data decode(String text){
        if(text.length()>MAX_BYTES)throw new IllegalArgumentException("Import too large");
        Data d=JSON.fromJson(text,Data.class);validate(d);return d;
    }
    public static void validate(Data d){
        if(d==null||d.version!=1||d.waypoints==null||d.waypoints.size()>LIMIT||d.hiddenGroups==null||d.active==null)throw new IllegalArgumentException("Invalid waypoint document/version");
        Set<String> ids=new HashSet<>();for(var w:d.waypoints){if(w==null)throw new IllegalArgumentException("Null waypoint");w.validate();if(!ids.add(w.id))throw new IllegalArgumentException("Duplicate waypoint ID");}
        if(d.hiddenGroups.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Invalid group");
        d.maxDistance=Math.clamp(d.maxDistance,16,4096);
        if(!d.active.isEmpty()&&!ids.contains(d.active))d.active="";
    }
    public static Data load(Path file)throws IOException {if(!Files.exists(file))return new Data();if(Files.size(file)>MAX_BYTES)throw new IOException("Waypoint file too large");return decode(Files.readString(file));}
    public static void save(Path file,Data d)throws IOException {validate(d);Files.createDirectories(file.getParent());JsonFiles.writeAtomic(JSON,file,d);}
    public static Data copy(Data d){return decode(JSON.toJson(d));}
    public static Data append(Data existing,Data imported){
        var result=copy(existing);validate(imported);
        if(result.waypoints.size()+imported.waypoints.size()>LIMIT)throw new IllegalArgumentException("Maximum 500 waypoints");
        for(var w:copy(imported).waypoints){w.id=UUID.randomUUID().toString();result.waypoints.add(w);}
        return result;
    }
    public static boolean visible(Data d,Waypoint w,String world,String dim,String area){return w.enabled&&!d.hiddenGroups.contains(w.group)&&w.matches(world,dim,area);}
}
