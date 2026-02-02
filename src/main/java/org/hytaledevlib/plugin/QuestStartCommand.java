package org.hytaledevlib.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.hytaledevlib.lib.PlayerHelper;
import org.hytaledevlib.lib.QuestHelper;
import org.hytaledevlib.lib.EntityHelper;

import javax.annotation.Nonnull;

public class QuestStartCommand extends CommandBase {
    
    public QuestStartCommand() {
        super("queststart", "Start the Gather Wood quest");
        this.setPermissionGroup(GameMode.Adventure);
    }
    
    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        Player player = ctx.senderAs(Player.class);
        
        boolean started = QuestHelper.startQuest(player, "gather_wood");
        if (started) {
            PlayerHelper.sendMessage(player, 
                "§a[Quest] Started: Gather Wood - Collect 10 Wood_Oak_Trunk");
        } else {
            PlayerHelper.sendMessage(player, 
                "§c[Quest] Could not start quest (already active or completed)");
        }
    }
}
