package dev.shorned.qol.gui;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
/** Paged form; draft values survive resize/page changes, Escape cancels without mutation. */
public final class FormScreen extends Screen {
    private final Screen parent;
    private final LinkedHashMap<String,String> values;
    private final Consumer<Map<String,String>> save;
    private final Map<String,EditBox> inputs=new LinkedHashMap<>();
    private int page;private String error="";
    public FormScreen(Screen parent,String title,LinkedHashMap<String,String> values,Consumer<Map<String,String>> save){super(Component.literal(title));this.parent=parent;this.values=values;this.save=save;}
    @Override protected void init(){
        inputs.clear();int count=Math.max(1,(height-100)/36), w=Math.min(380,width-24),x=(width-w)/2;
        var keys=new ArrayList<>(values.keySet());page=Math.clamp(page,0,Math.max(0,(keys.size()-1)/count));
        for(int i=page*count;i<Math.min(keys.size(),(page+1)*count);i++){
            String key=keys.get(i);var e=new EditBox(font,x,44+(i-page*count)*36,w,20,Component.literal(key));e.setMaxLength(512);e.setValue(values.get(key));e.setResponder(v->values.put(key,v));inputs.put(key,e);addRenderableWidget(e);
        }
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(x,height-28,w/4-3,20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"),b->{try{save.accept(Map.copyOf(values));onClose();}catch(RuntimeException e){error=e.getMessage()==null?"Invalid value":e.getMessage();}}).bounds(x+w/4,height-28,w/4-3,20).build());
        var prev=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;rebuildWidgets();}).bounds(x+w/2,height-28,w/4-3,20).build());prev.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;rebuildWidgets();}).bounds(x+w*3/4,height-28,w/4-3,20).build());next.active=(page+1)*count<keys.size();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){g.fill(0,0,width,height,0xF0101520);super.extractRenderState(g,x,y,dt);g.text(font,title,12,12,0xFFFFB347,true);for(var e:inputs.entrySet())g.text(font,e.getKey(),e.getValue().getX(),e.getValue().getY()-10,0xFFDDDDDD,true);g.text(font,font.plainSubstrByWidth(error,width-20),10,height-42,0xFFFF7777,true);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
