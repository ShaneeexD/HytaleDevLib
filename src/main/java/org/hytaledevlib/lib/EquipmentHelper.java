package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * EquipmentHelper - Utilities for tracking armor and equipment changes.
 * 
 * Provides easy-to-use methods for detecting when entities equip or unequip armor,
 * tools, and utility items (offhand). Supports cancellation and item inspection.
 * 
 * Works with any LivingEntity, not just players.
 */
public class EquipmentHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Track registered equipment listeners by world and entity UUID
    private static final Map<String, Map<UUID, EquipmentListener>> trackedEntities = new ConcurrentHashMap<>();
    
    /**
     * Equipment change details with cancellation support.
     */
    public static class EquipmentChange {
        private final LivingEntity entity;
        private final EquipmentSlotType slotType;
        private final int slotIndex;
        private final ItemStack oldItem;
        private final ItemStack newItem;
        private final String action;
        private final Transaction transaction;
        private boolean cancelled = false;
        
        public EquipmentChange(LivingEntity entity, EquipmentSlotType slotType, int slotIndex, 
                              ItemStack oldItem, ItemStack newItem, String action, Transaction transaction) {
            this.entity = entity;
            this.slotType = slotType;
            this.slotIndex = slotIndex;
            this.oldItem = oldItem;
            this.newItem = newItem;
            this.action = action;
            this.transaction = transaction;
        }
        
        public LivingEntity getEntity() { return entity; }
        public EquipmentSlotType getSlotType() { return slotType; }
        public int getSlotIndex() { return slotIndex; }
        public ItemStack getOldItem() { return oldItem; }
        public ItemStack getNewItem() { return newItem; }
        public String getAction() { return action; }
        public Transaction getTransaction() { return transaction; }
        
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        
        public boolean isEquipping() { return "ADD".equals(action) || "SET".equals(action); }
        public boolean isUnequipping() { return "REMOVE".equals(action); }
        
        public String getOldItemId() { 
            return oldItem != null ? oldItem.getItemId() : null; 
        }
        
        public String getNewItemId() { 
            return newItem != null ? newItem.getItemId() : null; 
        }
        
        public int getOldQuantity() {
            return oldItem != null ? oldItem.getQuantity() : 0;
        }
        
        public int getNewQuantity() {
            return newItem != null ? newItem.getQuantity() : 0;
        }
        
        /**
         * Get the armor slot if this is an armor change.
         * @return ItemArmorSlot or null if not armor
         */
        public ItemArmorSlot getArmorSlot() {
            if (slotType == EquipmentSlotType.ARMOR && slotIndex >= 0 && slotIndex < ItemArmorSlot.VALUES.length) {
                return ItemArmorSlot.VALUES[slotIndex];
            }
            return null;
        }
    }
    
    /**
     * Equipment slot types.
     */
    public enum EquipmentSlotType {
        ARMOR,      // Head, Chest, Hands, Legs
        UTILITY,    // Offhand/utility items
        TOOLS,      // Tool slots
        HOTBAR,     // Hotbar (if tracking hotbar as equipment)
        STORAGE     // Main inventory (if tracking)
    }
    
    /**
     * Internal listener storage.
     */
    private static class EquipmentListener {
        final LivingEntity entity;
        final BiConsumer<EquipmentChange, ItemContainer> callback;
        final Function<EquipmentChange, Boolean> filter;
        
        EquipmentListener(LivingEntity entity, BiConsumer<EquipmentChange, ItemContainer> callback, 
                         Function<EquipmentChange, Boolean> filter) {
            this.entity = entity;
            this.callback = callback;
            this.filter = filter;
        }
    }
    
    /**
     * Register equipment change tracking for a specific entity.
     * 
     * @param world The world containing the entity
     * @param entity The living entity to track
     * @param callback Callback that receives equipment changes
     * @return true if successfully registered
     */
    public static boolean onEquipmentChange(World world, LivingEntity entity, 
                                           BiConsumer<EquipmentChange, ItemContainer> callback) {
        return onEquipmentChange(world, entity, callback, null);
    }
    
    /**
     * Register equipment change tracking for a specific entity with filtering.
     * 
     * @param world The world containing the entity
     * @param entity The living entity to track
     * @param callback Callback that receives equipment changes
     * @param filter Optional filter function (return true to process, false to skip)
     * @return true if successfully registered
     */
    public static boolean onEquipmentChange(World world, LivingEntity entity, 
                                           BiConsumer<EquipmentChange, ItemContainer> callback,
                                           Function<EquipmentChange, Boolean> filter) {
        try {
            // Get entity UUID using EntityHelper
            UUID entityUuid = EntityHelper.getUUID(entity);
            String worldName = world.getName();
            
            Inventory inventory = entity.getInventory();
            if (inventory == null) {
                LOGGER.atWarning().log("Entity has no inventory");
                return false;
            }
            
            // Register on armor container
            if (inventory.getArmor() != null) {
                inventory.getArmor().registerChangeEvent(event -> {
                    handleEquipmentChange(entity, EquipmentSlotType.ARMOR, event, callback, filter);
                });
            }
            
            // Register on utility container
            if (inventory.getUtility() != null) {
                inventory.getUtility().registerChangeEvent(event -> {
                    handleEquipmentChange(entity, EquipmentSlotType.UTILITY, event, callback, filter);
                });
            }
            
            // Register on tools container
            if (inventory.getTools() != null) {
                inventory.getTools().registerChangeEvent(event -> {
                    handleEquipmentChange(entity, EquipmentSlotType.TOOLS, event, callback, filter);
                });
            }
            
            // Register on hotbar container (optional - not typically used for equipment)
            if (inventory.getHotbar() != null) {
                inventory.getHotbar().registerChangeEvent(event -> {
                    handleEquipmentChange(entity, EquipmentSlotType.HOTBAR, event, callback, filter);
                });
            }
            
            // Register on storage container (optional - not typically used for equipment)
            if (inventory.getStorage() != null) {
                inventory.getStorage().registerChangeEvent(event -> {
                    handleEquipmentChange(entity, EquipmentSlotType.STORAGE, event, callback, filter);
                });
            }
            
            // Track this listener
            trackedEntities.putIfAbsent(worldName, new ConcurrentHashMap<>());
            trackedEntities.get(worldName).put(entityUuid, new EquipmentListener(entity, callback, filter));
            
            LOGGER.atInfo().log("Registered equipment tracking for entity: " + entityUuid);
            return true;
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register equipment tracking: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register equipment tracking for all players when they join a world.
     * 
     * @param world The world to track
     * @param callback Callback that receives equipment changes
     */
    public static void onPlayerEquipmentChange(World world, BiConsumer<EquipmentChange, ItemContainer> callback) {
        onPlayerEquipmentChange(world, callback, null);
    }
    
    /**
     * Register equipment tracking for all players when they join a world with filtering.
     * 
     * @param world The world to track
     * @param callback Callback that receives equipment changes
     * @param filter Optional filter function
     */
    public static void onPlayerEquipmentChange(World world, BiConsumer<EquipmentChange, ItemContainer> callback,
                                              Function<EquipmentChange, Boolean> filter) {
        // Register for all current players
        try {
            for (com.hypixel.hytale.server.core.entity.Entity entity : EntityHelper.getEntities(world)) {
                if (entity instanceof LivingEntity livingEntity) {
                    if (EntityHelper.isPlayer(entity)) {
                        onEquipmentChange(world, livingEntity, callback, filter);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register for current players: " + e.getMessage());
        }
        
        // TODO: Add event listener for new players joining (requires AddPlayerToWorldEvent)
        LOGGER.atInfo().log("Registered equipment tracking for all current players in world: " + world.getName());
    }
    
    /**
     * Track only armor changes (Head, Chest, Hands, Legs).
     * 
     * @param world The world containing the entity
     * @param entity The living entity to track
     * @param callback Callback that receives armor changes
     * @return true if successfully registered
     */
    public static boolean onArmorChange(World world, LivingEntity entity, 
                                       BiConsumer<EquipmentChange, ItemContainer> callback) {
        return onEquipmentChange(world, entity, callback, change -> 
            change.getSlotType() == EquipmentSlotType.ARMOR
        );
    }
    
    /**
     * Track only utility/offhand changes.
     * 
     * @param world The world containing the entity
     * @param entity The living entity to track
     * @param callback Callback that receives utility changes
     * @return true if successfully registered
     */
    public static boolean onUtilityChange(World world, LivingEntity entity, 
                                         BiConsumer<EquipmentChange, ItemContainer> callback) {
        return onEquipmentChange(world, entity, callback, change -> 
            change.getSlotType() == EquipmentSlotType.UTILITY
        );
    }
    
    /**
     * Track only tool changes.
     * 
     * @param world The world containing the entity
     * @param entity The living entity to track
     * @param callback Callback that receives tool changes
     * @return true if successfully registered
     */
    public static boolean onToolChange(World world, LivingEntity entity, 
                                      BiConsumer<EquipmentChange, ItemContainer> callback) {
        return onEquipmentChange(world, entity, callback, change -> 
            change.getSlotType() == EquipmentSlotType.TOOLS
        );
    }
    
    /**
     * Unregister equipment tracking for a specific entity.
     * 
     * @param world The world containing the entity
     * @param entity The entity to stop tracking
     */
    public static void unregisterEntity(World world, LivingEntity entity) {
        String worldName = world.getName();
        UUID entityUuid = EntityHelper.getUUID(entity);
        
        Map<UUID, EquipmentListener> worldTracking = trackedEntities.get(worldName);
        if (worldTracking != null) {
            worldTracking.remove(entityUuid);
            LOGGER.atInfo().log("Unregistered equipment tracking for entity: " + entityUuid);
        }
    }
    
    /**
     * Clear all equipment tracking for a world.
     * 
     * @param world The world to clear
     */
    public static void clearWorld(World world) {
        String worldName = world.getName();
        trackedEntities.remove(worldName);
        LOGGER.atInfo().log("Cleared all equipment tracking for world: " + worldName);
    }
    
    /**
     * Get the number of tracked entities in a world.
     * 
     * @param world The world to check
     * @return Number of tracked entities
     */
    public static int getTrackedEntityCount(World world) {
        Map<UUID, EquipmentListener> worldTracking = trackedEntities.get(world.getName());
        return worldTracking != null ? worldTracking.size() : 0;
    }
    
    /**
     * Handle equipment change event from a container.
     */
    private static void handleEquipmentChange(LivingEntity entity, EquipmentSlotType slotType,
                                             ItemContainer.ItemContainerChangeEvent event,
                                             BiConsumer<EquipmentChange, ItemContainer> callback,
                                             Function<EquipmentChange, Boolean> filter) {
        try {
            ItemContainer container = event.container();
            Transaction transaction = event.transaction();
            
            // Only process equipment slots (armor, utility, tools), not storage/hotbar
            // Storage/hotbar events fire when moving items TO/FROM equipment
            if (slotType == EquipmentSlotType.STORAGE || slotType == EquipmentSlotType.HOTBAR) {
                return; // Skip storage/hotbar events
            }
            
            // Parse transaction to get slot index and items
            EquipmentChange change = parseEquipmentChange(entity, slotType, transaction);
            if (change == null) {
                return;
            }
            
            // Apply filter if provided
            if (filter != null && !filter.apply(change)) {
                return;
            }
            
            // Call user callback
            callback.accept(change, container);
            
            // Handle cancellation (revert the change)
            if (change.isCancelled()) {
                revertEquipmentChange(container, change);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error in equipment change callback: " + e.getMessage());
        }
    }
    
    /**
     * Determine the equipment slot type from the container.
     */
    private static EquipmentSlotType getSlotType(LivingEntity entity, ItemContainer container) {
        try {
            Inventory inventory = entity.getInventory();
            if (inventory == null) return null;
            
            if (container == inventory.getArmor()) {
                return EquipmentSlotType.ARMOR;
            } else if (container == inventory.getUtility()) {
                return EquipmentSlotType.UTILITY;
            } else if (container == inventory.getTools()) {
                return EquipmentSlotType.TOOLS;
            } else if (container == inventory.getHotbar()) {
                return EquipmentSlotType.HOTBAR;
            } else if (container == inventory.getStorage()) {
                return EquipmentSlotType.STORAGE;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse transaction into EquipmentChange.
     */
    private static EquipmentChange parseEquipmentChange(LivingEntity entity, EquipmentSlotType slotType, 
                                                       Transaction transaction) {
        try {
            int slotIndex = -1;
            ItemStack oldItem = null;
            ItemStack newItem = null;
            String action = "UNKNOWN";

            try {
                // Handle MoveTransaction (most common for equipment changes)
                java.lang.reflect.Method getMoveType = transaction.getClass().getMethod("getMoveType");
                java.lang.reflect.Method getRemoveTransaction = transaction.getClass().getMethod("getRemoveTransaction");
                java.lang.reflect.Method getAddTransaction = transaction.getClass().getMethod("getAddTransaction");

                Object moveType = getMoveType.invoke(transaction);
                String moveTypeStr = moveType != null ? moveType.toString() : "";
                Object removeTransaction = getRemoveTransaction.invoke(transaction);
                Object addTransaction = getAddTransaction.invoke(transaction);

                if ("MOVE_FROM_SELF".equals(moveTypeStr)) {
                    // Unequipping: the slot modified in this container is the removeTransaction slot
                    if (removeTransaction != null) {
                        java.lang.reflect.Method getSlot = removeTransaction.getClass().getMethod("getSlot");
                        java.lang.reflect.Method getSlotBefore = removeTransaction.getClass().getMethod("getSlotBefore");
                        slotIndex = ((Short)getSlot.invoke(removeTransaction)).intValue();
                        Object slotBefore = getSlotBefore.invoke(removeTransaction);
                        if (slotBefore instanceof ItemStack) {
                            oldItem = (ItemStack) slotBefore;
                        }
                    }

                    action = "REMOVE";
                } else if ("MOVE_TO_SELF".equals(moveTypeStr)) {
                    // Equipping: the slot modified in this container is determined by the addTransaction
                    if (addTransaction != null) {
                        // SlotTransaction / ItemStackSlotTransaction
                        try {
                            java.lang.reflect.Method getSlot = addTransaction.getClass().getMethod("getSlot");
                            java.lang.reflect.Method getSlotBefore = addTransaction.getClass().getMethod("getSlotBefore");
                            java.lang.reflect.Method getSlotAfter = addTransaction.getClass().getMethod("getSlotAfter");

                            slotIndex = ((Short)getSlot.invoke(addTransaction)).intValue();
                            Object slotBefore = getSlotBefore.invoke(addTransaction);
                            Object slotAfter = getSlotAfter.invoke(addTransaction);

                            if (slotBefore instanceof ItemStack) {
                                oldItem = (ItemStack) slotBefore;
                            }
                            if (slotAfter instanceof ItemStack) {
                                newItem = (ItemStack) slotAfter;
                            }
                        } catch (NoSuchMethodException ignored) {
                            // ItemStackTransaction (shift-click, combined container moves, etc.)
                            try {
                                java.lang.reflect.Method getSlotTransactions = addTransaction.getClass().getMethod("getSlotTransactions");
                                Object slotTransactionsObj = getSlotTransactions.invoke(addTransaction);
                                if (slotTransactionsObj instanceof java.util.List<?> slotTransactions) {
                                    for (Object t : slotTransactions) {
                                        if (t == null) continue;
                                        try {
                                            java.lang.reflect.Method succeeded = t.getClass().getMethod("succeeded");
                                            if (!(Boolean)succeeded.invoke(t)) continue;

                                            java.lang.reflect.Method getSlot = t.getClass().getMethod("getSlot");
                                            java.lang.reflect.Method getSlotBefore = t.getClass().getMethod("getSlotBefore");
                                            java.lang.reflect.Method getSlotAfter = t.getClass().getMethod("getSlotAfter");

                                            int candidateSlot = ((Short)getSlot.invoke(t)).intValue();
                                            Object candidateAfter = getSlotAfter.invoke(t);

                                            // Only accept slot indices within the relevant equipment container capacity
                                            int maxSlot;
                                            if (slotType == EquipmentSlotType.ARMOR) {
                                                maxSlot = entity.getInventory().getArmor() != null ? entity.getInventory().getArmor().getCapacity() : 0;
                                            } else if (slotType == EquipmentSlotType.UTILITY) {
                                                maxSlot = entity.getInventory().getUtility() != null ? entity.getInventory().getUtility().getCapacity() : 0;
                                            } else if (slotType == EquipmentSlotType.TOOLS) {
                                                maxSlot = entity.getInventory().getTools() != null ? entity.getInventory().getTools().getCapacity() : 0;
                                            } else {
                                                maxSlot = 0;
                                            }

                                            if (maxSlot > 0 && (candidateSlot < 0 || candidateSlot >= maxSlot)) {
                                                continue;
                                            }

                                            if (candidateAfter instanceof ItemStack) {
                                                slotIndex = candidateSlot;
                                                Object candidateBefore = getSlotBefore.invoke(t);
                                                if (candidateBefore instanceof ItemStack) {
                                                    oldItem = (ItemStack) candidateBefore;
                                                }
                                                newItem = (ItemStack) candidateAfter;
                                                break;
                                            }
                                        } catch (Exception ignored2) {
                                            // continue
                                        }
                                    }
                                }
                            } catch (Exception ignored2) {
                                // continue
                            }
                        }
                    }

                    // Determine action from old/new
                    if (oldItem == null && newItem != null) {
                        action = "ADD";
                    } else if (oldItem != null && newItem == null) {
                        action = "REMOVE";
                    } else if (oldItem != null && newItem != null) {
                        action = "REPLACE";
                    } else {
                        action = "ADD";
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // Not a MoveTransaction; fall through to string parsing
            } catch (Exception e) {
                LOGGER.atWarning().log("Could not extract equipment change: " + e.getMessage());
            }

            // Fallback: parse slot/action from toString when reflection fails
            if (slotIndex == -1 || "UNKNOWN".equals(action)) {
                String transactionStr = transaction.toString();

                if (slotIndex == -1) {
                    int slotStart = transactionStr.indexOf("slot=");
                    if (slotStart != -1) {
                        int slotEnd = transactionStr.indexOf(",", slotStart);
                        if (slotEnd == -1) {
                            slotEnd = transactionStr.indexOf("}", slotStart);
                        }
                        if (slotEnd != -1) {
                            try {
                                slotIndex = Integer.parseInt(transactionStr.substring(slotStart + 5, slotEnd).trim());
                            } catch (NumberFormatException ignored2) {
                                // ignore
                            }
                        }
                    }
                }

                if ("UNKNOWN".equals(action)) {
                    if (transactionStr.contains("action=ADD")) {
                        action = "ADD";
                    } else if (transactionStr.contains("action=REMOVE")) {
                        action = "REMOVE";
                    } else if (transactionStr.contains("action=SET")) {
                        action = "SET";
                    } else if (transactionStr.contains("action=REPLACE")) {
                        action = "REPLACE";
                    }
                }
            }

            return new EquipmentChange(entity, slotType, slotIndex, oldItem, newItem, action, transaction);
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to parse equipment change: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Revert an equipment change (for cancellation).
     * Restores the old item to the equipment slot AND returns the new item to its source.
     */
    private static void revertEquipmentChange(ItemContainer container, EquipmentChange change) {
        try {
            int slotIndex = change.getSlotIndex();
            ItemStack oldItem = change.getOldItem();
            ItemStack newItem = change.getNewItem();
            Transaction transaction = change.getTransaction();
            
            if (slotIndex < 0 || slotIndex >= container.getCapacity()) {
                LOGGER.atWarning().log("Cannot revert equipment change: invalid slot index " + slotIndex);
                return;
            }
            
            // Step 1: Restore the old item to the equipment slot
            com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction restoreTransaction = 
                container.setItemStackForSlot((short) slotIndex, oldItem);
            
            if (!restoreTransaction.succeeded()) {
                LOGGER.atWarning().log("Failed to restore old item to equipment slot");
                return;
            }
            
            // Step 2: Return the new item to its source location (if it exists)
            if (newItem != null && !ItemStack.isEmpty(newItem)) {
                try {
                    // Try to extract source slot from MoveTransaction
                    java.lang.reflect.Method getRemoveTransaction = transaction.getClass().getMethod("getRemoveTransaction");
                    Object removeTransaction = getRemoveTransaction.invoke(transaction);
                    
                    if (removeTransaction != null) {
                        // Get the source slot where the item came from
                        java.lang.reflect.Method getSlot = removeTransaction.getClass().getMethod("getSlot");
                        int sourceSlot = ((Short)getSlot.invoke(removeTransaction)).intValue();
                        
                        // Get the source container (inventory) from the entity
                        LivingEntity entity = change.getEntity();
                        Inventory inventory = entity.getInventory();
                        
                        // The slot number is already correct for its container
                        // We just need to figure out which container it came from
                        // Try both hotbar and storage to see which one has the capacity
                        ItemContainer sourceContainer = null;
                        
                        if (change.getSlotType() == EquipmentSlotType.ARMOR) {
                            // Check if slot is within hotbar range
                            if (sourceSlot >= 0 && sourceSlot < inventory.getHotbar().getCapacity()) {
                                sourceContainer = inventory.getHotbar();
                                LOGGER.atInfo().log("DEBUG: Using hotbar, slot: " + sourceSlot);
                            } 
                            // Otherwise it's in storage
                            else if (sourceSlot >= 0 && sourceSlot < inventory.getStorage().getCapacity()) {
                                sourceContainer = inventory.getStorage();
                                LOGGER.atInfo().log("DEBUG: Using storage, slot: " + sourceSlot);
                            }
                        }
                        
                        // Return the item to its source slot (no adjustment needed)
                        if (sourceContainer != null && sourceSlot >= 0 && sourceSlot < sourceContainer.getCapacity()) {
                            com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction returnTransaction = 
                                sourceContainer.setItemStackForSlot((short) sourceSlot, newItem);
                            
                            if (returnTransaction.succeeded()) {
                                LOGGER.atInfo().log("Successfully cancelled equipment change - item returned to slot " + sourceSlot);
                            } else {
                                LOGGER.atWarning().log("Restored equipment slot but failed to return item to source");
                            }
                        } else {
                            LOGGER.atWarning().log("Could not determine source container/slot - item may be lost");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("Failed to return item to source: " + e.getMessage());
                }
            } else {
                LOGGER.atInfo().log("Successfully cancelled equipment change for slot " + slotIndex);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to revert equipment change: " + e.getMessage());
        }
    }
}
