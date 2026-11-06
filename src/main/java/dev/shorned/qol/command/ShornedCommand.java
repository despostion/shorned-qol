package dev.shorned.qol.command;
import com.mojang.brigadier.CommandDispatcher;
import dev.shorned.qol.ShornedQolClient;
import dev.shorned.qol.gui.HubScreen;
import dev.shorned.qol.hud.HudEditorScreen;
import dev.shorned.qol.util.Msg;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.minecraft.commands.CommandBuildContext;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
public final class ShornedCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> d,CommandBuildContext ctx){
        var root=literal("shorned").executes(c->open("Overview"));
        for(String name:new String[]{"mining","fishing","settings","debug"})
            root.then(literal(name).executes(c->open(name.equals("debug") ? "Debug & Tools" : Character.toUpperCase(name.charAt(0))+name.substring(1))));
        root.then(literal("hud").executes(c->{ShornedQolClient.pendingScreen=new HudEditorScreen(new HubScreen(null,"Overview"));return 1;}));
        root.then(literal("help").executes(c->{c.getSource().sendFeedback(Msg.of("/shorned [mining|fishing|hud|settings|debug]. Legacy commands remain under /sqol."));return 1;}));
        d.register(root);
    }
    private static int open(String section){ShornedQolClient.pendingScreen=new HubScreen(null,section);return 1;}
}
