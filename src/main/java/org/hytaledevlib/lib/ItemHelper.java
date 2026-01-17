package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Helper class for working with items and item containers.
 * Provides simplified methods for creating items, managing containers, and common item operations.
 */
public class ItemHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Random RANDOM = new Random();
    
    /**
     * Create an item stack with the specified ID and quantity.
     * 
     * @param itemId The item ID (e.g., "Furniture_Crude_Torch")
     * @param quantity The quantity
     * @return The created ItemStack, or null if creation failed
     */
    @Nullable
    public static ItemStack createStack(String itemId, int quantity) {
        try {
            return new ItemStack(itemId, quantity);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error creating item stack: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create an item stack with quantity 1.
     * 
     * @param itemId The item ID
     * @return The created ItemStack, or null if creation failed
     */
    @Nullable
    public static ItemStack createStack(String itemId) {
        return createStack(itemId, 1);
    }
    
    /**
     * Add an item to a container in the first available slot.
     * 
     * @param container The container to add to
     * @param itemId The item ID
     * @param quantity The quantity
     * @return true if the item was fully added, false if there was a remainder
     */
    public static boolean addToContainer(ItemContainer container, String itemId, int quantity) {
        try {
            ItemStack stack = createStack(itemId, quantity);
            if (stack == null) return false;
            
            com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction transaction = 
                container.addItemStack(stack);
            
            return transaction.succeeded() && ItemStack.isEmpty(transaction.getRemainder());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error adding item to container: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Add an item stack to a specific slot in a container.
     * 
     * @param container The container
     * @param slot The slot index
     * @param itemId The item ID
     * @param quantity The quantity
     * @return true if successful
     */
    public static boolean addToContainerSlot(ItemContainer container, int slot, String itemId, int quantity) {
        try {
            ItemStack stack = createStack(itemId, quantity);
            if (stack == null) return false;
            
            container.addItemStackToSlot((short) slot, stack);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error adding item to container slot: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Add an item stack to a specific slot in a container.
     * 
     * @param container The container
     * @param slot The slot index
     * @param stack The item stack to add
     * @return true if successful
     */
    public static boolean addToContainerSlot(ItemContainer container, int slot, ItemStack stack) {
        try {
            container.addItemStackToSlot((short) slot, stack);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error adding item stack to container slot: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fill a container with multiple items in order.
     * Items are added to slots 0, 1, 2, etc.
     * 
     * @param container The container to fill
     * @param items Pairs of itemId and quantity (itemId1, qty1, itemId2, qty2, ...)
     * @return Number of items successfully added
     */
    public static int fillContainer(ItemContainer container, Object... items) {
        if (items.length % 2 != 0) {
            LOGGER.at(Level.WARNING).log("fillContainer requires pairs of (itemId, quantity)");
            return 0;
        }
        
        int added = 0;
        int slot = 0;
        
        for (int i = 0; i < items.length; i += 2) {
            try {
                String itemId = (String) items[i];
                int quantity = (Integer) items[i + 1];
                
                if (addToContainerSlot(container, slot, itemId, quantity)) {
                    added++;
                    slot++;
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error in fillContainer: " + e.getMessage());
            }
        }
        
        return added;
    }
    
    /**
     * Add an item to a random empty slot in a container.
     * 
     * @param container The container to add to
     * @param itemId The item ID
     * @param quantity The quantity
     * @return true if the item was added to a random slot, false if no empty slots available
     */
    public static boolean addToContainerRandom(ItemContainer container, String itemId, int quantity) {
        try {
            // Find all empty slots
            List<Short> emptySlots = new ArrayList<>();
            short capacity = container.getCapacity();
            
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack existing = container.getItemStack(slot);
                if (existing == null || ItemStack.isEmpty(existing)) {
                    emptySlots.add(slot);
                }
            }
            
            if (emptySlots.isEmpty()) {
                LOGGER.at(Level.WARNING).log("No empty slots available in container");
                return false;
            }
            
            // Pick a random empty slot
            short randomSlot = emptySlots.get(RANDOM.nextInt(emptySlots.size()));
            return addToContainerSlot(container, randomSlot, itemId, quantity);
            
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error adding item to random slot: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fill a container with multiple items in random empty slots.
     * Unlike fillContainer which uses sequential slots, this places items randomly.
     * 
     * @param container The container to fill
     * @param items Pairs of itemId and quantity (itemId1, qty1, itemId2, qty2, ...)
     * @return Number of items successfully added
     */
    public static int fillContainerRandom(ItemContainer container, Object... items) {
        if (items.length % 2 != 0) {
            LOGGER.at(Level.WARNING).log("fillContainerRandom requires pairs of (itemId, quantity)");
            return 0;
        }
        
        int added = 0;
        
        for (int i = 0; i < items.length; i += 2) {
            try {
                String itemId = (String) items[i];
                int quantity = (Integer) items[i + 1];
                
                if (addToContainerRandom(container, itemId, quantity)) {
                    added++;
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error in fillContainerRandom: " + e.getMessage());
            }
        }
        
        return added;
    }
    
    /**
     * Get all items from a container as a list.
     * 
     * @param container The container
     * @return List of all non-empty item stacks
     */
    public static List<ItemStack> getItemsFromContainer(ItemContainer container) {
        List<ItemStack> items = new ArrayList<>();
        
        try {
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack != null && !ItemStack.isEmpty(stack)) {
                    items.add(stack);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting items from container: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Get an item from a specific slot in a container.
     * 
     * @param container The container
     * @param slot The slot index
     * @return The item stack, or null if slot is empty
     */
    @Nullable
    public static ItemStack getItemFromSlot(ItemContainer container, int slot) {
        try {
            return container.getItemStack((short) slot);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting item from slot: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a container has space for an item stack.
     * 
     * @param container The container
     * @param stack The item stack to check
     * @return true if the container can fit the item
     */
    public static boolean hasSpace(ItemContainer container, ItemStack stack) {
        try {
            // Check if there's an empty slot
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack existing = container.getItemStack(slot);
                if (existing == null || ItemStack.isEmpty(existing)) {
                    return true;
                }
                // Check if it can stack with existing item
                if (existing.isStackableWith(stack)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking container space: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a container is empty.
     * 
     * @param container The container
     * @return true if the container has no items
     */
    public static boolean isEmpty(ItemContainer container) {
        try {
            return container.isEmpty();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking if container is empty: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Clear all items from a container.
     * 
     * @param container The container to clear
     * @return true if successful
     */
    public static boolean clearContainer(ItemContainer container) {
        try {
            container.clear();
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error clearing container: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Count how many of a specific item are in a container.
     * 
     * @param container The container
     * @param itemId The item ID to count
     * @return Total quantity of the item
     */
    public static int countItemInContainer(ItemContainer container, String itemId) {
        int total = 0;
        
        try {
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack != null && !ItemStack.isEmpty(stack) && stack.getItemId().equals(itemId)) {
                    total += stack.getQuantity();
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error counting items in container: " + e.getMessage());
        }
        
        return total;
    }
    
    /**
     * Remove a specific quantity of an item from a container.
     * 
     * @param container The container
     * @param itemId The item ID to remove
     * @param quantity The quantity to remove
     * @return true if the full quantity was removed
     */
    public static boolean removeFromContainer(ItemContainer container, String itemId, int quantity) {
        try {
            ItemStack template = createStack(itemId, quantity);
            if (template == null) return false;
            
            com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction transaction = 
                container.removeItemStack(template);
            
            return transaction.succeeded();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error removing item from container: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if two item stacks are stackable (same item, can combine).
     * 
     * @param stack1 First item stack
     * @param stack2 Second item stack
     * @return true if they can stack together
     */
    public static boolean areStackable(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) return false;
        try {
            return stack1.isStackableWith(stack2);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the item ID from an item stack.
     * 
     * @param stack The item stack
     * @return The item ID, or null if stack is null/empty
     */
    @Nullable
    public static String getItemId(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) return null;
        try {
            return stack.getItemId();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the quantity from an item stack.
     * 
     * @param stack The item stack
     * @return The quantity, or 0 if stack is null/empty
     */
    public static int getQuantity(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) return 0;
        try {
            return stack.getQuantity();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check if an item stack is empty or null.
     * 
     * @param stack The item stack to check
     * @return true if the stack is empty or null
     */
    public static boolean isEmptyStack(ItemStack stack) {
        return stack == null || ItemStack.isEmpty(stack);
    }
}
