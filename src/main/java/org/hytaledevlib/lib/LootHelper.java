package org.hytaledevlib.lib;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * LootHelper - Utilities for customizing block drops and loot tables.
 * 
 * Allows you to override default block drops with custom items and quantities.
 * Uses ECS systems to intercept block break events and spawn custom loot.
 */
public class LootHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<String, Map<String, CustomLootTable>> worldLootTables = new ConcurrentHashMap<>();
    
    /**
     * Register a custom loot table for a specific block type.
     * When the block is broken, the custom drops will be spawned instead of (or in addition to) default drops.
     * 
     * @param world The world to register the loot table in
     * @param blockTypeId The block type ID (e.g., "Rock_Stone", "Ore_Coal")
     * @param lootProvider Function that receives block position and returns list of ItemDrops
     */
    public static void registerBlockLoot(World world, String blockTypeId, BiFunction<Vector3i, String, List<ItemDrop>> lootProvider) {
        String worldName = world.getName();
        worldLootTables.putIfAbsent(worldName, new ConcurrentHashMap<>());
        
        CustomLootTable lootTable = new CustomLootTable(lootProvider, false);
        worldLootTables.get(worldName).put(blockTypeId, lootTable);
        
        // Register the ECS system if this is the first loot table for this world
        if (worldLootTables.get(worldName).size() == 1) {
            registerLootSystem(world);
        }
        
        LOGGER.atInfo().log("Registered custom loot for block: " + blockTypeId);
    }
    
    /**
     * Register a custom loot table that REPLACES the default drops entirely.
     * The block will be removed and only your custom items will drop.
     * 
     * @param world The world to register the loot table in
     * @param blockTypeId The block type ID (e.g., "Rock_Stone", "Ore_Coal")
     * @param lootProvider Function that receives block position and returns list of ItemDrops
     */
    public static void registerBlockLootReplacement(World world, String blockTypeId, BiFunction<Vector3i, String, List<ItemDrop>> lootProvider) {
        String worldName = world.getName();
        worldLootTables.putIfAbsent(worldName, new ConcurrentHashMap<>());
        
        CustomLootTable lootTable = new CustomLootTable(lootProvider, true);
        worldLootTables.get(worldName).put(blockTypeId, lootTable);
        
        // Register the ECS system if this is the first loot table for this world
        if (worldLootTables.get(worldName).size() == 1) {
            registerLootSystem(world);
        }
        
        LOGGER.atInfo().log("Registered replacement loot for block: " + blockTypeId);
    }
    
    /**
     * Clear all custom loot tables for a world.
     * 
     * @param world The world to clear loot tables from
     */
    public static void clearLootTables(World world) {
        worldLootTables.remove(world.getName());
        LOGGER.atInfo().log("Cleared all loot tables for world: " + world.getName());
    }
    
    /**
     * Register the ECS system that handles custom loot drops.
     */
    private static void registerLootSystem(World world) {
        try {
            EntityEventSystem<EntityStore, BreakBlockEvent> system = new EntityEventSystem<EntityStore, BreakBlockEvent>(BreakBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final BreakBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        
                        // Skip Empty blocks
                        if ("Empty".equals(blockTypeId)) {
                            return;
                        }
                        
                        // Get the player who broke the block
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        com.hypixel.hytale.server.core.entity.Entity player = store.getComponent(
                            ref, 
                            com.hypixel.hytale.server.core.entity.entities.Player.getComponentType()
                        );
                        
                        // Check if we have custom loot for this block
                        Map<String, CustomLootTable> lootTables = worldLootTables.get(world.getName());
                        if (lootTables != null && lootTables.containsKey(blockTypeId)) {
                            CustomLootTable lootTable = lootTables.get(blockTypeId);
                            
                            // Get custom drops
                            List<ItemDrop> drops = lootTable.lootProvider.apply(position, blockTypeId);
                            
                            // If this is a replacement, remove the block first to prevent default drops
                            if (lootTable.replaceDefault) {
                                BlockHelper.setBlock(world, position.getX(), position.getY(), position.getZ(), 0); // Set to air/empty
                            }
                            
                            // Spawn custom items
                            if (drops != null && !drops.isEmpty()) {
                                for (ItemDrop drop : drops) {
                                    spawnItemDrop(world, position, drop, player, store, commandBuffer);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in LootHelper system: " + e.getMessage());
                    }
                }
                
                @Nullable
                @Override
                public Query<EntityStore> getQuery() {
                    return PlayerRef.getComponentType();
                }
                
                @Nonnull
                @Override
                public Set<Dependency<EntityStore>> getDependencies() {
                    return Collections.singleton(RootDependency.first());
                }
            };
            
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered LootHelper ECS system for world: " + world.getName());
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register LootHelper system: " + e.getMessage());
        }
    }
    
    /**
     * Spawn an item drop at a block position as a physical item entity in the world.
     * Uses ItemComponent.generateItemDrop to create proper item entities with physics.
     * Uses CommandBuffer to properly add entities from within an ECS system.
     */
    private static void spawnItemDrop(
            World world, 
            Vector3i blockPos, 
            ItemDrop drop, 
            com.hypixel.hytale.server.core.entity.Entity player,
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
            com.hypixel.hytale.component.CommandBuffer<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> commandBuffer) {
        try {
            // Convert block position to world position (center of block + slight offset up)
            Vector3d spawnPos = new Vector3d(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5
            );
            
            // Create item stack
            com.hypixel.hytale.server.core.inventory.ItemStack itemStack = 
                ItemHelper.createStack(drop.itemId, drop.quantity);
            if (itemStack == null) {
                LOGGER.atWarning().log("Failed to create item stack for: " + drop.itemId);
                return;
            }
            
            // Generate item entity using ItemComponent.generateItemDrop
            com.hypixel.hytale.component.Holder<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> itemEntityHolder = 
                com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.generateItemDrop(
                    store,
                    itemStack,
                    spawnPos,
                    com.hypixel.hytale.math.vector.Vector3f.ZERO,
                    (float) drop.velocity.getX(),
                    (float) drop.velocity.getY(),
                    (float) drop.velocity.getZ()
                );
            
            if (itemEntityHolder != null) {
                // Use CommandBuffer to add entity (required when called from ECS system)
                commandBuffer.addEntity(itemEntityHolder, com.hypixel.hytale.component.AddReason.SPAWN);
            } else {
                LOGGER.atWarning().log("Failed to generate item entity for: " + drop.itemId);
            }
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to spawn item drop: " + e.getMessage());
        }
    }
    
    /**
     * Internal class to store custom loot table information.
     */
    private static class CustomLootTable {
        final BiFunction<Vector3i, String, List<ItemDrop>> lootProvider;
        final boolean replaceDefault;
        
        CustomLootTable(BiFunction<Vector3i, String, List<ItemDrop>> lootProvider, boolean replaceDefault) {
            this.lootProvider = lootProvider;
            this.replaceDefault = replaceDefault;
        }
    }
    
    /**
     * Represents an item drop with quantity and optional velocity.
     */
    public static class ItemDrop {
        public final String itemId;
        public final int quantity;
        public final Vector3d velocity;
        
        /**
         * Create an item drop with default velocity (stationary).
         */
        public ItemDrop(String itemId, int quantity) {
            this(itemId, quantity, new Vector3d(0, 0.1, 0));
        }
        
        /**
         * Create an item drop with custom velocity.
         */
        public ItemDrop(String itemId, int quantity, Vector3d velocity) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.velocity = velocity;
        }
        
        /**
         * Create an item drop with random velocity spread.
         */
        public static ItemDrop withRandomVelocity(String itemId, int quantity) {
            Random random = new Random();
            Vector3d velocity = new Vector3d(
                (random.nextDouble() - 0.5) * 0.2,
                random.nextDouble() * 0.3 + 0.1,
                (random.nextDouble() - 0.5) * 0.2
            );
            return new ItemDrop(itemId, quantity, velocity);
        }
    }
}
