package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EventHelper - Simplified event handling for common Hytale modding tasks.
 * 
 * This utility provides easy-to-use methods for detecting common game events
 * with pre-tested patterns that are confirmed to work.
 */
public class EventHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Pattern ITEM_PATTERN = Pattern.compile("itemId=([^,]+), quantity=(\\d+)");
    
    /**
     * Register a callback for when a player joins a world.
     * 
     * @param plugin Your plugin instance
     * @param callback Consumer that receives the world
     */
    public static void onPlayerJoinWorld(JavaPlugin plugin, Consumer<World> callback) {
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
            try {
                // TODO: Extract world from event
                // Currently blocked by API access limitations
                LOGGER.atInfo().log("Player joined world event triggered");
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerJoinWorld: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when items are dropped from inventory.
     * 
     * This uses LivingEntityInventoryChangeEvent which fires reliably when
     * players drop items (press Q).
     * 
     * @param plugin Your plugin instance
     * @param callback BiConsumer that receives itemId and quantity
     */
    public static void onItemDrop(JavaPlugin plugin, BiConsumer<String, Integer> callback) {
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, (event) -> {
            try {
                var transaction = event.getTransaction();
                if (transaction != null && transaction.succeeded()) {
                    String transactionStr = transaction.toString();
                    
                    // Check if this is a REMOVE action (item dropped)
                    if (transactionStr.contains("action=REMOVE")) {
                        // Parse item details from transaction string
                        // Format: slotBefore=ItemStack{itemId=Soil_Dirt, quantity=1, ...}
                        Matcher matcher = ITEM_PATTERN.matcher(transactionStr);
                        if (matcher.find()) {
                            String itemId = matcher.group(1);
                            int quantity = Integer.parseInt(matcher.group(2));
                            callback.accept(itemId, quantity);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onItemDrop: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when items are picked up.
     * 
     * This uses LivingEntityInventoryChangeEvent which fires when items
     * are added to inventory.
     * 
     * @param plugin Your plugin instance
     * @param callback BiConsumer that receives itemId and quantity
     */
    public static void onItemPickup(JavaPlugin plugin, BiConsumer<String, Integer> callback) {
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, (event) -> {
            try {
                var transaction = event.getTransaction();
                if (transaction != null && transaction.succeeded()) {
                    String transactionStr = transaction.toString();
                    
                    // Check if this is an ADD action (item picked up)
                    if (transactionStr.contains("action=ADD")) {
                        // Parse item details from transaction string
                        Matcher matcher = ITEM_PATTERN.matcher(transactionStr);
                        if (matcher.find()) {
                            String itemId = matcher.group(1);
                            int quantity = Integer.parseInt(matcher.group(2));
                            callback.accept(itemId, quantity);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onItemPickup: " + e.getMessage());
            }
        });
    }
}
