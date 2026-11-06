package dev.shorned.qol.waypoint;
import java.util.UUID;
public class Waypoint {
    public String id=UUID.randomUUID().toString(),name="Waypoint",description="",group="General",world="",dimension="minecraft:overworld",area="";
    public double x,y,z;
    public int color=0xFFB347;
    public String marker="BOX";
    public boolean enabled=true,beam=false,throughWalls=false;
    public void validate(){
        UUID.fromString(id);
        if(name==null||name.isBlank()||name.length()>80)throw new IllegalArgumentException("Name must be 1-80 characters");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000||Math.abs(y)>2048)throw new IllegalArgumentException("Coordinates out of range");
        if(description==null||description.length()>512||group==null||group.length()>80||world==null||dimension==null||area==null)throw new IllegalArgumentException("Invalid waypoint text");
        if(world.isBlank()||dimension.isBlank())throw new IllegalArgumentException("World and dimension are required");
        if(!java.util.Set.of("BOX","POINT","RING").contains(marker))throw new IllegalArgumentException("Marker: BOX, POINT or RING");
        color &= 0xFFFFFF;
    }
    public double distance(double px,double py,double pz){return Math.sqrt((x-px)*(x-px)+(y-py)*(y-py)+(z-pz)*(z-pz));}
    public double bearing(double px,double pz,double yaw){double a=Math.toDegrees(Math.atan2(-(x-px),z-pz))-yaw;return ((a+180)%360+360)%360-180;}
    public boolean matches(String w,String d,String a){return world.equals(w)&&dimension.equals(d)&&(area.isEmpty()||area.equals(a));}
}
