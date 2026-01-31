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
import java.util.function.Function;

/**
 * LootHelper - Utilities for customizing block drops and mob loot tables.
 * 
 * Allows you to override default block drops with custom items and quantities.
 * Also allows you to add custom drops to mobs when they die.
 * Uses ECS systems to intercept block break events and entity death events to spawn custom loot.
 */
public class LootHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Map<String, Map<String, CustomLootTable>> worldLootTables = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, MobLootTable>> worldMobLootTables = new ConcurrentHashMap<>();
    private static final Set<String> mobDeathSystemRegistered = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
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
     * Internal class to store custom mob loot table information.
     */
    private static class MobLootTable {
        final Function<Vector3d, List<ItemDrop>> lootProvider;
        final boolean replaceDefault;
        
        MobLootTable(Function<Vector3d, List<ItemDrop>> lootProvider, boolean replaceDefault) {
            this.lootProvider = lootProvider;
            this.replaceDefault = replaceDefault;
        }
    }
    
    /**
     * Represents an item drop with quantity, optional velocity, and drop chance.
     */
    public static class ItemDrop {
        public final String itemId;
        public final int quantity;
        public final Vector3d velocity;
        public final float dropChance; // 0.0 to 1.0 (0% to 100%)
        
        /**
         * Create an item drop with default velocity (stationary) and 100% drop chance.
         */
        public ItemDrop(String itemId, int quantity) {
            this(itemId, quantity, new Vector3d(0, 0.1, 0), 1.0f);
        }
        
        /**
         * Create an item drop with custom velocity and 100% drop chance.
         */
        public ItemDrop(String itemId, int quantity, Vector3d velocity) {
            this(itemId, quantity, velocity, 1.0f);
        }
        
        /**
         * Create an item drop with custom velocity and drop chance.
         * 
         * @param itemId The item ID to drop
         * @param quantity How many items to drop
         * @param velocity The velocity to apply to the item
         * @param dropChance Drop chance from 0.0 to 1.0 (0% to 100%)
         */
        public ItemDrop(String itemId, int quantity, Vector3d velocity, float dropChance) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.velocity = velocity;
            this.dropChance = Math.max(0.0f, Math.min(1.0f, dropChance)); // Clamp between 0 and 1
        }
        
        /**
         * Create an item drop with a specific drop chance (0.0 to 1.0).
         * 
         * @param itemId The item ID to drop
         * @param quantity How many items to drop
         * @param dropChance Drop chance from 0.0 to 1.0 (0% to 100%)
         * @return ItemDrop with specified chance
         */
        public static ItemDrop withChance(String itemId, int quantity, float dropChance) {
            return new ItemDrop(itemId, quantity, new Vector3d(0, 0.1, 0), dropChance);
        }
        
        /**
         * Create an item drop with random velocity and a specific drop chance.
         * 
         * @param itemId The item ID to drop
         * @param quantity How many items to drop
         * @param dropChance Drop chance from 0.0 to 1.0 (0% to 100%)
         * @return ItemDrop with random velocity and specified chance
         */
        public static ItemDrop withRandomVelocityAndChance(String itemId, int quantity, float dropChance) {
            Random random = new Random();
            double vx = (random.nextDouble() - 0.5) * 0.3;
            double vy = 0.2 + random.nextDouble() * 0.1;
            double vz = (random.nextDouble() - 0.5) * 0.3;
            return new ItemDrop(itemId, quantity, new Vector3d(vx, vy, vz), dropChance);
        }
        
        /**
         * Create an item drop with random velocity spread and 100% drop chance.
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
    
    // ============================================
    // MOB DROP HELPER METHODS
    // ============================================
    
    /**
     * Register custom loot drops for a specific mob/entity type.
     * When the mob dies, the custom drops will be spawned in addition to default drops.
     * 
     * NOTE: You must call handleMobDeath() from your death handler for this to work!
     * 
     * @param world The world to register the mob loot in
     * @param entityTypeId The entity type ID (e.g., "Kweebec", "Trork", or component type name)
     * @param lootProvider Function that receives entity position and returns list of ItemDrops
     */
    public static void registerMobLoot(World world, String entityTypeId, Function<Vector3d, List<ItemDrop>> lootProvider) {
        String worldName = world.getName();
        worldMobLootTables.putIfAbsent(worldName, new ConcurrentHashMap<>());
        
        MobLootTable lootTable = new MobLootTable(lootProvider, false);
        worldMobLootTables.get(worldName).put(entityTypeId, lootTable);
        
        LOGGER.atInfo().log("Registered custom mob loot for entity: " + entityTypeId);
    }
    
    /**
     * Register custom loot drops that REPLACE the default mob drops entirely.
     * When the mob dies, only your custom items will drop.
     * 
     * @param world The world to register the mob loot in
     * @param entityTypeId The entity type ID (e.g., "Kweebec", "Trork")
     * @param lootProvider Function that receives entity position and returns list of ItemDrops
     */
    public static void registerMobLootReplacement(World world, String entityTypeId, Function<Vector3d, List<ItemDrop>> lootProvider) {
        String worldName = world.getName();
        worldMobLootTables.putIfAbsent(worldName, new ConcurrentHashMap<>());
        
        MobLootTable lootTable = new MobLootTable(lootProvider, true);
        worldMobLootTables.get(worldName).put(entityTypeId, lootTable);
        
        LOGGER.atInfo().log("Registered replacement mob loot for entity: " + entityTypeId);
    }
    
    /**
     * Spawn an item drop at a specific location in the world.
     * Useful for custom drop logic or spawning items programmatically.
     * 
     * @param world The world to spawn the item in
     * @param position The position to spawn the item at
     * @param itemId The item ID to spawn
     * @param quantity How many items to spawn
     * @param velocity The velocity to apply to the item (use Vector3d.ZERO for stationary)
     */
    public static void spawnItemAtLocation(World world, Vector3d position, String itemId, int quantity, Vector3d velocity) {
        try {
            com.hypixel.hytale.server.core.inventory.ItemStack itemStack = ItemHelper.createStack(itemId, quantity);
            if (itemStack == null) {
                LOGGER.atWarning().log("Failed to create item stack for: " + itemId);
                return;
            }
            
            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = 
                entityStore.getStore();
            
            com.hypixel.hytale.component.Holder<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> itemEntityHolder = 
                com.hypixel.hytale.server.core.modules.entity.item.ItemComponent.generateItemDrop(
                    store,
                    itemStack,
                    position,
                    com.hypixel.hytale.math.vector.Vector3f.ZERO,
                    (float) velocity.getX(),
                    (float) velocity.getY(),
                    (float) velocity.getZ()
                );
            
            if (itemEntityHolder != null) {
                store.addEntity(itemEntityHolder, com.hypixel.hytale.component.AddReason.SPAWN);
            } else {
                LOGGER.atWarning().log("Failed to generate item entity for: " + itemId);
            }
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to spawn item at location: " + e.getMessage());
        }
    }
    
    /**
     * Clear all custom mob loot tables for a world.
     * 
     * @param world The world to clear mob loot tables from
     */
    public static void clearMobLootTables(World world) {
        worldMobLootTables.remove(world.getName());
        LOGGER.atInfo().log("Cleared all mob loot tables for world: " + world.getName());
    }
    
    /**
     * Handle mob death and spawn custom loot if registered.
     * Call this method from your DeathHelper.onEntityDeath() callback.
     * 
     * @param death The EntityDeath object from DeathHelper
     * @param world The world where the death occurred
     */
    public static void handleMobDeath(DeathHelper.EntityDeath death, World world) {
        try {
            Vector3d position = death.getPosition();
            if (position == null) {
                LOGGER.atInfo().log("[MobLoot] Death position is null, skipping");
                return;
            }
            
            // Try to identify the entity type
            String entityTypeId = identifyEntityType(death.getEntityRef(), death.getStore());
            
            LOGGER.atInfo().log("[MobLoot] Entity died: " + (entityTypeId != null ? entityTypeId : "UNKNOWN"));
            
            if (entityTypeId == null) {
                LOGGER.atInfo().log("[MobLoot] Could not identify entity type, skipping");
                return;
            }
            
            // Check if we have custom loot for this entity type
            Map<String, MobLootTable> mobLootTables = worldMobLootTables.get(world.getName());
            LOGGER.atInfo().log("[MobLoot] Checking loot tables for world: " + world.getName());
            LOGGER.atInfo().log("[MobLoot] Loot tables exist: " + (mobLootTables != null));
            
            if (mobLootTables != null) {
                LOGGER.atInfo().log("[MobLoot] Registered mob types: " + mobLootTables.keySet());
                LOGGER.atInfo().log("[MobLoot] Has loot for " + entityTypeId + ": " + mobLootTables.containsKey(entityTypeId));
            }
            
            if (mobLootTables != null && mobLootTables.containsKey(entityTypeId)) {
                MobLootTable lootTable = mobLootTables.get(entityTypeId);
                
                LOGGER.atInfo().log("[MobLoot] Found custom loot for: " + entityTypeId);
                
                // Get custom drops
                List<ItemDrop> drops = lootTable.lootProvider.apply(position);
                
                LOGGER.atInfo().log("[MobLoot] Generated " + (drops != null ? drops.size() : 0) + " drops");
                
                // Spawn custom items (deferred to avoid store processing error)
                if (drops != null && !drops.isEmpty()) {
                    Random random = new Random();
                    for (ItemDrop drop : drops) {
                        // Check drop chance
                        float roll = random.nextFloat();
                        if (roll <= drop.dropChance) {
                            LOGGER.atInfo().log("[MobLoot] Spawning: " + drop.quantity + "x " + drop.itemId + " (chance: " + (drop.dropChance * 100) + "%, rolled: " + (roll * 100) + "%)");
                            // Defer spawn to next tick to avoid "Store is currently processing" error
                            WorldHelper.waitTicks(world, 1, () -> {
                                spawnItemAtLocation(world, position, drop.itemId, drop.quantity, drop.velocity);
                            });
                        } else {
                            LOGGER.atInfo().log("[MobLoot] Skipped: " + drop.quantity + "x " + drop.itemId + " (chance: " + (drop.dropChance * 100) + "%, rolled: " + (roll * 100) + "%)");
                        }
                    }
                }
                
                // Note about replacement mode
                if (lootTable.replaceDefault) {
                    LOGGER.atInfo().log("Note: Mob loot replacement mode adds custom drops but doesn't remove default drops yet");
                }
            } else {
                LOGGER.atInfo().log("[MobLoot] No custom loot registered for: " + entityTypeId);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error in mob death loot handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try to identify the entity type from a reference.
     * Returns a string identifier that can be used to match against registered mob loot tables.
     */
    private static String identifyEntityType(
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store) {
        
        // Try to get NPC entity component
        com.hypixel.hytale.server.npc.entities.NPCEntity npcEntity = 
            store.getComponent(ref, com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
        
        if (npcEntity != null && npcEntity.getRoleName() != null) {
            return npcEntity.getRoleName();
        }
        
        // Try to get Player component
        com.hypixel.hytale.server.core.entity.entities.Player playerComponent = 
            store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        
        if (playerComponent != null) {
            return "Player";
        }
        
        // Could add more entity type checks here (animals, etc.)
        
        return null;
    }
    
}
