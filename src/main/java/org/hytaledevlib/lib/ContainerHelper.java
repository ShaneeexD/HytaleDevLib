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
import java.util.function.Consumer;

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
    
    // Flag to prevent infinite loop when reverting transactions
    private static final ThreadLocal<Boolean> isReverting = ThreadLocal.withInitial(() -> false);
    
    /**
     * Parsed container transaction details with cancellation support.
     */
    public static class ContainerTransaction {
        private final String action;
        private final String itemId;
        private final int quantity;
        private final String rawTransaction;
        private final ItemContainer container;
        private final com.hypixel.hytale.server.core.inventory.transaction.Transaction transaction;
        private boolean cancelled = false;
        
        public ContainerTransaction(String action, String itemId, int quantity, String rawTransaction,
                                   ItemContainer container, com.hypixel.hytale.server.core.inventory.transaction.Transaction transaction) {
            this.action = action;
            this.itemId = itemId;
            this.quantity = quantity;
            this.rawTransaction = rawTransaction;
            this.container = container;
            this.transaction = transaction;
        }
        
        public String getAction() { return action; }
        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public String getRawTransaction() { return rawTransaction; }
        public ItemContainer getContainer() { return container; }
        public com.hypixel.hytale.server.core.inventory.transaction.Transaction getTransaction() { return transaction; }
        
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        
        public boolean isAdded() { return "ADDED".equals(action); }
        public boolean isRemoved() { return "REMOVED".equals(action); }
        public boolean isMoved() { return "MOVED".equals(action); }
        public boolean isSet() { return "SET".equals(action); }
    }
    
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
     * 
     * Automatically registers listeners on containers as they are placed by players,
     * and unregisters them when destroyed.
     * 
     * @param world The world to enable auto-tracking for
     * @param callback Consumer that receives parsed ContainerTransaction for all container changes
     */
    public static void enableAutoTracking(World world, Consumer<ContainerTransaction> callback) {
        String worldName = world.getName();
        // Store as raw callback internally
        BiConsumer<ItemContainer, ItemContainer.ItemContainerChangeEvent> rawCallback = (container, event) -> {
            // Skip if we're currently reverting a transaction (prevents infinite loop)
            if (isReverting.get()) {
                return;
            }
            
            ContainerTransaction transaction = parseTransaction(container, event);
            callback.accept(transaction);
            
            // Handle cancellation
            if (transaction.isCancelled()) {
                revertContainerTransaction(transaction);
            }
        };
        globalCallbacks.put(worldName, rawCallback);
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
    public static void enableAutoTrackingSimple(World world, Consumer<String> callback) {
        enableAutoTracking(world, (transaction) -> {
            callback.accept(transaction.getRawTransaction());
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
     * Parse a transaction event into a ContainerTransaction object.
     * Handles MoveTransaction to correctly detect if items were added or removed from THIS container.
     * 
     * @param container The container where the transaction occurred
     * @param event The ItemContainerChangeEvent to parse
     * @return Parsed ContainerTransaction with action, itemId, and quantity
     */
    public static ContainerTransaction parseTransaction(ItemContainer container, ItemContainer.ItemContainerChangeEvent event) {
        String transactionStr = event.transaction().toString();
        
        // Log the raw transaction for debugging
        LOGGER.atInfo().log("=== CONTAINER TRANSACTION DEBUG ===");
        LOGGER.atInfo().log("Transaction Class: " + event.transaction().getClass().getSimpleName());
        LOGGER.atInfo().log("Raw Transaction: " + transactionStr);
        
        ContainerTransaction parsed = parseTransactionString(transactionStr);
        
        LOGGER.atInfo().log("Parsed Action: " + parsed.getAction());
        LOGGER.atInfo().log("Parsed ItemId: " + parsed.getItemId());
        LOGGER.atInfo().log("Parsed Quantity: " + parsed.getQuantity());
        LOGGER.atInfo().log("===================================");
        
        // Create new transaction with container and transaction objects for cancellation support
        return new ContainerTransaction(parsed.getAction(), parsed.getItemId(), parsed.getQuantity(), 
                                       parsed.getRawTransaction(), container, event.transaction());
    }
    
    /**
     * Parse a transaction string into a ContainerTransaction object.
     * 
     * @param transaction The transaction string to parse
     * @return Parsed ContainerTransaction with action, itemId, and quantity (without container/transaction objects)
     */
    private static ContainerTransaction parseTransactionString(String transaction) {
        String itemId = null;
        int quantity = 0;
        String action = "UNKNOWN";
        
        // For ListTransaction (used by "take all" button), parse the first MoveTransaction in the list
        if (transaction.startsWith("ListTransaction{")) {
            // Extract the first MoveTransaction from the list
            int firstMoveStart = transaction.indexOf("MoveTransaction{");
            if (firstMoveStart != -1) {
                // Find the matching closing brace for this MoveTransaction
                int braceCount = 1;
                int pos = firstMoveStart + "MoveTransaction{".length();
                while (pos < transaction.length() && braceCount > 0) {
                    if (transaction.charAt(pos) == '{') braceCount++;
                    else if (transaction.charAt(pos) == '}') braceCount--;
                    pos++;
                }
                // Extract just the first MoveTransaction
                String firstMove = transaction.substring(firstMoveStart, pos);
                // Recursively parse this MoveTransaction
                return parseTransactionString(firstMove);
            }
        }
        // For ItemStackTransaction (used by individual quick move), check the action field
        else if (transaction.startsWith("ItemStackTransaction{")) {
            // ItemStackTransaction with action=REMOVE means items are being removed from this container
            if (transaction.contains("action=REMOVE")) {
                action = "REMOVED";
            } else if (transaction.contains("action=ADD")) {
                action = "ADDED";
            } else if (transaction.contains("action=SET")) {
                action = "SET";
            }
        }
        // For MoveTransaction, we need to determine if items were added or removed from THIS container
        else if (transaction.startsWith("MoveTransaction{")) {
            // Check moveType to determine direction
            // moveType=MOVE_FROM_SELF means items are leaving this container (REMOVED)
            // moveType=MOVE_TO_SELF means items are entering this container (ADDED)
            if (transaction.contains("moveType=MOVE_FROM_SELF")) {
                action = "REMOVED";
            } else if (transaction.contains("moveType=MOVE_TO_SELF")) {
                action = "ADDED";
            } else {
                // Fallback: check removeTransaction for action=REMOVE
                int removeTransStart = transaction.indexOf("removeTransaction=");
                if (removeTransStart != -1) {
                    int removeTransEnd = transaction.indexOf("moveType=", removeTransStart);
                    if (removeTransEnd != -1) {
                        String removeSection = transaction.substring(removeTransStart, removeTransEnd);
                        if (removeSection.contains("action=REMOVE")) {
                            action = "REMOVED";
                        }
                    }
                }
                
                // If still unknown, check addTransaction
                if (action.equals("UNKNOWN")) {
                    int addTransStart = transaction.indexOf("addTransaction=");
                    if (addTransStart != -1) {
                        String addSection = transaction.substring(addTransStart);
                        if (addSection.contains("action=ADD") || addSection.contains("action=SET")) {
                            action = "ADDED";
                        }
                    }
                }
            }
        } else {
            // For other transaction types, use simple action detection
            if (transaction.contains("action=ADD")) {
                action = "ADDED";
            } else if (transaction.contains("action=REMOVE")) {
                action = "REMOVED";
            } else if (transaction.contains("action=SET")) {
                action = "SET";
            } else if (transaction.contains("action=REPLACE")) {
                action = "REPLACED";
            } else if (transaction.contains("action=CLEAR")) {
                action = "CLEARED";
            }
        }
        
        // Parse itemId and quantity from transaction string
        int itemIdStart = transaction.indexOf("itemId=");
        if (itemIdStart != -1) {
            int itemIdEnd = transaction.indexOf(",", itemIdStart);
            if (itemIdEnd != -1) {
                itemId = transaction.substring(itemIdStart + 7, itemIdEnd);
            }
            
            // Find quantity after itemId
            int quantityStart = transaction.indexOf("quantity=", itemIdStart);
            if (quantityStart != -1) {
                int quantityEnd = transaction.indexOf(",", quantityStart);
                if (quantityEnd != -1) {
                    try {
                        quantity = Integer.parseInt(transaction.substring(quantityStart + 9, quantityEnd));
                    } catch (NumberFormatException e) {
                        quantity = 0;
                    }
                }
            }
        }
        
        return new ContainerTransaction(action, itemId, quantity, transaction, null, null);
    }
    
    /**
     * Check if a block type is a known container type.
     * Handles block state variations like _State_Definitions_OpenWindow and _State_Definitions_CloseWindow.
     * Also strips leading asterisks (*) from block type IDs.
     * 
     * @param blockTypeId The block type ID to check
     * @return true if this is a container type
     */
    public static boolean isContainerType(String blockTypeId) {
        // Strip leading asterisks (*, **) from the block type ID
        String cleanBlockTypeId = blockTypeId.replaceFirst("^\\*+", "");
        
        // First check exact match
        if (CONTAINER_BLOCK_TYPES.contains(cleanBlockTypeId)) {
            return true;
        }
        
        // Check if the cleanBlockTypeId starts with any known container type
        // This handles state variations like "Furniture_Crude_Chest_Small_State_Definitions_OpenWindow"
        for (String containerType : CONTAINER_BLOCK_TYPES) {
            if (cleanBlockTypeId.startsWith(containerType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Register a callback for when items change in a container at a specific position.
     * Callback receives parsed ContainerTransaction with action, itemId, and quantity.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives parsed ContainerTransaction
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerChange(World world, Vector3i position, 
                                           Consumer<ContainerTransaction> callback) {
        return onContainerChangeRaw(world, position, (container, event) -> {
            // Skip if we're currently reverting a transaction (prevents infinite loop)
            if (isReverting.get()) {
                return;
            }
            
            ContainerTransaction transaction = parseTransaction(container, event);
            callback.accept(transaction);
            
            // Handle cancellation
            if (transaction.isCancelled()) {
                revertContainerTransaction(transaction);
            }
        });
    }
    
    /**
     * Register a callback for when items change in a container at a specific position.
     * Callback receives raw container and event objects.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback BiConsumer that receives the container and event
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerChangeRaw(World world, Vector3i position, 
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
     * Register a simplified callback that only receives transaction string.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives transaction string
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerChangeSimple(World world, Vector3i position, 
                                                  Consumer<String> callback) {
        return onContainerChangeRaw(world, position, (container, event) -> {
            String transactionStr = event.transaction().toString();
            callback.accept(transactionStr);
        });
    }
    
    /**
     * Register a callback for when items are added to a container.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives parsed ContainerTransaction
     * @return true if successfully registered, false if no container exists at position
     */
    /**
     * Revert a container transaction (for cancellation).
     * Attempts to restore the container to its state before the transaction.
     */
    private static void revertContainerTransaction(ContainerTransaction transaction) {
        try {
            ItemContainer container = transaction.getContainer();
            com.hypixel.hytale.server.core.inventory.transaction.Transaction trans = transaction.getTransaction();
            
            if (container == null || trans == null) {
                LOGGER.atWarning().log("Cannot revert transaction: missing container or transaction object");
                return;
            }
            
            // Set flag to prevent infinite loop
            isReverting.set(true);
            
            // Try to extract slot information using reflection
            try {
                // For ListTransaction (take all button), iterate through all MoveTransactions
                if (trans.getClass().getSimpleName().equals("ListTransaction")) {
                    try {
                        java.lang.reflect.Method getList = trans.getClass().getMethod("getList");
                        Object listObj = getList.invoke(trans);
                        
                        if (listObj instanceof java.util.List<?>) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> transactionList = (java.util.List<Object>)listObj;
                            
                            // Revert each MoveTransaction in the list
                            for (Object moveTransObj : transactionList) {
                                if (moveTransObj == null) continue;
                                
                                // Create a temporary ContainerTransaction for this MoveTransaction
                                ContainerTransaction tempTrans = new ContainerTransaction(
                                    transaction.getAction(),
                                    transaction.getItemId(),
                                    transaction.getQuantity(),
                                    transaction.getRawTransaction(),
                                    container,
                                    (com.hypixel.hytale.server.core.inventory.transaction.Transaction)moveTransObj
                                );
                                
                                // Recursively revert this individual MoveTransaction
                                revertContainerTransaction(tempTrans);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Failed to handle ListTransaction: " + e.getMessage());
                    }
                    return;
                }
                // For MoveTransaction, we need to reverse the move
                else if (trans.getClass().getSimpleName().equals("MoveTransaction")) {
                    java.lang.reflect.Method getMoveType = trans.getClass().getMethod("getMoveType");
                    java.lang.reflect.Method getRemoveTransaction = trans.getClass().getMethod("getRemoveTransaction");
                    java.lang.reflect.Method getAddTransaction = trans.getClass().getMethod("getAddTransaction");
                    
                    Object moveType = getMoveType.invoke(trans);
                    String moveTypeStr = moveType != null ? moveType.toString() : "";
                    Object removeTransaction = getRemoveTransaction.invoke(trans);
                    Object addTransaction = getAddTransaction.invoke(trans);
                    
                    if ("MOVE_FROM_SELF".equals(moveTypeStr)) {
                        // Item was removed from container - restore it to chest AND remove from player inventory
                        if (removeTransaction != null && addTransaction != null) {
                            // Step 1: Restore item to chest
                            java.lang.reflect.Method getSlot = removeTransaction.getClass().getMethod("getSlot");
                            java.lang.reflect.Method getSlotBefore = removeTransaction.getClass().getMethod("getSlotBefore");
                            
                            int chestSlot = ((Short)getSlot.invoke(removeTransaction)).intValue();
                            Object slotBefore = getSlotBefore.invoke(removeTransaction);
                            
                            if (slotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                container.setItemStackForSlot((short)chestSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)slotBefore);
                            }
                            
                            // Step 2: Get otherContainer (player inventory) and remove the item from there
                            try {
                                java.lang.reflect.Method getOtherContainer = trans.getClass().getMethod("getOtherContainer");
                                Object otherContainerObj = getOtherContainer.invoke(trans);
                                
                                if (otherContainerObj instanceof ItemContainer) {
                                    ItemContainer playerInventory = (ItemContainer)otherContainerObj;
                                    
                                    // Try simple slot transaction first (normal click)
                                    try {
                                        java.lang.reflect.Method getAddSlot = addTransaction.getClass().getMethod("getSlot");
                                        java.lang.reflect.Method getAddSlotBefore = addTransaction.getClass().getMethod("getSlotBefore");
                                        
                                        int playerSlot = ((Short)getAddSlot.invoke(addTransaction)).intValue();
                                        Object playerSlotBefore = getAddSlotBefore.invoke(addTransaction);
                                        
                                        // Restore player inventory slot to its previous state (usually null)
                                        if (playerSlotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                            playerInventory.setItemStackForSlot((short)playerSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)playerSlotBefore);
                                        } else {
                                            playerInventory.setItemStackForSlot((short)playerSlot, null);
                                        }
                                        
                                    } catch (NoSuchMethodException e) {
                                        // This is ItemStackTransaction (shift-click) - handle slotTransactions list
                                        try {
                                            java.lang.reflect.Method getSlotTransactions = addTransaction.getClass().getMethod("getSlotTransactions");
                                            Object slotTransactionsObj = getSlotTransactions.invoke(addTransaction);
                                            
                                            if (slotTransactionsObj instanceof java.util.List<?>) {
                                                @SuppressWarnings("unchecked")
                                                java.util.List<Object> slotTransactions = (java.util.List<Object>)slotTransactionsObj;
                                                
                                                // Revert all successful slot transactions in player inventory
                                                for (Object slotTrans : slotTransactions) {
                                                    if (slotTrans == null) continue;
                                                    
                                                    try {
                                                        java.lang.reflect.Method succeeded = slotTrans.getClass().getMethod("succeeded");
                                                        if (!(Boolean)succeeded.invoke(slotTrans)) continue;
                                                        
                                                        java.lang.reflect.Method getSlotMethod = slotTrans.getClass().getMethod("getSlot");
                                                        java.lang.reflect.Method getSlotBeforeMethod = slotTrans.getClass().getMethod("getSlotBefore");
                                                        
                                                        int playerSlot = ((Short)getSlotMethod.invoke(slotTrans)).intValue();
                                                        Object playerSlotBefore = getSlotBeforeMethod.invoke(slotTrans);
                                                        
                                                        if (playerSlotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                                            playerInventory.setItemStackForSlot((short)playerSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)playerSlotBefore);
                                                        } else {
                                                            playerInventory.setItemStackForSlot((short)playerSlot, null);
                                                        }
                                                    } catch (Exception ex) {
                                                        // Silently continue on individual slot failure
                                                    }
                                                }
                                            }
                                        } catch (Exception ex) {
                                            LOGGER.atWarning().log("Failed to handle ItemStackTransaction: " + ex.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.atWarning().log("Could not access otherContainer: " + e.getMessage());
                            }
                        }
                        return;
                    } else if ("MOVE_TO_SELF".equals(moveTypeStr)) {
                        // Item was added to container - remove from chest AND restore to player inventory
                        if (addTransaction != null && removeTransaction != null) {
                            // Try ItemStackSlotTransaction first (single slot - normal click)
                            try {
                                java.lang.reflect.Method getSlot = addTransaction.getClass().getMethod("getSlot");
                                java.lang.reflect.Method getSlotBefore = addTransaction.getClass().getMethod("getSlotBefore");
                                
                                int chestSlot = ((Short)getSlot.invoke(addTransaction)).intValue();
                                Object chestSlotBefore = getSlotBefore.invoke(addTransaction);
                                
                                // Step 1: Restore chest slot
                                if (chestSlotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                    container.setItemStackForSlot((short)chestSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)chestSlotBefore);
                                } else {
                                    container.setItemStackForSlot((short)chestSlot, null);
                                }
                                
                                // Step 2: Restore player inventory
                                try {
                                    java.lang.reflect.Method getOtherContainer = trans.getClass().getMethod("getOtherContainer");
                                    Object otherContainerObj = getOtherContainer.invoke(trans);
                                    
                                    if (otherContainerObj instanceof ItemContainer) {
                                        ItemContainer playerInventory = (ItemContainer)otherContainerObj;
                                        
                                        java.lang.reflect.Method getRemoveSlot = removeTransaction.getClass().getMethod("getSlot");
                                        java.lang.reflect.Method getRemoveSlotBefore = removeTransaction.getClass().getMethod("getSlotBefore");
                                        
                                        int playerSlot = ((Short)getRemoveSlot.invoke(removeTransaction)).intValue();
                                        Object playerSlotBefore = getRemoveSlotBefore.invoke(removeTransaction);
                                        
                                        if (playerSlotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                            playerInventory.setItemStackForSlot((short)playerSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)playerSlotBefore);
                                        } else {
                                            playerInventory.setItemStackForSlot((short)playerSlot, null);
                                        }
                                        
                                    }
                                } catch (Exception ex) {
                                    LOGGER.atWarning().log("Could not access otherContainer: " + ex.getMessage());
                                }
                                return;
                            } catch (NoSuchMethodException e) {
                                // This is ItemStackTransaction (shift-click) - handle slotTransactions list
                                try {
                                    java.lang.reflect.Method getSlotTransactions = addTransaction.getClass().getMethod("getSlotTransactions");
                                    Object slotTransactionsObj = getSlotTransactions.invoke(addTransaction);
                                    
                                    if (slotTransactionsObj instanceof java.util.List<?>) {
                                        @SuppressWarnings("unchecked")
                                        java.util.List<Object> slotTransactions = (java.util.List<Object>)slotTransactionsObj;
                                        
                                        // Step 1: Revert all successful slot transactions in chest
                                        for (Object slotTrans : slotTransactions) {
                                            if (slotTrans == null) continue;
                                            
                                            try {
                                                java.lang.reflect.Method succeeded = slotTrans.getClass().getMethod("succeeded");
                                                if (!(Boolean)succeeded.invoke(slotTrans)) continue;
                                                
                                                java.lang.reflect.Method getSlot = slotTrans.getClass().getMethod("getSlot");
                                                java.lang.reflect.Method getSlotBefore = slotTrans.getClass().getMethod("getSlotBefore");
                                                
                                                int slot = ((Short)getSlot.invoke(slotTrans)).intValue();
                                                Object slotBefore = getSlotBefore.invoke(slotTrans);
                                                
                                                if (slotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                                    container.setItemStackForSlot((short)slot, (com.hypixel.hytale.server.core.inventory.ItemStack)slotBefore);
                                                } else {
                                                    container.setItemStackForSlot((short)slot, null);
                                                }
                                            } catch (Exception ex) {
                                                // Silently continue on individual slot failure
                                            }
                                        }
                                        
                                        // Step 2: Restore player inventory
                                        try {
                                            java.lang.reflect.Method getOtherContainer = trans.getClass().getMethod("getOtherContainer");
                                            Object otherContainerObj = getOtherContainer.invoke(trans);
                                            
                                            if (otherContainerObj instanceof ItemContainer) {
                                                ItemContainer playerInventory = (ItemContainer)otherContainerObj;
                                                
                                                java.lang.reflect.Method getRemoveSlot = removeTransaction.getClass().getMethod("getSlot");
                                                java.lang.reflect.Method getRemoveSlotBefore = removeTransaction.getClass().getMethod("getSlotBefore");
                                                
                                                int playerSlot = ((Short)getRemoveSlot.invoke(removeTransaction)).intValue();
                                                Object playerSlotBefore = getRemoveSlotBefore.invoke(removeTransaction);
                                                
                                                if (playerSlotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                                                    playerInventory.setItemStackForSlot((short)playerSlot, (com.hypixel.hytale.server.core.inventory.ItemStack)playerSlotBefore);
                                                } else {
                                                    playerInventory.setItemStackForSlot((short)playerSlot, null);
                                                }
                                            }
                                        } catch (Exception ex2) {
                                            LOGGER.atWarning().log("Could not restore player inventory: " + ex2.getMessage());
                                        }
                                        
                                    }
                                } catch (Exception ex) {
                                    LOGGER.atWarning().log("Failed to handle ItemStackTransaction: " + ex.getMessage());
                                }
                            }
                        }
                        return;
                    }
                }
                // For standalone ItemStackSlotTransaction (drag outside UI to drop)
                // We need to restore the item to the chest AND remove the dropped item entity
                else if (trans.getClass().getSimpleName().equals("ItemStackSlotTransaction")) {
                    LOGGER.atInfo().log("ItemStackSlotTransaction detected - will restore and remove dropped item");
                    
                    String itemId = transaction.getItemId();
                    int quantity = transaction.getQuantity();
                    
                    // Find the world and position from tracked containers
                    for (Map.Entry<String, Map<Vector3i, ContainerListener>> worldEntry : trackedContainers.entrySet()) {
                        String worldName = worldEntry.getKey();
                        for (Map.Entry<Vector3i, ContainerListener> containerEntry : worldEntry.getValue().entrySet()) {
                            if (containerEntry.getValue().container == container) {
                                Vector3i position = containerEntry.getKey();
                                
                                // Get the world from Universe
                                World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldName);
                                
                                if (world != null) {
                                    // Schedule removal of dropped items near the container after a short delay
                                    WorldHelper.waitTicks(world, 3, () -> {
                                        EntityHelper.removeNearbyItemEntities(world, position, itemId, quantity, 5.0);
                                        LOGGER.atInfo().log("Attempted to remove dropped item: " + quantity + "x " + itemId);
                                    });
                                }
                                
                                // Don't return - let it fall through to the SlotTransaction handler below to restore the item
                                break;
                            }
                        }
                    }
                    // Fall through to SlotTransaction handler to restore the item
                }
                
                // For simple SlotTransaction (direct slot changes)
                try {
                    java.lang.reflect.Method getSlot = trans.getClass().getMethod("getSlot");
                    java.lang.reflect.Method getSlotBefore = trans.getClass().getMethod("getSlotBefore");
                    
                    int slot = ((Short)getSlot.invoke(trans)).intValue();
                    Object slotBefore = getSlotBefore.invoke(trans);
                    
                    if (slotBefore instanceof com.hypixel.hytale.server.core.inventory.ItemStack) {
                        container.setItemStackForSlot((short)slot, (com.hypixel.hytale.server.core.inventory.ItemStack)slotBefore);
                    } else {
                        container.setItemStackForSlot((short)slot, null);
                    }
                } catch (NoSuchMethodException e) {
                    // Not a simple slot transaction, that's okay
                }
                
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                LOGGER.atWarning().log("Could not revert transaction using reflection: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to revert container transaction: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always clear the flag
            isReverting.set(false);
        }
    }
    
    public static boolean onContainerItemAdd(World world, Vector3i position, 
                                            Consumer<ContainerTransaction> callback) {
        return onContainerChange(world, position, (transaction) -> {
            if (transaction.isAdded()) {
                callback.accept(transaction);
            }
        });
    }
    
    /**
     * Register a callback for when items are removed from a container.
     * 
     * @param world The world containing the container
     * @param position The block position of the container
     * @param callback Consumer that receives parsed ContainerTransaction
     * @return true if successfully registered, false if no container exists at position
     */
    public static boolean onContainerItemRemove(World world, Vector3i position, 
                                               Consumer<ContainerTransaction> callback) {
        return onContainerChange(world, position, (transaction) -> {
            if (transaction.isRemoved()) {
                callback.accept(transaction);
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
