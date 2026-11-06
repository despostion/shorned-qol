package dev.shorned.qol.hud;
import dev.shorned.qol.config.Config;
import dev.shorned.qol.gui.*;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;
public final class WidgetSettingsScreen extends HubScreen {
    private final String id;
    public WidgetSettingsScreen(Screen parent,String id){super(parent,"Widget: "+id);this.id=id;}
    @Override protected void content(){var c=HudManager.config(id);
        action("Enabled: "+c.enabled,()->{c.enabled=!c.enabled;Config.save();rebuildWidgets();});
        action("Appearance",()->{var v=new LinkedHashMap<String,String>();v.put("Scale (0.5-3)",""+c.scale);v.put("Opacity (0-100)",""+c.opacity);v.put("Text hex RGB",String.format("%06X",c.textColor));v.put("Background hex RGB",String.format("%06X",c.backgroundColor));v.put("Padding (0-20)",""+c.padding);v.put("Spacing (0-16)",""+c.spacing);
            open(new FormScreen(this,"Appearance: "+id,v,m->{var n=new WidgetConfig();n.scale=Float.parseFloat(m.get("Scale (0.5-3)"));n.opacity=Integer.parseInt(m.get("Opacity (0-100)"));n.textColor=Integer.parseInt(m.get("Text hex RGB"),16);n.backgroundColor=Integer.parseInt(m.get("Background hex RGB"),16);n.padding=Integer.parseInt(m.get("Padding (0-20)"));n.spacing=Integer.parseInt(m.get("Spacing (0-16)"));n.sanitize();c.scale=n.scale;c.opacity=n.opacity;c.textColor=n.textColor;c.backgroundColor=n.backgroundColor;c.padding=n.padding;c.spacing=n.spacing;Config.save();}));});
        action("Background: "+c.background,()->{c.background=!c.background;Config.save();rebuildWidgets();});
        action("Border: "+c.border,()->{c.border=!c.border;Config.save();rebuildWidgets();});
        action("Reset widget",()->open(new ConfirmScreen(this,"Reset "+id+"?",()->{Config.get().widgets.put(id,HudManager.defaults(new ArrayList<>(HudManager.WIDGETS.keySet()).indexOf(id)));Config.save();})));
    }
}
