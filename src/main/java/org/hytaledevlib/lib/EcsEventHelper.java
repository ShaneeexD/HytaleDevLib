package org.hytaledevlib.lib;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthChunk;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.util.ChunkUtil;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * EcsEventHelper - Helper methods for ECS-based events (block breaking, placing, etc.)
 * 
 * These events require ECS systems to be registered with the world's EntityStore.
 * This helper simplifies the process by creating and registering systems automatically.
 * 
 * IMPORTANT: These methods must be called AFTER you have access to a World instance,
 * typically in the AddPlayerToWorldEvent callback.
 */
public class EcsEventHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /**
     * Functional interface for callbacks that accept three parameters.
     */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }
    
    /**
     * Register a callback for when a player breaks a block.
     * 
     * This creates and registers an ECS system to handle BreakBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback BiConsumer that receives the block position and block type ID
     */
    public static void onBlockBreak(World world, BiConsumer<Vector3i, String> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, BreakBlockEvent> system = new EntityEventSystem<EntityStore, BreakBlockEvent>(BreakBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final BreakBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        
                        // Filter out "Empty" blocks - these are triggered when placing blocks
                        // and don't represent actual block breaking
                        if (!"Empty".equals(blockTypeId)) {
                            callback.accept(position, blockTypeId);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockBreak callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockBreak ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockBreak system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player breaks a block (with player entity).
     * 
     * This creates and registers an ECS system to handle BreakBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback TriConsumer that receives the block position, block type ID, and player entity
     */
    public static void onBlockBreak(World world, TriConsumer<Vector3i, String, Entity> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, BreakBlockEvent> system = new EntityEventSystem<EntityStore, BreakBlockEvent>(BreakBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final BreakBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        
                        // Filter out "Empty" blocks
                        if (!"Empty".equals(blockTypeId)) {
                            // Get the player entity reference
                            com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                            UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                            if (uuidComp != null) {
                                Entity playerEntity = world.getEntity(uuidComp.getUuid());
                                callback.accept(position, blockTypeId, playerEntity);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockBreak callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockBreak (with entity) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockBreak system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player places a block.
     * 
     * This creates and registers an ECS system to handle PlaceBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback BiConsumer that receives the block position and item ID being placed
     */
    public static void onBlockPlace(World world, BiConsumer<Vector3i, String> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, PlaceBlockEvent> system = new EntityEventSystem<EntityStore, PlaceBlockEvent>(PlaceBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final PlaceBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        // Get the item being placed from the player's hand
                        String itemId = event.getItemInHand() != null ? event.getItemInHand().getItemId() : "Unknown";
                        callback.accept(position, itemId);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockPlace callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockPlace ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockPlace system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player places a block (with player entity).
     * 
     * This creates and registers an ECS system to handle PlaceBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback TriConsumer that receives the block position, item ID being placed, and player entity
     */
    public static void onBlockPlace(World world, TriConsumer<Vector3i, String, Entity> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, PlaceBlockEvent> system = new EntityEventSystem<EntityStore, PlaceBlockEvent>(PlaceBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final PlaceBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String itemId = event.getItemInHand() != null ? event.getItemInHand().getItemId() : "Unknown";
                        
                        // Get the player entity reference
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity playerEntity = world.getEntity(uuidComp.getUuid());
                            callback.accept(position, itemId, playerEntity);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockPlace callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockPlace (with entity) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockPlace system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player damages a block (mining progress).
     * 
     * This creates and registers an ECS system to handle DamageBlockEvent.
     * This event fires continuously while a player is mining/damaging a block.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives block position, block type ID, current damage, damage amount, and item in hand
     */
    public static void onBlockDamage(World world, BlockDamageCallback callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, DamageBlockEvent> system = new EntityEventSystem<EntityStore, DamageBlockEvent>(DamageBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final DamageBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        float currentDamage = event.getCurrentDamage();
                        float damage = event.getDamage();
                        ItemStack itemInHand = event.getItemInHand();
                        String itemId = itemInHand != null ? itemInHand.getItemId() : null;
                        
                        callback.accept(position, blockTypeId, currentDamage, damage, itemId);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockDamage callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockDamage ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockDamage system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player damages a block (with player entity).
     * 
     * This creates and registers an ECS system to handle DamageBlockEvent.
     * This event fires continuously while a player is mining/damaging a block.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives block position, block type ID, current damage, damage amount, item in hand, and player entity
     */
    public static void onBlockDamage(World world, BlockDamageWithEntityCallback callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, DamageBlockEvent> system = new EntityEventSystem<EntityStore, DamageBlockEvent>(DamageBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final DamageBlockEvent event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        float currentDamage = event.getCurrentDamage();
                        float damage = event.getDamage();
                        ItemStack itemInHand = event.getItemInHand();
                        String itemId = itemInHand != null ? itemInHand.getItemId() : null;
                        
                        // Get the player entity reference
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity playerEntity = world.getEntity(uuidComp.getUuid());
                            callback.accept(position, blockTypeId, currentDamage, damage, itemId, playerEntity);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockDamage callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockDamage (with entity) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockDamage system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player damages a block (with write access to block health).
     * 
     * This creates and registers an ECS system to handle DamageBlockEvent.
     * Provides a BlockDamageContext that allows modifying the block's health/damage.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives a BlockDamageContext with read/write access
     */
    public static void onBlockDamage(World world, java.util.function.Consumer<BlockDamageContext> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, DamageBlockEvent> system = new EntityEventSystem<EntityStore, DamageBlockEvent>(DamageBlockEvent.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final DamageBlockEvent event) {
                    try {
                        // Get the player entity reference
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        Entity playerEntity = null;
                        if (uuidComp != null) {
                            playerEntity = world.getEntity(uuidComp.getUuid());
                        }
                        
                        // Create context with write access
                        BlockDamageContext context = new BlockDamageContext(
                            world,
                            event.getTargetBlock(),
                            event.getBlockType().getId(),
                            event.getCurrentDamage(),
                            event.getDamage(),
                            event.getItemInHand() != null ? event.getItemInHand().getItemId() : null,
                            playerEntity
                        );
                        
                        callback.accept(context);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockDamage callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockDamage (with context) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockDamage system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player discovers a new zone.
     * 
     * This creates and registers an ECS system to handle DiscoverZoneEvent.Display.
     * This event fires when a player enters a new zone for the first time.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives zone discovery information
     */
    public static void onZoneDiscovery(World world, ZoneDiscoveryCallback callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> system = new EntityEventSystem<EntityStore, DiscoverZoneEvent.Display>(DiscoverZoneEvent.Display.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final DiscoverZoneEvent.Display event) {
                    try {
                        WorldMapTracker.ZoneDiscoveryInfo discoveryInfo = event.getDiscoveryInfo();
                        callback.accept(discoveryInfo);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onZoneDiscovery callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onZoneDiscovery ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onZoneDiscovery system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player discovers a new zone (with player entity).
     * 
     * This creates and registers an ECS system to handle DiscoverZoneEvent.Display.
     * This event fires when a player enters a new zone for the first time.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback BiConsumer that receives zone discovery information and player entity
     */
    public static void onZoneDiscovery(World world, BiConsumer<WorldMapTracker.ZoneDiscoveryInfo, Entity> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> system = new EntityEventSystem<EntityStore, DiscoverZoneEvent.Display>(DiscoverZoneEvent.Display.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final DiscoverZoneEvent.Display event) {
                    try {
                        WorldMapTracker.ZoneDiscoveryInfo discoveryInfo = event.getDiscoveryInfo();
                        
                        // Get the player entity reference
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity playerEntity = world.getEntity(uuidComp.getUuid());
                            callback.accept(discoveryInfo, playerEntity);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onZoneDiscovery callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onZoneDiscovery (with entity) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onZoneDiscovery system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player interacts with a block (right-click/F key).
     * 
     * This creates and registers an ECS system to handle UseBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback BiConsumer that receives the block position and block type ID
     */
    public static void onBlockInteract(World world, BiConsumer<Vector3i, String> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, UseBlockEvent.Post> system = new EntityEventSystem<EntityStore, UseBlockEvent.Post>(UseBlockEvent.Post.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final UseBlockEvent.Post event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        callback.accept(position, blockTypeId);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockInteract callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockInteract ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockInteract system: " + e.getMessage());
        }
    }
    
    /**
     * Register a callback for when a player interacts with a block (with player entity).
     * 
     * This creates and registers an ECS system to handle UseBlockEvent.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback TriConsumer that receives the block position, block type ID, and player entity
     */
    public static void onBlockInteract(World world, TriConsumer<Vector3i, String, Entity> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            
            // Create a custom ECS system for this callback
            EntityEventSystem<EntityStore, UseBlockEvent.Post> system = new EntityEventSystem<EntityStore, UseBlockEvent.Post>(UseBlockEvent.Post.class) {
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final UseBlockEvent.Post event) {
                    try {
                        Vector3i position = event.getTargetBlock();
                        String blockTypeId = event.getBlockType().getId();
                        
                        // Get the player entity reference
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity playerEntity = world.getEntity(uuidComp.getUuid());
                            callback.accept(position, blockTypeId, playerEntity);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onBlockInteract callback: " + e.getMessage());
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
            
            // Register the system with the entity store
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onBlockInteract (with entity) ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onBlockInteract system: " + e.getMessage());
        }
    }
    
    /**
     * Functional interface for block damage callbacks.
     * Provides detailed information about block mining progress.
     */
    @FunctionalInterface
    public interface BlockDamageCallback {
        /**
         * Called when a block is damaged by a player.
         * 
         * @param position The position of the block being damaged
         * @param blockTypeId The block type ID (e.g., "Rock_Stone")
         * @param currentDamage The current accumulated damage on the block
         * @param damage The amount of damage being applied this tick
         * @param itemInHand The item ID in the player's hand (null if empty)
         */
        void accept(Vector3i position, String blockTypeId, float currentDamage, float damage, @Nullable String itemInHand);
    }
    
    /**
     * Functional interface for block damage callbacks with player entity.
     * Provides detailed information about block mining progress and the player.
     */
    @FunctionalInterface
    public interface BlockDamageWithEntityCallback {
        /**
         * Called when a block is damaged by a player.
         * 
         * @param position The position of the block being damaged
         * @param blockTypeId The block type ID (e.g., "Rock_Stone")
         * @param currentDamage The current accumulated damage on the block
         * @param damage The amount of damage being applied this tick
         * @param itemInHand The item ID in the player's hand (null if empty)
         * @param playerEntity The player entity damaging the block
         */
        void accept(Vector3i position, String blockTypeId, float currentDamage, float damage, @Nullable String itemInHand, Entity playerEntity);
    }
    
    /**
     * Functional interface for zone discovery callbacks.
     * Provides information about discovered zones.
     */
    @FunctionalInterface
    public interface ZoneDiscoveryCallback {
        /**
         * Called when a player discovers a new zone.
         * 
         * @param discoveryInfo The zone discovery information containing zone name, region, display settings, etc.
         */
        void accept(WorldMapTracker.ZoneDiscoveryInfo discoveryInfo);
    }
    
    /**
     * Context object for block damage events with read/write access to block health.
     * Allows modifying the block's health, applying extra damage, or repairing blocks.
     */
    public static class BlockDamageContext {
        private final World world;
        private final Vector3i position;
        private final String blockTypeId;
        private final float currentDamage;
        private final float damage;
        private final String itemInHand;
        private final Entity playerEntity;
        private final com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType;
        
        private BlockDamageContext(World world, Vector3i position, String blockTypeId, 
                                   float currentDamage, float damage, String itemInHand, Entity playerEntity) {
            this.world = world;
            this.position = position;
            this.blockTypeId = blockTypeId;
            this.currentDamage = currentDamage;
            this.damage = damage;
            this.itemInHand = itemInHand;
            this.playerEntity = playerEntity;
            this.blockType = world.getBlockType(position);
        }
        
        public Vector3i getPosition() { return position; }
        public String getBlockTypeId() { return blockTypeId; }
        public float getCurrentDamage() { return currentDamage; }
        public float getDamage() { return damage; }
        public String getItemInHand() { return itemInHand; }
        public Entity getPlayerEntity() { return playerEntity; }
        public World getWorld() { return world; }
        
        /**
         * Get the gather type of the block (e.g., "Rocks", "Woods", "Soils", "SoftBlocks", etc.).
         * This is useful for filtering which blocks a tool should work on.
         * 
         * @return The gather type string, or null if not available
         */
        public String getGatherType() {
            try {
                if (blockType != null) {
                    // Access: blockType.gathering.breaking.gatherType
                    java.lang.reflect.Field gatheringField = blockType.getClass().getDeclaredField("gathering");
                    gatheringField.setAccessible(true);
                    Object gathering = gatheringField.get(blockType);
                    
                    if (gathering != null) {
                        java.lang.reflect.Method getBreakingMethod = gathering.getClass().getMethod("getBreaking");
                        Object breaking = getBreakingMethod.invoke(gathering);
                        
                        if (breaking != null) {
                            java.lang.reflect.Method getGatherTypeMethod = breaking.getClass().getMethod("getGatherType");
                            Object gatherType = getGatherTypeMethod.invoke(breaking);
                            return gatherType != null ? gatherType.toString() : null;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to get gather type: " + e.getMessage());
            }
            return null;
        }
        
        /**
         * Get the current health of the block (0.0 = destroyed, 1.0 = full health).
         * @return Current block health, or 1.0 if block has no damage
         */
        public float getBlockHealth() {
            try {
                BlockHealthChunk healthChunk = getBlockHealthChunk();
                if (healthChunk != null) {
                    return healthChunk.getBlockHealth(position);
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to get block health: " + e.getMessage());
            }
            return 1.0f;
        }
        
        /**
         * Set the block's health directly (0.0 = destroyed, 1.0 = full health).
         * @param health New health value (0.0 to 1.0)
         */
        public void setBlockHealth(float health) {
            try {
                BlockHealthChunk healthChunk = getBlockHealthChunk();
                if (healthChunk != null) {
                    float currentHealth = healthChunk.getBlockHealth(position);
                    float delta = health - currentHealth;
                    
                    if (delta < 0) {
                        // Damaging the block
                        TimeResource uptime = world.getEntityStore().getStore().getResource(TimeResource.getResourceType());
                        healthChunk.damageBlock(uptime.getNow(), world, position, -delta);
                    } else if (delta > 0) {
                        // Repairing the block
                        healthChunk.repairBlock(world, position, delta);
                    }
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to set block health: " + e.getMessage());
            }
        }
        
        /**
         * Apply additional damage to the block.
         * @param extraDamage Amount of damage to apply (reduces health)
         */
        public void applyDamage(float extraDamage) {
            try {
                BlockHealthChunk healthChunk = getBlockHealthChunk();
                if (healthChunk != null) {
                    TimeResource uptime = world.getEntityStore().getStore().getResource(TimeResource.getResourceType());
                    healthChunk.damageBlock(uptime.getNow(), world, position, extraDamage);
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to apply damage: " + e.getMessage());
            }
        }
        
        /**
         * Repair the block by a certain amount.
         * @param repairAmount Amount to repair (increases health)
         */
        public void repairBlock(float repairAmount) {
            try {
                BlockHealthChunk healthChunk = getBlockHealthChunk();
                if (healthChunk != null) {
                    healthChunk.repairBlock(world, position, repairAmount);
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to repair block: " + e.getMessage());
            }
        }
        
        /**
         * Multiply the mining speed by applying extra damage.
         * For example, multiplier of 2.0 makes the block break 2x faster.
         * @param multiplier Speed multiplier (2.0 = 2x faster, 0.5 = 2x slower)
         */
        public void setMiningSpeedMultiplier(float multiplier) {
            if (multiplier <= 0) return;
            
            // Calculate extra damage needed to achieve the multiplier
            // If multiplier is 2.0, we need to apply 2x the normal damage
            float extraDamage = damage * (multiplier - 1.0f);
            if (extraDamage > 0) {
                applyDamage(extraDamage);
            }
        }
        
        private BlockHealthChunk getBlockHealthChunk() {
            try {
                ChunkStore chunkStore = world.getChunkStore();
                long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
                Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
                
                if (chunkRef != null) {
                    ComponentType<ChunkStore, BlockHealthChunk> healthType = 
                        BlockHealthModule.get().getBlockHealthChunkComponentType();
                    return chunkStore.getStore().getComponent(chunkRef, healthType);
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Failed to get BlockHealthChunk: " + e.getMessage());
            }
            return null;
        }
    }
    
    // ==================== Game Mode Change Event ====================
    
    /**
     * Register a callback for when a player's game mode changes.
     * This event is cancellable - you can prevent the game mode change.
     * 
     * @param world The world to register the system in
     * @param callback BiConsumer that receives the player entity and the new GameMode
     */
    public static void onGameModeChange(World world, BiConsumer<Entity, com.hypixel.hytale.protocol.GameMode> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent> system = 
                new EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent>(
                    com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent.class) {
                
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent event) {
                    try {
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity entity = world.getEntity(uuidComp.getUuid());
                            com.hypixel.hytale.protocol.GameMode newGameMode = event.getGameMode();
                            callback.accept(entity, newGameMode);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onGameModeChange callback: " + e.getMessage());
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
            LOGGER.atInfo().log("Registered onGameModeChange ECS system");
            
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register onGameModeChange system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== Hotbar Slot Switch Event ====================
    
    /**
     * Register a callback for when a player switches their active hotbar slot.
     * This event is cancellable - you can prevent the slot switch.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives the player entity, previous slot, and new slot
     */
    public static void onHotbarSwitch(World world, TriConsumer<Entity, Integer, Integer> callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent> system = 
                new EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent>(
                    com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent.class) {
                
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent event) {
                    try {
                        com.hypixel.hytale.component.Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuidComp != null) {
                            Entity entity = world.getEntity(uuidComp.getUuid());
                            int previousSlot = event.getPreviousSlot();
                            int newSlot = event.getNewSlot();
                            callback.accept(entity, previousSlot, newSlot);
                        }
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onHotbarSwitch callback: " + e.getMessage());
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
            LOGGER.atInfo().log("Registered onHotbarSwitch ECS system");
            
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register onHotbarSwitch system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== Moon Phase Change Event ====================
    
    /**
     * Register a callback for when the moon phase changes in the world.
     * Moon phases are represented as integers (0-7 typically).
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives the new moon phase
     */
    public static void onMoonPhaseChange(World world, java.util.function.IntConsumer callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.MoonPhaseChangeEvent> system = 
                new EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.MoonPhaseChangeEvent>(
                    com.hypixel.hytale.server.core.universe.world.events.ecs.MoonPhaseChangeEvent.class) {
                
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final com.hypixel.hytale.server.core.universe.world.events.ecs.MoonPhaseChangeEvent event) {
                    try {
                        int newMoonPhase = event.getNewMoonPhase();
                        callback.accept(newMoonPhase);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onMoonPhaseChange callback: " + e.getMessage());
                    }
                }
                
                @Nullable
                @Override
                public Query<EntityStore> getQuery() {
                    return null; // World event, no specific entity query
                }
                
                @Nonnull
                @Override
                public Set<Dependency<EntityStore>> getDependencies() {
                    return Collections.singleton(RootDependency.first());
                }
            };
            
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onMoonPhaseChange ECS system");
            
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register onMoonPhaseChange system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== Chunk Save Event ====================
    
    /**
     * Register a callback for when a chunk is about to be saved.
     * This event is cancellable - you can prevent the chunk from being saved.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives the chunk position (as long)
     */
    public static void onChunkSave(World world, java.util.function.LongConsumer callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent> system = 
                new EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent>(
                    com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent.class) {
                
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkSaveEvent event) {
                    try {
                        com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = event.getChunk();
                        long chunkIndex = chunk.getIndex();
                        callback.accept(chunkIndex);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onChunkSave callback: " + e.getMessage());
                    }
                }
                
                @Nullable
                @Override
                public Query<EntityStore> getQuery() {
                    return null; // Chunk event, no specific entity query
                }
                
                @Nonnull
                @Override
                public Set<Dependency<EntityStore>> getDependencies() {
                    return Collections.singleton(RootDependency.first());
                }
            };
            
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onChunkSave ECS system");
            
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register onChunkSave system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== Chunk Unload Event ====================
    
    /**
     * Register a callback for when a chunk is about to be unloaded.
     * This event is cancellable - you can prevent the chunk from being unloaded.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives the chunk position (as long)
     */
    public static void onChunkUnload(World world, java.util.function.LongConsumer callback) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent> system = 
                new EntityEventSystem<EntityStore, com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent>(
                    com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent.class) {
                
                @Override
                public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                                   @Nonnull final Store<EntityStore> store,
                                   @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                                   @Nonnull final com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent event) {
                    try {
                        com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = event.getChunk();
                        long chunkIndex = chunk.getIndex();
                        callback.accept(chunkIndex);
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onChunkUnload callback: " + e.getMessage());
                    }
                }
                
                @Nullable
                @Override
                public Query<EntityStore> getQuery() {
                    return null; // Chunk event, no specific entity query
                }
                
                @Nonnull
                @Override
                public Set<Dependency<EntityStore>> getDependencies() {
                    return Collections.singleton(RootDependency.first());
                }
            };
            
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onChunkUnload ECS system");
            
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to register onChunkUnload system: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
