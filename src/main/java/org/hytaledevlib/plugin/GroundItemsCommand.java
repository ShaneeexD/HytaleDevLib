package org.hytaledevlib.plugin;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * Command to test the GroundItems plugin.
 */
public class GroundItemsCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;

    public GroundItemsCommand(String pluginName, String pluginVersion) {
        super("grounditems", "Display information about the " + pluginName + " plugin.");
        this.setPermissionGroup(GameMode.Adventure);
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("GroundItems v" + pluginVersion + " - A mod that will display text above ground items!"));
        ctx.sendMessage(Message.raw("Note: Full rendering functionality requires additional API access."));
    }
}
