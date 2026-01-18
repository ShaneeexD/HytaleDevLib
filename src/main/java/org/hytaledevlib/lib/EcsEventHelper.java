package org.hytaledevlib.lib;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
}
