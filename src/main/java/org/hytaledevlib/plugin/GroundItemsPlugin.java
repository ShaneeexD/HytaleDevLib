package org.hytaledevlib.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GroundItems Plugin - Displays text labels above items on the ground.
 * 
 * This plugin is a foundation for rendering text above ground items.
 * The full implementation requires deeper integration with Hytale's ECS system.
 */
public class GroundItemsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Timer itemLabelTimer;
    private World cachedWorld;

    public GroundItemsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("GroundItems v" + this.getManifest().getVersion().toString() + " loaded!");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up GroundItems plugin...");
        
        // Player joins world - we can use this to access the world later
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
            LOGGER.atInfo().log("=== Player joined world! ===");
            // TODO: Implement world access and entity querying
            // This would require proper ECS system setup or alternative approach
        });
        
        // Test BreakBlockEvent (we know this type of event works)
        this.getEventRegistry().registerGlobal(BreakBlockEvent.class, (event) -> {
            LOGGER.atInfo().log("=== BreakBlockEvent received! ===");
            LOGGER.atInfo().log("Block broken at: " + event.getTargetBlock());
        });
        
        // Test InteractivelyPickupItemEvent
        this.getEventRegistry().registerGlobal(InteractivelyPickupItemEvent.class, (event) -> {
            LOGGER.atInfo().log("=== InteractivelyPickupItemEvent received! ===");
            LOGGER.atInfo().log("Event type: " + event.getClass().getName());
        });
        
        // Keep DropItemEvent listener (even though it doesn't fire)
        this.getEventRegistry().registerGlobal(DropItemEvent.class, (event) -> {
            LOGGER.atInfo().log("=== DropItemEvent received! ===");
            LOGGER.atInfo().log("Event type: " + event.getClass().getName());
        });
        
        // LivingEntityInventoryChangeEvent - fires when inventory changes (including drops!)
        // COMMENTED OUT - Using direct entity query approach instead
        /*
        this.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, (event) -> {
            LOGGER.atInfo().log("=== LivingEntityInventoryChangeEvent received! ===");
            
            try {
                var transaction = event.getTransaction();
                if (transaction != null) {
                    LOGGER.atInfo().log("Transaction type: " + transaction.getClass().getName());
                    LOGGER.atInfo().log("Transaction succeeded: " + transaction.succeeded());
                    LOGGER.atInfo().log("Transaction toString: " + transaction.toString());
                    // Can extract item from: slotBefore=ItemStack{itemId=Soil_Dirt, quantity=1...}
                }
                
                var container = event.getItemContainer();
                if (container != null) {
                    LOGGER.atInfo().log("Container type: " + container.getClass().getName());
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Error processing inventory change: " + e.getMessage());
                e.printStackTrace();
            }
        });
        */
        
        LOGGER.atInfo().log("Event-based detection disabled - using direct entity query approach instead");
        
        // Register command
        this.getCommandRegistry().registerCommand(new GroundItemsCommand(this.getName(), this.getManifest().getVersion().toString()));
        
        LOGGER.atInfo().log("GroundItems plugin setup complete!");
        LOGGER.atInfo().log("Listening for: AddPlayerToWorldEvent, BreakBlockEvent, InteractivelyPickupItemEvent, DropItemEvent, LivingEntityInventoryChangeEvent");
    }
}
