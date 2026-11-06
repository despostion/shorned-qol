package dev.shorned.qol.hud;
public class WidgetConfig {
    public boolean enabled=true, background=true, border=true;
    public double x=0.02,y=0.02;
    public float scale=1;
    public int opacity=60,textColor=0xFFFFFF,backgroundColor=0x101520,padding=5,spacing=3;
    public void sanitize(){
        x=Double.isFinite(x)?Math.clamp(x,0,1):0.02; y=Double.isFinite(y)?Math.clamp(y,0,1):0.02;
        scale=Float.isFinite(scale)?Math.clamp(scale,0.5f,3):1;
        opacity=Math.clamp(opacity,0,100);padding=Math.clamp(padding,0,20);spacing=Math.clamp(spacing,0,16);
        textColor &= 0xFFFFFF; backgroundColor &= 0xFFFFFF;
    }
}
