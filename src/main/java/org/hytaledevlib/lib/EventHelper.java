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
     * Note: This event fires when a player is being added to the world, but the player
     * may not be in world.getPlayers() yet. Use the callback to get the world reference,
     * then check for existing players or wait for them to be fully added.
     * 
     * @param plugin Your plugin instance
     * @param callback Consumer that receives the world
     */
    public static void onPlayerJoinWorld(JavaPlugin plugin, Consumer<World> callback) {
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
            try {
                World world = event.getWorld();
                callback.accept(world);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerJoinWorld: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when a player joins a world with access to the player's UUID and username.
     * 
     * This extracts the player UUID from the event's Holder component and retrieves the username
     * using EntityHelper after a short delay to ensure the player is added to the world.
     * 
     * @param plugin Your plugin instance
     * @param callback TriConsumer that receives the world, player UUID, and player username
     */
    public static void onPlayerJoinWorldWithUUID(JavaPlugin plugin, TriConsumer<World, java.util.UUID, String> callback) {
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
            try {
                World world = event.getWorld();
                com.hypixel.hytale.component.Holder<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> holder = event.getHolder();
                
                // Get UUID from UUIDComponent
                com.hypixel.hytale.server.core.entity.UUIDComponent uuidComp = holder.getComponent(
                    com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType()
                );
                
                if (uuidComp != null) {
                    java.util.UUID uuid = uuidComp.getUuid();
                    
                    // Wait a tick for player to be fully added to world, then get username
                    WorldHelper.waitTicks(world, 50, () -> {
                        com.hypixel.hytale.server.core.entity.Entity player = EntityHelper.getPlayerByUUID(world, uuid);
                        String username = player != null ? EntityHelper.getName(player) : "Unknown";
                        callback.accept(world, uuid, username);
                    });
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerJoinWorldWithUUID: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when a player disconnects from the server.
     * 
     * @param plugin Your plugin instance
     * @param callback BiConsumer that receives the player's UUID and username
     */
    public static void onPlayerDisconnect(JavaPlugin plugin, BiConsumer<java.util.UUID, String> callback) {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (event) -> {
            try {
                java.util.UUID uuid = event.getPlayerRef().getUuid();
                String username = event.getPlayerRef().getUsername();
                callback.accept(uuid, username);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerDisconnect: " + e.getMessage());
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
                    // Exclude MOVE actions (placing blocks) and MaterialTransaction (crafting)
                    if (transactionStr.contains("action=REMOVE") && 
                        !transactionStr.contains("action=MOVE") &&
                        !transactionStr.contains("MaterialTransaction{action=REMOVE")) {
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
                    // Exclude MOVE actions (placing blocks), crafting (MaterialTransaction), 
                    // and crafting output (ListTransaction starting with ItemStackTransaction ADD + allOrNothing=false, filter=true)
                    if (transactionStr.contains("action=ADD") && 
                        !transactionStr.contains("action=MOVE") &&
                        !transactionStr.contains("MaterialTransaction{action=REMOVE") &&
                        !(transactionStr.startsWith("ListTransaction{succeeded=true, list=[ItemStackTransaction{succeeded=true, action=ADD") && 
                          transactionStr.contains("allOrNothing=false, filter=true"))) {
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
     * Register a callback for when a player disconnects from the server (by username).
     * 
     * @param plugin Your plugin instance
     * @param callback Consumer that receives the player's username
     */
    public static void onPlayerDisconnectByName(JavaPlugin plugin, Consumer<String> callback) {
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (event) -> {
            try {
                String username = event.getPlayerRef().getUsername();
                callback.accept(username);
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onPlayerDisconnectByName: " + e.getMessage());
            }
        });
    }
    
    /**
     * Register a callback for when a player crafts an item.
     * 
     * NOTE: Crafting detection through LivingEntityInventoryChangeEvent.
     * Crafting shows up as ADD transactions with an "output=" field.
     * 
     * @param plugin Your plugin instance
     * @param callback BiConsumer that receives output item ID and quantity
     */
    public static void onCraftRecipe(JavaPlugin plugin, BiConsumer<String, Integer> callback) {
        plugin.getEventRegistry().registerGlobal(LivingEntityInventoryChangeEvent.class, (event) -> {
            try {
                var transaction = event.getTransaction();
                if (transaction != null && transaction.succeeded()) {
                    String transactionStr = transaction.toString();
                    
                    // Crafting creates TWO ListTransactions:
                    // 1. Materials removed: MaterialTransaction{action=REMOVE} + nested transactions
                    // 2. Item added: ItemStackTransaction{action=ADD} with allOrNothing=false, filter=true
                    // We detect the second one (the crafted item being added)
                    
                    // Pattern: ListTransaction with ONLY ItemStackTransaction ADD (no MaterialTransaction)
                    // and has allOrNothing=false, filter=true (distinguishes from regular pickups)
                    if (transactionStr.startsWith("ListTransaction{succeeded=true, list=[ItemStackTransaction{succeeded=true, action=ADD") &&
                        transactionStr.contains("allOrNothing=false, filter=true")) {
                        
                        // Parse the crafted item
                        Pattern craftPattern = Pattern.compile("ItemStackTransaction\\{succeeded=true, action=ADD, query=ItemStack\\{itemId=([^,]+), quantity=(\\d+)");
                        Matcher craftMatcher = craftPattern.matcher(transactionStr);
                        
                        if (craftMatcher.find()) {
                            String itemId = craftMatcher.group(1);
                            int quantity = Integer.parseInt(craftMatcher.group(2));
                            callback.accept(itemId, quantity);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Error in onCraftRecipe: " + e.getMessage());
            }
        });
    }
    
    /**
     * Functional interface for three-parameter callbacks.
     */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
    
    // NOTE: Gamemode changes are not available through ChangeGameModeEvent.
    // The /gamemode command uses the built-in CommandManager system, not ECS events.
    // ChangeGameModeEvent may be for a different gamemode system or not implemented yet.
}
