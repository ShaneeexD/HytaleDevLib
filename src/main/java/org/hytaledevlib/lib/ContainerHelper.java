package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * ContainerHelper - Utilities for tracking item container changes.
 * 
 * Provides easy-to-use methods for detecting when items are added, removed, or changed
 * in containers like chests, furnaces, and other block-based item storage.
 * 
 * Note: This helper tracks containers at specific block positions. You must register
 * each container you want to monitor.
 */
public class ContainerHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Track registered containers by world and position
    private static final Map<String, Map<Vector3i, ContainerListener>> trackedContainers = new ConcurrentHashMap<>();
    
    // Track global callbacks for auto-registration
    private static final Map<String, BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent>> globalCallbacks = new ConcurrentHashMap<>();
    
    // Track which worlds have auto-registration enabled
    private static final Map<String, Boolean> autoRegistrationEnabled = new ConcurrentHashMap<>();
    
    // List of container block types to auto-track
    private static final java.util.Set<String> CONTAINER_BLOCK_TYPES = new java.util.HashSet<>(java.util.Arrays.asList(
        // Workbenches
        "Bench_Alchemy",
        "Bench_Arcane",
        "Bench_Armory",
        "Bench_Armour",
        "Bench_Builders",
        "Bench_Campfire",
        "Bench_Cooking",
        "Bench_Farming",
        "Bench_Furnace",
        "Bench_Furniture",
        "Bench_Loom",
        "Bench_Lumbermill",
        "Bench_Memories",
        "Bench_Salvage",
        "Bench_Tannery",
        "Bench_Trough",
        "Bench_Weapon",
        "Bench_WorkBench",
        // Chests
        "Furniture_Ancient_Chest_Large",
        "Furniture_Ancient_Chest_Small",
        "Furniture_Christmas_Chest_Small",
        "Furniture_Christmas_Chest_Small_Green",
        "Furniture_Christmas_Chest_Small_Red",
        "Furniture_Christmas_Chest_Small_RedDotted",
        "Furniture_Christmas_Chest_Small_White",
        "Furniture_Crude_Chest_Large",
        "Furniture_Crude_Chest_Small",
        "Furniture_Desert_Chest_Large",
        "Furniture_Desert_Chest_Small",
        "Furniture_Dungeon_Chest_Epic",
        "Furniture_Dungeon_Chest_Epic_Large",
        "Furniture_Dungeon_Chest_Legendary_Large",
        "Furniture_Feran_Chest_Large",
        "Furniture_Feran_Chest_Small",
        "Furniture_Frozen_Castle_Chest_Large",
        "Furniture_Frozen_Castle_Chest_Small",
        "Furniture_Goblin_Chest_Small",
        "Furniture_Human_Ruins_Chest_Large",
        "Furniture_Human_Ruins_Chest_Small",
        "Furniture_Jungle_Chest_Large",
        "Furniture_Jungle_Chest_Small",
        "Furniture_Kweebec_Chest_Large",
        "Furniture_Kweebec_Chest_Small",
        "Furniture_Lumberjack_Chest_Large",
        "Furniture_Lumberjack_Chest_Small",
        "Furniture_Royal_Magic_Chest_Large",
        "Furniture_Royal_Magic_Chest_Small",
        "Furniture_Scarak_Hive_Chest_Large",
        "Furniture_Scarak_Hive_Chest_Small",
        "Furniture_Tavern_Chest_Large",
        "Furniture_Tavern_Chest_Small",
        "Furniture_Temple_Dark_Chest_Large",
        "Furniture_Temple_Dark_Chest_Small",
        "Furniture_Temple_Emerald_Chest_Large",
        "Furniture_Temple_Emerald_Chest_Small",
        "Furniture_Temple_Light_Chest_Large",
        "Furniture_Temple_Light_Chest_Small",
        "Furniture_Temple_Scarak_Chest_Large",
        "Furniture_Temple_Scarak_Chest_Small",
        "Furniture_Temple_Wind_Chest_Large",
        "Furniture_Temple_Wind_Chest_Small",
        "Furniture_Village_Chest_Large",
        "Furniture_Village_Chest_Small"
    ));
    
    /**
     * Enable automatic container tracking for a world.
     * This will automatically register listeners on all containers as they are placed,
     * and unregister them when destroyed.
     * 
     * @param world The world to enable auto-tracking for
     * @param callback The callback to invoke for all container changes
     */
    public static void enableAutoTracking(World world, BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent> callback) {
        String worldName = world.getName();
        globalCallbacks.put(worldName, callback);
        autoRegistrationEnabled.put(worldName, true);
        
        // Register block placement listener
        EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
            if (CONTAINER_BLOCK_TYPES.contains(itemId)) {
                LOGGER.atInfo().log("🔍 Container block placed detected: " + itemId + " at " + position);
                LOGGER.atInfo().log("   Waiting 2 ticks for block state to initialize...");
                
                // Wait for block state to be created
                WorldHelper.waitTicks(world, 2, () -> {
                    boolean success = onContainerChange(world, position, callback);
                    if (success) {
                        LOGGER.atInfo().log("✅ Successfully auto-registered container at " + position);
                        LOGGER.atInfo().log("   Type: " + itemId);
                        LOGGER.atInfo().log("   Total tracked: " + getTrackedContainerCount(world));
                    } else {
                        LOGGER.atWarning().log("❌ Failed to auto-register container at " + position);
                        LOGGER.atWarning().log("   Type: " + itemId);
                        LOGGER.atWarning().log("   Reason: No ItemContainerState found at position");
                    }
                });
            }
        });
        
        // Register block break listener
        EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
            if (CONTAINER_BLOCK_TYPES.contains(blockTypeId)) {
                unregisterContainer(world, position);
                LOGGER.atInfo().log("Auto-unregistered container at " + position + " (type: " + blockTypeId + ")");
            }
        });
        
        LOGGER.atInfo().log("Enabled auto-tracking for containers in world: " + worldName);
    }
    
    /**
     * Enable automatic container tracking with a simplified callback.
     * 
     * @param world The world to enable auto-tracking for
     * @param callback Consumer that receives transaction string
     */
    public static void enableAutoTrackingSimple(World world, java.util.function.Consumer<String> callback) {
        enableAutoTracking(world, (container, event) -> {
            callback.accept(event.transaction().toString());
        });
    }
    
    /**
     * Disable automatic container tracking for a world.
     * 
     * @param world The world to disable auto-tracking for
     */
    public static void disableAutoTracking(World world) {
        String worldName = world.getName();
        globalCallbacks.remove(worldName);
        autoRegistrationEnabled.remove(worldName);
        LOGGER.atInfo().log("Disabled auto-tracking for world: " + worldName);
    }
    
    /**
     * Check if auto-tracking is enabled for a world.
     * 
     * @param world The world to check
     * @return true if auto-tracking is enabled
     */
    public static boolean isAutoTrackingEnabled(World world) {
        return autoRegistrationEnabled.getOrDefault(world.getName(), false);
    }
    
    /**
     * Add a container block type to the auto-tracking list.
     * 
     * @param blockTypeId The block type ID to track (e.g., "Furniture_Custom_Chest")
     */
    public static void addContainerType(String blockTypeId) {
        CONTAINER_BLOCK_TYPES.add(blockTypeId);
        LOGGER.atInfo().log("Added container type to auto-tracking: " + blockTypeId);
    }
    
    /**
     * Check if a block type is a known container type.
     * 
     * @param blockTypeId The block type ID to check
     * @return true if this is a container type
     */
    public static boolean isContainerType(String blockTypeId) {
        return CONTAINER_BLOCK_TYPES.contains(blockTypeId);
    }
    
    /**
     * Register a callback for when items change in a container at a specific position.
     * 
     * This will get the container from the block state at the given position and register
     * a listener for all item changes (add, remove, move, etc.).
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback BiConsumer that receives the container and transaction details
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerChange(World world, Vector3i position, 
                                           BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent> callback) {
        try {
            // Get the block state at this position
            BlockState state = BlockStateHelper.getState(world, position.getX(), position.getY(), position.getZ());
            
            if (state instanceof ItemContainerState containerState) {
                ItemContainer container = containerState.getItemContainer();
                
                // Register the change event listener
                container.registerChangeEvent(event -> {
                    try {
                        callback.accept(container, event);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in container change callback: " + e.getMessage());
                    }
                });
                
                // Track this container
                String worldName = world.getName();
                trackedContainers.putIfAbsent(worldName, new ConcurrentHashMap<>());
                trackedContainers.get(worldName).put(position, new ContainerListener(container, callback));
                
                LOGGER.atInfo().log("Registered container listener at " + position);
                return true;
            } else {
                LOGGER.atWarning().log("No container found at position: " + position);
                return false;
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register container listener: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register a simplified callback that only receives transaction details.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives transaction string for parsing
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerChangeSimple(World world, Vector3i position, 
                                                  java.util.function.Consumer<String> callback) {
        return onContainerChange(world, position, (container, event) -> {
            String transactionStr = event.transaction().toString();
            callback.accept(transactionStr);
        });
    }
    
    /**
     * Register a callback for when items are added to a container.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives the transaction details
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerItemAdd(World world, Vector3i position, 
                                            java.util.function.Consumer<String> callback) {
        return onContainerChange(world, position, (container, event) -> {
            String transactionStr = event.transaction().toString();
            if (transactionStr.contains("action=ADD")) {
                callback.accept(transactionStr);
            }
        });
    }
    
    /**
     * Register a callback for when items are removed from a container.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives the transaction details
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerItemRemove(World world, Vector3i position, 
                                               java.util.function.Consumer<String> callback) {
        return onContainerChange(world, position, (container, event) -> {
            String transactionStr = event.transaction().toString();
            if (transactionStr.contains("action=REMOVE")) {
                callback.accept(transactionStr);
            }
        });
    }
    
    /**
     * Unregister container tracking at a specific position.
     * 
     * @param world The world containing the container
     * @param position The block position to stop tracking
     */
    public static void unregisterContainer(World world, Vector3i position) {
        String worldName = world.getName();
        Map<Vector3i, ContainerListener> worldContainers = trackedContainers.get(worldName);
        if (worldContainers != null) {
            worldContainers.remove(position);
            LOGGER.atInfo().log("Unregistered container at " + position);
        }
    }
    
    /**
     * Clear all tracked containers for a world.
     * 
     * @param world The world to clear container tracking for
     */
    public static void clearWorld(World world) {
        String worldName = world.getName();
        trackedContainers.remove(worldName);
        LOGGER.atInfo().log("Cleared all container tracking for world: " + worldName);
    }
    
    /**
     * Get the number of tracked containers in a world.
     * 
     * @param world The world to check
     * @return Number of tracked containers
     */
    public static int getTrackedContainerCount(World world) {
        String worldName = world.getName();
        Map<Vector3i, ContainerListener> worldContainers = trackedContainers.get(worldName);
        return worldContainers != null ? worldContainers.size() : 0;
    }
    
    /**
     * Internal class to track container listeners.
     */
    private static class ContainerListener {
        final ItemContainer container;
        final BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent> callback;
        
        ContainerListener(ItemContainer container, BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent> callback) {
            this.container = container;
            this.callback = callback;
        }
    }
}
