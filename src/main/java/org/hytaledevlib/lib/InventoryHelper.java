package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class InventoryHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static boolean giveItem(Entity entity, String itemId, int quantity) {
        if (!(entity instanceof LivingEntity)) {
            LOGGER.at(Level.WARNING).log("Entity is not a LivingEntity, cannot give items");
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            ItemStack itemStack = new ItemStack(itemId, quantity);
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            
            ListTransaction<ItemStackTransaction> transaction = combined.addItemStacks(List.of(itemStack));
            
            return transaction.succeeded();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error giving item: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeItem(Entity entity, String itemId, int quantity) {
        if (!(entity instanceof LivingEntity)) {
            LOGGER.at(Level.WARNING).log("Entity is not a LivingEntity, cannot remove items");
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            
            ItemStack itemToRemove = new ItemStack(itemId, quantity);
            ItemStackTransaction transaction = combined.removeItemStack(itemToRemove);
            
            return transaction.succeeded();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error removing item: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasItem(Entity entity, String itemId, int quantity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            
            ItemStack itemToCheck = new ItemStack(itemId, quantity);
            return combined.canRemoveItemStack(itemToCheck);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking item: " + e.getMessage());
            return false;
        }
    }

    public static int countItem(Entity entity, String itemId) {
        if (!(entity instanceof LivingEntity)) {
            return 0;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            int count = 0;
            
            count += countItemInContainer(inventory.getHotbar(), itemId);
            count += countItemInContainer(inventory.getStorage(), itemId);
            count += countItemInContainer(inventory.getBackpack(), itemId);
            
            return count;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error counting item: " + e.getMessage());
            return 0;
        }
    }

    private static int countItemInContainer(ItemContainer container, String itemId) {
        int count = 0;
        short capacity = container.getCapacity();
        
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && itemId.equals(stack.getItemId())) {
                count += stack.getQuantity();
            }
        }
        
        return count;
    }

    public static List<ItemStack> getAllItems(Entity entity) {
        List<ItemStack> items = new ArrayList<>();
        
        if (!(entity instanceof LivingEntity)) {
            return items;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            addItemsFromContainer(inventory.getHotbar(), items);
            addItemsFromContainer(inventory.getStorage(), items);
            addItemsFromContainer(inventory.getBackpack(), items);
            addItemsFromContainer(inventory.getArmor(), items);
            addItemsFromContainer(inventory.getUtility(), items);
            
            return items;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting all items: " + e.getMessage());
            return items;
        }
    }

    private static void addItemsFromContainer(ItemContainer container, List<ItemStack> items) {
        short capacity = container.getCapacity();
        
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null) {
                items.add(stack);
            }
        }
    }

    @Nullable
    public static ItemStack getActiveHotbarItem(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return null;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            byte activeSlot = inventory.getActiveHotbarSlot();
            if (activeSlot >= 0) {
                return inventory.getHotbar().getItemStack((short) activeSlot);
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting active hotbar item: " + e.getMessage());
            return null;
        }
    }

    public static boolean setHotbarSlot(Entity entity, int slotIndex, ItemStack itemStack) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            if (slotIndex < 0 || slotIndex >= inventory.getHotbar().getCapacity()) {
                LOGGER.at(Level.WARNING).log("Invalid hotbar slot index: " + slotIndex);
                return false;
            }
            
            ItemStackSlotTransaction transaction = inventory.getHotbar().setItemStackForSlot((short) slotIndex, itemStack);
            return transaction.succeeded();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error setting hotbar slot: " + e.getMessage());
            return false;
        }
    }

    public static boolean clearInventory(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            inventory.getHotbar().clear();
            inventory.getStorage().clear();
            inventory.getBackpack().clear();
            
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error clearing inventory: " + e.getMessage());
            return false;
        }
    }

    public static boolean isInventoryFull(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return true;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            ItemContainer combined = inventory.getCombinedHotbarFirst();
            
            ItemStack testStack = new ItemStack("Rock_Stone", 1);
            return !combined.canAddItemStacks(List.of(testStack));
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking if inventory is full: " + e.getMessage());
            return true;
        }
    }

    public static int getEmptySlotCount(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return 0;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            int emptySlots = 0;
            
            emptySlots += countEmptySlotsInContainer(inventory.getHotbar());
            emptySlots += countEmptySlotsInContainer(inventory.getStorage());
            emptySlots += countEmptySlotsInContainer(inventory.getBackpack());
            
            return emptySlots;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error counting empty slots: " + e.getMessage());
            return 0;
        }
    }

    private static int countEmptySlotsInContainer(ItemContainer container) {
        int count = 0;
        short capacity = container.getCapacity();
        
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack == null) {
                count++;
            }
        }
        
        return count;
    }

    public static byte getActiveHotbarSlotIndex(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return -1;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            return inventory.getActiveHotbarSlot();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting active hotbar slot: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Replace an item in the entity's inventory/hotbar with a different item.
     * Searches through hotbar, storage, and backpack for the old item and replaces it.
     * 
     * @param entity The entity whose inventory to search
     * @param oldItemId The item ID to find and replace
     * @param newItemId The item ID to replace with
     * @param quantity The quantity of the new item
     * @return true if the item was found and replaced, false otherwise
     */
    public static boolean replaceItem(Entity entity, String oldItemId, String newItemId, int quantity) {
        if (!(entity instanceof LivingEntity)) {
            LOGGER.at(Level.WARNING).log("Entity is not a LivingEntity, cannot replace items");
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            // Try to find and replace in hotbar first
            if (replaceItemInContainer(inventory.getHotbar(), oldItemId, newItemId, quantity)) {
                return true;
            }
            
            // Then try storage
            if (replaceItemInContainer(inventory.getStorage(), oldItemId, newItemId, quantity)) {
                return true;
            }
            
            // Finally try backpack
            if (replaceItemInContainer(inventory.getBackpack(), oldItemId, newItemId, quantity)) {
                return true;
            }
            
            LOGGER.at(Level.WARNING).log("Item " + oldItemId + " not found in inventory");
            return false;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error replacing item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to replace an item in a specific container.
     * 
     * @param container The container to search
     * @param oldItemId The item ID to find
     * @param newItemId The item ID to replace with
     * @param quantity The quantity of the new item
     * @return true if the item was found and replaced
     */
    private static boolean replaceItemInContainer(ItemContainer container, String oldItemId, String newItemId, int quantity) {
        short capacity = container.getCapacity();
        
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && oldItemId.equals(stack.getItemId())) {
                // Found the item, replace it
                try {
                    ItemStack newStack = new ItemStack(newItemId, quantity);
                    ItemStackSlotTransaction transaction = container.setItemStackForSlot(i, newStack);
                    if (transaction.succeeded()) {
                        LOGGER.at(Level.INFO).log("Replaced " + oldItemId + " with " + newItemId + " in slot " + i);
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Error creating replacement item: " + e.getMessage());
                    return false;
                }
            }
        }
        
        return false;
    }

    /**
     * Change the state of the item in the entity's active hand slot.
     * This is useful for items that have multiple states (like buckets: empty, filled with water, etc.)
     * 
     * Uses ItemStack.withState() to create a new item with the specified state.
     * 
     * @param entity The entity whose active hand item to change
     * @param state The state to change to (e.g., "Filled_Water" for buckets)
     * @return true if the item's state was changed, false otherwise
     */
    public static boolean changeItemStateInActiveHand(Entity entity, String state) {
        if (!(entity instanceof LivingEntity)) {
            LOGGER.at(Level.WARNING).log("Entity is not a LivingEntity, cannot change item state");
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            byte activeSlot = inventory.getActiveHotbarSlot();
            
            ItemStack currentStack = inventory.getHotbar().getItemStack(activeSlot);
            if (currentStack == null || ItemStack.isEmpty(currentStack)) {
                LOGGER.at(Level.WARNING).log("No item in active hand slot");
                return false;
            }
            
            try {
                ItemStack newStack = currentStack.withState(state);
                ItemStackSlotTransaction transaction = inventory.getHotbar().setItemStackForSlot(activeSlot, newStack);
                if (transaction.succeeded()) {
                    LOGGER.at(Level.INFO).log("Changed active hand item '" + currentStack.getItemId() + "' to state '" + state + "' (new ID: " + newStack.getItemId() + ")");
                    return true;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.at(Level.WARNING).log("Invalid state '" + state + "' for item " + currentStack.getItemId() + ": " + e.getMessage());
                return false;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error changing item state in active hand: " + e.getMessage());
            return false;
        }
    }

    /**
     * Change the state of an item in the entity's inventory.
     * This is useful for items that have multiple states (like buckets: empty, filled with water, etc.)
     * 
     * Uses ItemStack.withState() to create a new item with the specified state.
     * 
     * @param entity The entity whose inventory to search
     * @param itemId The item ID to find
     * @param state The state to change to (e.g., "Filled_Water" for buckets)
     * @return true if the item was found and its state was changed, false otherwise
     */
    public static boolean changeItemState(Entity entity, String itemId, String state) {
        if (!(entity instanceof LivingEntity)) {
            LOGGER.at(Level.WARNING).log("Entity is not a LivingEntity, cannot change item state");
            return false;
        }

        try {
            LivingEntity livingEntity = (LivingEntity) entity;
            Inventory inventory = livingEntity.getInventory();
            
            // Try to find and change state in hotbar first
            if (changeItemStateInContainer(inventory.getHotbar(), itemId, state)) {
                return true;
            }
            
            // Then try storage
            if (changeItemStateInContainer(inventory.getStorage(), itemId, state)) {
                return true;
            }
            
            // Finally try backpack
            if (changeItemStateInContainer(inventory.getBackpack(), itemId, state)) {
                return true;
            }
            
            LOGGER.at(Level.WARNING).log("Item " + itemId + " not found in inventory");
            return false;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error changing item state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to change an item's state in a specific container.
     * 
     * @param container The container to search
     * @param itemId The item ID to find
     * @param state The state to change to
     * @return true if the item was found and state was changed
     */
    private static boolean changeItemStateInContainer(ItemContainer container, String itemId, String state) {
        short capacity = container.getCapacity();
        
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            if (stack != null && itemId.equals(stack.getItemId())) {
                // Found the item, change its state
                try {
                    ItemStack newStack = stack.withState(state);
                    ItemStackSlotTransaction transaction = container.setItemStackForSlot(i, newStack);
                    if (transaction.succeeded()) {
                        LOGGER.at(Level.INFO).log("Changed " + itemId + " to state '" + state + "' in slot " + i + " (new ID: " + newStack.getItemId() + ")");
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.at(Level.WARNING).log("Invalid state '" + state + "' for item " + itemId + ": " + e.getMessage());
                    return false;
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Error changing item state: " + e.getMessage());
                    return false;
                }
            }
        }
        
        return false;
    }
}
