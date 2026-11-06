package dev.shorned.qol.hud;
import dev.shorned.qol.config.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class HudEditorScreen extends Screen {
    private final Screen parent;private String selected="mithril",drag;private double ox,oy;
    public HudEditorScreen(Screen parent){super(Component.literal("Shorned HUD editor"));this.parent=parent;}
    @Override protected void init(){int w=Math.min(460,width-12),x=(width-w)/2;
        addRenderableWidget(Button.builder(Component.literal("Select: "+selected),b->{var ids=new ArrayList<>(HudManager.WIDGETS.keySet());selected=ids.get((ids.indexOf(selected)+1)%ids.size());rebuildWidgets();}).bounds(x,height-48,w/2-2,20).build());
        addRenderableWidget(Button.builder(Component.literal("Appearance / visibility"),b->minecraft.gui.setScreen(new WidgetSettingsScreen(this,selected))).bounds(x+w/2,height-48,w/2,20).build());
        addRenderableWidget(Button.builder(Component.literal("Save & back"),b->onClose()).bounds(x,height-24,w/2-2,20).build());
        addRenderableWidget(Button.builder(Component.literal("Legacy layout editor"),b->minecraft.gui.setScreen(new LegacyHudEditorScreen(this))).bounds(x+w/2,height-24,w/2,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){HudManager.render(g,true);super.extractRenderState(g,x,y,dt);g.text(font,"Drag widgets. Grey = disabled. Positions snap to a 4px grid.",6,4,0xFFFFB347,true);var b=HudManager.bounds.get(selected);if(b!=null){g.fill(b[0],b[1],b[0]+b[2],b[1]+2,0xFFFFB347);}}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean d){if(super.mouseClicked(e,d))return true;if(e.button()!=0)return false;var ids=new ArrayList<>(HudManager.bounds.keySet());Collections.reverse(ids);for(String id:ids){int[] b=HudManager.bounds.get(id);if(e.x()>=b[0]&&e.x()<=b[0]+b[2]&&e.y()>=b[1]&&e.y()<=b[1]+b[3]){selected=drag=id;ox=e.x()-b[0];oy=e.y()-b[1];return true;}}return false;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(drag==null)return super.mouseDragged(e,dx,dy);var b=HudManager.bounds.get(drag);var c=HudManager.config(drag);c.x=Math.clamp(Math.round((e.x()-ox)/4)*4.0/Math.max(1,width-b[2]),0,1);c.y=Math.clamp(Math.round((e.y()-oy)/4)*4.0/Math.max(1,height-b[3]),0,1);return true;}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(drag!=null){drag=null;Config.save();rebuildWidgets();return true;}return super.mouseReleased(e);}
    @Override public void onClose(){Config.save();minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
