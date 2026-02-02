package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
    
    // Item Entity Methods
    
    /**
     * Represents a dropped item entity in the world.
     * Contains the entity reference and item information.
     */
    public static class ItemEntity {
        private final com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref;
        private final String itemId;
        private final int quantity;
        private final com.hypixel.hytale.math.vector.Vector3d position;
        
        public ItemEntity(
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
            String itemId,
            int quantity,
            com.hypixel.hytale.math.vector.Vector3d position
        ) {
            this.ref = ref;
            this.itemId = itemId;
            this.quantity = quantity;
            this.position = position;
        }
        
        public com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> getRef() {
            return ref;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public com.hypixel.hytale.math.vector.Vector3d getPosition() {
            return position;
        }
    }
    
    /**
     * Get all dropped item entities in the world.
     * 
     * @param world The world to search in
     * @return List of ItemEntity objects representing all dropped items
     */
    public static List<ItemEntity> getItemEntities(com.hypixel.hytale.server.core.universe.world.World world) {
        List<ItemEntity> items = new ArrayList<>();
        if (world == null) {
            return items;
        }
        
        com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
        com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = entityStore.getStore();
        
        // Use forEachChunk to iterate through entities with ItemComponent
        java.util.function.BiConsumer<
            com.hypixel.hytale.component.ArchetypeChunk<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>,
            com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>
        > consumer = (archetypeChunk, commandBuffer) -> {
            for (int i = 0; i < archetypeChunk.size(); i++) {
                try {
                    // Get the ItemComponent
                    com.hypixel.hytale.server.core.modules.entity.item.ItemComponent itemComp = 
                        archetypeChunk.getComponent(i, com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.getComponentType());
                    
                    if (itemComp != null) {
                        // Get the transform for position
                        TransformComponent transform = 
                            archetypeChunk.getComponent(i, TransformComponent.getComponentType());
                        
                        if (transform != null) {
                            // Get item info
                            ItemStack itemStack = itemComp.getItemStack();
                            String itemId = itemStack != null ? itemStack.getItemId() : "unknown";
                            int quantity = itemStack != null ? itemStack.getQuantity() : 0;
                            com.hypixel.hytale.math.vector.Vector3d position = transform.getPosition();
                            
                            // Get the Ref
                            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref = 
                                archetypeChunk.getReferenceTo(i);
                            
                            items.add(new ItemEntity(ref, itemId, quantity, position));
                        }
                    }
                } catch (Exception e) {
                    // Skip any errors
                }
            }
        };
        
        store.forEachChunk(
            (com.hypixel.hytale.component.query.Query<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>) 
                com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.getComponentType(), 
            consumer
        );
        
        return items;
    }
    
    /**
     * Get all dropped item entities of a specific item type.
     * 
     * @param world The world to search in
     * @param itemId The item ID to filter by (e.g., "Food_Pork_Raw")
     * @return List of ItemEntity objects matching the item ID
     */
    public static List<ItemEntity> getItemEntitiesByItemId(com.hypixel.hytale.server.core.universe.world.World world, String itemId) {
        List<ItemEntity> allItems = getItemEntities(world);
        List<ItemEntity> filtered = new ArrayList<>();
        
        for (ItemEntity item : allItems) {
            if (itemId.equals(item.getItemId())) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }
    
    /**
     * Get all dropped item entities within a radius of a position.
     * 
     * @param world The world to search in
     * @param center Center position to search from
     * @param radius Search radius in blocks
     * @return List of ItemEntity objects within the radius
     */
    public static List<ItemEntity> getItemEntitiesInRadius(
        com.hypixel.hytale.server.core.universe.world.World world,
        com.hypixel.hytale.math.vector.Vector3d center,
        double radius
    ) {
        List<ItemEntity> allItems = getItemEntities(world);
        List<ItemEntity> filtered = new ArrayList<>();
        
        for (ItemEntity item : allItems) {
            com.hypixel.hytale.math.vector.Vector3d itemPos = item.getPosition();
            double dx = center.getX() - itemPos.getX();
            double dy = center.getY() - itemPos.getY();
            double dz = center.getZ() - itemPos.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            if (distance <= radius) {
                filtered.add(item);
            }
        }
        
        return filtered;
    }
    
    /**
     * Remove a specific item entity from the world.
     * 
     * @param world The world containing the item entity
     * @param itemEntity The ItemEntity to remove
     * @return true if successfully removed
     */
    public static boolean removeItemEntity(com.hypixel.hytale.server.core.universe.world.World world, ItemEntity itemEntity) {
        if (world == null || itemEntity == null || itemEntity.getRef() == null) {
            return false;
        }
        
        try {
            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = entityStore.getStore();
            
            // Create a command buffer and remove the entity
            final boolean[] removed = {false};
            store.forEachChunk(
                (com.hypixel.hytale.component.query.Query<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>) 
                    com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.getComponentType(),
                (archetypeChunk, commandBuffer) -> {
                    if (itemEntity.getRef().isValid()) {
                        commandBuffer.removeEntity(itemEntity.getRef(), com.hypixel.hytale.component.RemoveReason.REMOVE);
                        removed[0] = true;
                    }
                }
            );
            
            return removed[0];
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error removing item entity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove all dropped item entities from the world.
     * 
     * @param world The world to clear items from
     * @return Number of items removed
     */
    public static int removeAllItemEntities(com.hypixel.hytale.server.core.universe.world.World world) {
        List<ItemEntity> items = getItemEntities(world);
        int removed = 0;
        
        for (ItemEntity item : items) {
            if (removeItemEntity(world, item)) {
                removed++;
            }
        }
        
        return removed;
    }
    
    /**
     * Remove all dropped item entities of a specific type from the world.
     * 
     * @param world The world to clear items from
     * @param itemId The item ID to remove (e.g., "Food_Pork_Raw")
     * @return Number of items removed
     */
    public static int removeItemEntitiesByItemId(com.hypixel.hytale.server.core.universe.world.World world, String itemId) {
        List<ItemEntity> items = getItemEntitiesByItemId(world, itemId);
        int removed = 0;
        
        for (ItemEntity item : items) {
            if (removeItemEntity(world, item)) {
                removed++;
            }
        }
        
        return removed;
    }
    
    /**
     * Count the total number of dropped item entities in the world.
     * 
     * @param world The world to count items in
     * @return Number of dropped items
     */
    public static int countItemEntities(com.hypixel.hytale.server.core.universe.world.World world) {
        return getItemEntities(world).size();
    }
    
    /**
     * Count dropped item entities of a specific type.
     * 
     * @param world The world to count items in
     * @param itemId The item ID to count
     * @return Number of matching dropped items
     */
    public static int countItemEntitiesByItemId(com.hypixel.hytale.server.core.universe.world.World world, String itemId) {
        return getItemEntitiesByItemId(world, itemId).size();
    }
    
    /**
     * Teleport a specific item entity to a new position.
     * 
     * @param world The world containing the item entity
     * @param itemEntity The ItemEntity to teleport
     * @param position The new position
     * @return true if successfully teleported
     */
    public static boolean teleportItemEntity(
        com.hypixel.hytale.server.core.universe.world.World world,
        ItemEntity itemEntity,
        Vector3d position
    ) {
        if (world == null || itemEntity == null || itemEntity.getRef() == null || position == null) {
            return false;
        }
        
        try {
            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = entityStore.getStore();
            
            // Update the transform component
            final boolean[] teleported = {false};
            store.forEachChunk(
                (com.hypixel.hytale.component.query.Query<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>) 
                    com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.getComponentType(),
                (archetypeChunk, commandBuffer) -> {
                    if (itemEntity.getRef().isValid()) {
                        TransformComponent transform = commandBuffer.getComponent(
                            itemEntity.getRef(),
                            TransformComponent.getComponentType()
                        );
                        if (transform != null) {
                            transform.setPosition(position);
                            teleported[0] = true;
                        }
                    }
                }
            );
            
            return teleported[0];
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error teleporting item entity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Teleport a specific item entity to coordinates.
     * 
     * @param world The world containing the item entity
     * @param itemEntity The ItemEntity to teleport
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if successfully teleported
     */
    public static boolean teleportItemEntity(
        com.hypixel.hytale.server.core.universe.world.World world,
        ItemEntity itemEntity,
        double x, double y, double z
    ) {
        return teleportItemEntity(world, itemEntity, new Vector3d(x, y, z));
    }
    
    /**
     * Teleport all dropped items of a specific type to a position.
     * 
     * @param world The world to search in
     * @param itemId The item ID to teleport (e.g., "Food_Pork_Raw")
     * @param position The destination position
     * @return Number of items teleported
     */
    public static int teleportItemEntitiesByItemId(
        com.hypixel.hytale.server.core.universe.world.World world,
        String itemId,
        Vector3d position
    ) {
        List<ItemEntity> items = getItemEntitiesByItemId(world, itemId);
        int teleported = 0;
        
        for (ItemEntity item : items) {
            if (teleportItemEntity(world, item, position)) {
                teleported++;
            }
        }
        
        return teleported;
    }
    
    /**
     * Teleport all dropped items within a radius to a position.
     * 
     * @param world The world to search in
     * @param center Center position to search from
     * @param radius Search radius in blocks
     * @param destination The destination position
     * @return Number of items teleported
     */
    public static int teleportItemEntitiesInRadius(
        com.hypixel.hytale.server.core.universe.world.World world,
        Vector3d center,
        double radius,
        Vector3d destination
    ) {
        List<ItemEntity> items = getItemEntitiesInRadius(world, center, radius);
        int teleported = 0;
        
        for (ItemEntity item : items) {
            if (teleportItemEntity(world, item, destination)) {
                teleported++;
            }
        }
        
        return teleported;
    }
    
    /**
     * Teleport all dropped items in the world to a position.
     * 
     * @param world The world to search in
     * @param position The destination position
     * @return Number of items teleported
     */
    public static int teleportAllItemEntities(
        com.hypixel.hytale.server.core.universe.world.World world,
        Vector3d position
    ) {
        List<ItemEntity> items = getItemEntities(world);
        int teleported = 0;
        
        for (ItemEntity item : items) {
            if (teleportItemEntity(world, item, position)) {
                teleported++;
            }
        }
        
        return teleported;
    }
    
    // Item Interaction Methods
    
    /**
     * Functional interface for item interaction handlers.
     * Provides the player, item stack, and target information for custom logic.
     */
    @FunctionalInterface
    public interface ItemInteractionHandler {
        /**
         * Called when the item interaction occurs.
         * 
         * @param player The player who triggered the interaction
         * @param item The item stack being used
         * @param targetBlock The block being targeted (null if none)
         * @param targetEntity The entity being targeted (null if none)
         * @return true to cancel the default interaction, false to allow it
         */
        boolean handle(com.hypixel.hytale.server.core.entity.entities.Player player, 
                       ItemStack item, 
                       com.hypixel.hytale.math.vector.Vector3i targetBlock, 
                       com.hypixel.hytale.server.core.entity.Entity targetEntity);
    }
    
    // Internal event listener and handler storage
    private static final java.util.Map<String, ItemInteractionHandler> rightClickHandlers = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, ItemInteractionHandler> leftClickHandlers = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean eventListenerRegistered = false;
    
    /**
     * Register a custom right-click handler for a specific item.
     * 
     * @param itemId The item ID to handle (e.g., "hytale:water_bucket")
     * @param handler The handler function to execute
     */
    public static void onItemRightClick(String itemId, ItemInteractionHandler handler) {
        rightClickHandlers.put(itemId, handler);
        ensureEventListenerRegistered();
    }
    
    /**
     * Register a custom left-click handler for a specific item.
     * 
     * @param itemId The item ID to handle (e.g., "hytale:diamond_sword")
     * @param handler The handler function to execute
     */
    public static void onItemLeftClick(String itemId, ItemInteractionHandler handler) {
        leftClickHandlers.put(itemId, handler);
        ensureEventListenerRegistered();
    }
    
    /**
     * Unregister a right-click handler for an item.
     * 
     * @param itemId The item ID to unregister
     */
    public static void unregisterRightClick(String itemId) {
        rightClickHandlers.remove(itemId);
    }
    
    /**
     * Unregister a left-click handler for an item.
     * 
     * @param itemId The item ID to unregister
     */
    public static void unregisterLeftClick(String itemId) {
        leftClickHandlers.remove(itemId);
    }

    /**
     * Backwards-compatible helper: if the dispatcher is not registered yet, remind the caller.
     * Call {@link #register(com.hypixel.hytale.server.core.plugin.JavaPlugin)} from your plugin.
     */
    private static void ensureEventListenerRegistered() {
        if (eventListenerRegistered) {
            return;
        }

        LOGGER.at(Level.WARNING).log("ItemHelper is not registered yet. Call ItemHelper.register(plugin) during plugin setup.");
    }

    /**
     * Register ItemHelper's interaction dispatcher with the plugin event registry.
     * This must be called once from your plugin (e.g. in setup()).
     */
    public static void register(com.hypixel.hytale.server.core.plugin.JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }

        if (eventListenerRegistered) {
            return;
        }

        try {
            // Register UseBlockEvent.Pre for right-clicks on blocks (ECS event)
            plugin.getEventRegistry().registerGlobal(
                com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Pre.class,
                ItemHelper::onUseBlock
            );
            
            // Register BreakBlockEvent for left-clicks on blocks (ECS event)
            plugin.getEventRegistry().registerGlobal(
                com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent.class,
                ItemHelper::onBreakBlock
            );
            
            eventListenerRegistered = true;
            LOGGER.at(Level.INFO).log(
                "ItemHelper registered UseBlockEvent.Pre and BreakBlockEvent dispatchers via plugin "
                    + plugin.getClass().getName()
            );
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to register ItemHelper interaction dispatchers: " + e.getMessage());
        }
    }

    /**
     * Get the block the player is looking at (simplified version).
     * Returns null for now - raycasting implementation needs correct API methods.
     * 
     * @param player The player to check
     * @param maxDistance Maximum raycast distance
     * @return BlockPosition of the targeted block, or null if none found
     */
    @Nullable
    public static com.hypixel.hytale.protocol.BlockPosition getTargetBlock(
        com.hypixel.hytale.server.core.entity.entities.Player player, 
        double maxDistance
    ) {
        // TODO: Implement raycasting with correct Hytale API
        // For now, return null - item interactions will work but without target detection
        return null;
    }
    
    /**
     * Get the entity the player is looking at (simplified version).
     * Returns null for now - raycasting implementation needs correct API methods.
     * 
     * @param player The player to check
     * @param maxDistance Maximum raycast distance
     * @return Entity being targeted, or null if none found
     */
    @Nullable
    public static com.hypixel.hytale.server.core.entity.Entity getTargetEntity(
        com.hypixel.hytale.server.core.entity.entities.Player player, 
        double maxDistance
    ) {
        // TODO: Implement raycasting with correct Hytale API
        // For now, return null - item interactions will work but without target detection
        return null;
    }
    
    /**
     * IMPORTANT LIMITATION:
     * The Hytale plugin API does not expose PlayerMouseButtonEvent for air-clicks.
     * This means we can only detect:
     * - Right-clicks ON BLOCKS (via BlockInteractEvent)
     * - Right-clicks ON ENTITIES (via EntityInteractEvent)
     * 
     * Right-clicks in AIR (no target) are NOT detectable through the plugin API.
     * 
     * Workarounds for air-click abilities:
     * 1. Use item drop (Q key) - PlayerDropItemEvent
     * 2. Use hand swap (F key) - PlayerSwapHandItemEvent
     * 3. Create custom items with interaction configs in JSON
     */
    
    private static void onUseBlock(
        com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Pre event
    ) {
        if (event == null || event.isCancelled()) {
            return;
        }

        try {
            LOGGER.at(Level.INFO).log("[ItemHelper] UseBlockEvent.Pre fired (right-click on block)");

            // Get the interaction context
            com.hypixel.hytale.server.core.entity.InteractionContext context = event.getContext();
            if (context == null) {
                return;
            }

            // Get the item from context
            ItemStack heldItem = context.getHeldItem();
            if (heldItem == null || ItemStack.isEmpty(heldItem)) {
                LOGGER.at(Level.INFO).log("[ItemHelper] No item in hand");
                return;
            }

            String itemId = heldItem.getItemId();
            if (itemId == null) {
                return;
            }

            LOGGER.at(Level.INFO).log("[ItemHelper] Item in hand: " + itemId);

            // Get player from entity reference
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> entityRef = context.getEntity();
            if (entityRef == null || !entityRef.isValid()) {
                LOGGER.at(Level.INFO).log("[ItemHelper] No valid entity reference");
                return;
            }

            // Get player component from entity
            com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> commandBuffer = context.getCommandBuffer();
            if (commandBuffer == null) {
                LOGGER.at(Level.INFO).log("[ItemHelper] No command buffer");
                return;
            }

            com.hypixel.hytale.server.core.entity.entities.Player player = commandBuffer.getComponent(
                entityRef, 
                com.hypixel.hytale.server.core.entity.entities.Player.getComponentType()
            );
            
            if (player == null) {
                LOGGER.at(Level.INFO).log("[ItemHelper] Entity is not a player");
                return;
            }

            com.hypixel.hytale.math.vector.Vector3i targetBlock = event.getTargetBlock();

            ItemInteractionHandler handler = rightClickHandlers.get(itemId);

            if (handler == null) {
                LOGGER.at(Level.INFO).log(
                    "[ItemHelper] No right-click handler registered for " + itemId
                );
                return;
            }

            LOGGER.at(Level.INFO).log(
                "[ItemHelper] Dispatching right-click handler for " + itemId
            );
            boolean cancel = handler.handle(player, heldItem, targetBlock, null);
            if (cancel) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error dispatching use block event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void onBreakBlock(
        com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent event
    ) {
        if (event == null || event.isCancelled()) {
            return;
        }

        try {
            LOGGER.at(Level.INFO).log("[ItemHelper] BreakBlockEvent fired (left-click on block)");

            // Get the item from the event
            ItemStack heldItem = event.getItemInHand();
            if (heldItem == null || ItemStack.isEmpty(heldItem)) {
                LOGGER.at(Level.INFO).log("[ItemHelper] No item in hand");
                return;
            }

            String itemId = heldItem.getItemId();
            if (itemId == null) {
                return;
            }

            LOGGER.at(Level.INFO).log("[ItemHelper] Item in hand: " + itemId);

            // Note: BreakBlockEvent is an ECS event, so we need to get the player from the ECS context
            // For now, we'll skip left-click handlers since we can't easily get the player reference
            // This is a limitation of the ECS event system
            
            com.hypixel.hytale.math.vector.Vector3i targetBlock = event.getTargetBlock();

            ItemInteractionHandler handler = leftClickHandlers.get(itemId);

            if (handler == null) {
                LOGGER.at(Level.INFO).log(
                    "[ItemHelper] No left-click handler registered for " + itemId
                );
                return;
            }

            LOGGER.at(Level.INFO).log(
                "[ItemHelper] Dispatching left-click handler for " + itemId
            );
            
            // TODO: Get player from ECS context - for now we can't call the handler without player
            LOGGER.at(Level.WARNING).log(
                "[ItemHelper] Cannot dispatch left-click handler - BreakBlockEvent doesn't provide player reference"
            );
            
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error dispatching break block event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
