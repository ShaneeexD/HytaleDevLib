package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
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
     * @param callback BiConsumer that receives itemId and quantity actually dropped
     */
    public static void onItemDrop(JavaPlugin plugin, BiConsumer<String, Integer> callback) {
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, (event) -> {
            try {
                var transaction = event.getTransaction();
                if (transaction != null && transaction.succeeded()) {
                    String transactionStr = transaction.toString();
                    
                    // Check if this is a REMOVE action (item dropped)
                    // Exclude MOVE actions which are used for placing blocks
                    if (transactionStr.contains("action=REMOVE") && !transactionStr.contains("action=MOVE")) {
                        // Parse slotBefore and slotAfter to calculate actual quantity dropped
                        Pattern beforePattern = Pattern.compile("slotBefore=ItemStack\\{itemId=([^,]+), quantity=(\\d+)");
                        Pattern afterPattern = Pattern.compile("slotAfter=ItemStack\\{itemId=([^,]+), quantity=(\\d+)");
                        
                        Matcher beforeMatcher = beforePattern.matcher(transactionStr);
                        Matcher afterMatcher = afterPattern.matcher(transactionStr);
                        
                        if (beforeMatcher.find()) {
                            String itemId = beforeMatcher.group(1);
                            int beforeQty = Integer.parseInt(beforeMatcher.group(2));
                            int afterQty = 0;
                            
                            // If slotAfter exists, get its quantity
                            if (afterMatcher.find()) {
                                afterQty = Integer.parseInt(afterMatcher.group(2));
                            }
                            
                            // Calculate actual dropped quantity
                            int droppedQty = beforeQty - afterQty;
                            if (droppedQty > 0) {
                                callback.accept(itemId, droppedQty);
                            }
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
                        Pattern pattern = Pattern.compile("itemId=([^,]+), quantity=(\\d+)");
                        Matcher matcher = pattern.matcher(transactionStr);
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
    
    // NOTE: Block breaking and placing are available through EcsEventHelper!
    // These events require ECS systems, which are handled automatically by EcsEventHelper.
    // Use EcsEventHelper.onBlockBreak(world, callback) and EcsEventHelper.onBlockPlace(world, callback)
    // These must be called after you have a World instance (e.g., in AddPlayerToWorldEvent).
    
    // NOTE: Block interaction (F key) is not available through UseBlockEvent.
    // UseBlockEvent.Pre doesn't fire for the F key interaction in Hytale.
    // This may require a different event system or input handling approach.
    
    /**
     * Register a callback for when a player sends a chat message.
     * 
     * @param plugin Your plugin instance
     * @param callback BiConsumer that receives the player username and message content
     */
    public static void onPlayerChat(JavaPlugin plugin, BiConsumer<String, String> callback) {
        plugin.getEventRegistry().registerGlobal(PlayerChatEvent.class, (event) -> {
            try {
                String username = event.getSender().getUsername();
                String message = event.getContent();
                callback.accept(username, message);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerChat: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when a player disconnects from the server.
     * 
     * @param plugin Your plugin instance
     * @param callback Consumer that receives the player's username
     */
    public static void onPlayerDisconnect(JavaPlugin plugin, Consumer<String> callback) {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (event) -> {
            try {
                String username = event.getPlayerRef().getUsername();
                callback.accept(username);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerDisconnect: " + e.getMessage());
            }
        });
    }
    
    // NOTE: Crafting detection is not reliable through CraftRecipeEvent.
    // CraftRecipeEvent.Post doesn't fire consistently.
    // Crafting shows up as inventory ADD transactions with output field instead.
    // Use LivingEntityInventoryChangeEvent with action=ADD and output=ItemStack to detect crafting if needed.
    
    // NOTE: Gamemode changes are not available through ChangeGameModeEvent.
    // The /gamemode command uses the built-in CommandManager system, not ECS events.
    // ChangeGameModeEvent may be for a different gamemode system or not implemented yet.
}
