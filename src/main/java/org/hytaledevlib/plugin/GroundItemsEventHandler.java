package org.hytaledevlib.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Event handler for ground items - logs when items are dropped.
 * 
 * Note: DropItemEvent.Drop is the actual event subclass that contains the ItemStack.
 * The base DropItemEvent is an ECS event that doesn't directly expose entity components.
 */
public class GroundItemsEventHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void onItemDrop(@Nonnull DropItemEvent event) {
        try {
            LOGGER.atInfo().log("DropItemEvent received! Event type: " + event.getClass().getName());
            
            // Check if this is a Drop event (which has the ItemStack)
            if (event instanceof DropItemEvent.Drop dropEvent) {
                ItemStack itemStack = dropEvent.getItemStack();
                
                LOGGER.atInfo().log("Drop event detected! ItemStack: " + (itemStack != null ? "present" : "null"));
                
                if (itemStack != null && !itemStack.isEmpty()) {
                    String itemId = itemStack.getItemId();
                    int quantity = itemStack.getQuantity();
                    
                    String displayText = quantity > 1 ? itemId + " x" + quantity : itemId;
                    LOGGER.atInfo().log("Item dropped: " + displayText);
                    
                    // TODO: Add DisplayNameComponent to the spawned entity
                    // This requires access to the entity reference which isn't exposed by DropItemEvent
                    // May need to use an ECS system instead of event handling
                } else {
                    LOGGER.atInfo().log("ItemStack is null or empty");
                }
            } else if (event instanceof DropItemEvent.PlayerRequest) {
                LOGGER.atInfo().log("PlayerRequest event detected");
            } else {
                LOGGER.atInfo().log("Unknown DropItemEvent subtype");
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to process dropped item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onItemDropDirect(@Nonnull DropItemEvent.Drop event) {
        LOGGER.atInfo().log("=== DropItemEvent.Drop DIRECT ===");
        ItemStack itemStack = event.getItemStack();
        if (itemStack != null && !itemStack.isEmpty()) {
            LOGGER.atInfo().log("Item: " + itemStack.getItemId() + " x" + itemStack.getQuantity());
        }
    }

    public static void onPlayerDropRequest(@Nonnull DropItemEvent.PlayerRequest event) {
        LOGGER.atInfo().log("=== DropItemEvent.PlayerRequest ===");
        LOGGER.atInfo().log("Player requested to drop an item!");
    }

    public static void onBlockBreak(@Nonnull BreakBlockEvent event) {
        LOGGER.atInfo().log("=== BreakBlockEvent ===");
        LOGGER.atInfo().log("A block was broken!");
    }
}
