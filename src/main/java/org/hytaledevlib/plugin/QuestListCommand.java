package org.hytaledevlib.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.hytaledevlib.lib.PlayerHelper;
import org.hytaledevlib.lib.QuestHelper;

import javax.annotation.Nonnull;
import java.util.List;

public class QuestListCommand extends CommandBase {
    
    public QuestListCommand() {
        super("questlist", "List your active quests");
        this.setPermissionGroup(GameMode.Adventure);
    }
    
    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        Player player = ctx.senderAs(Player.class);
        
        List<QuestHelper.QuestProgress> activeQuests = QuestHelper.getActiveQuests(player);
        
        if (activeQuests.isEmpty()) {
            PlayerHelper.sendMessage(player, "§e[Quest] No active quests. Use /queststart to begin.");
        } else {
            PlayerHelper.sendMessage(player, "§e[Quest] Active Quests:");
            for (QuestHelper.QuestProgress progress : activeQuests) {
                QuestHelper.Quest quest = QuestHelper.getQuest(progress.getQuestId());
                if (quest != null) {
                    double percentage = QuestHelper.getQuestCompletionPercentage(player, quest.getId());
                    PlayerHelper.sendMessage(player, 
                        "  §7- " + quest.getName() + " (" + String.format("%.0f", percentage) + "%)");
                }
            }
        }
    }
}
