package org.hytaledevlib.lib;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

/**
 * DeathHelper - Helper for tracking entity deaths
 * 
 * Provides callbacks when entities (players or NPCs) die, including:
 * - The entity that died
 * - Death position
 * - Damage source (what killed the entity)
 * - Damage cause (how they died)
 */
public class DeathHelper {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /**
     * Register a callback for when any entity dies.
     * 
     * This creates and registers an ECS system to handle DeathComponent additions.
     * Must be called after you have a World instance.
     * 
     * @param world The world to register the system in
     * @param callback Consumer that receives death information
     */
    public static void onEntityDeath(World world, Consumer<EntityDeath> callback) {
        try {
            RefChangeSystem<EntityStore, DeathComponent> system = new RefChangeSystem<EntityStore, DeathComponent>() {
                @Nonnull
                @Override
                public ComponentType<EntityStore, DeathComponent> componentType() {
                    return DeathComponent.getComponentType();
                }
                
                @Nonnull
                @Override
                public Query<EntityStore> getQuery() {
                    return Archetype.empty(); // Listen to all entities
                }
                
                @Nonnull
                @Override
                public Set<Dependency<EntityStore>> getDependencies() {
                    return Collections.singleton(RootDependency.first());
                }
                
                @Override
                public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
                    try {
                        // Get death position
                        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                        Vector3d position = transform != null ? transform.getPosition() : null;
                        
                        // Get death info (damage source and cause)
                        Damage deathInfo = component.getDeathInfo();
                        
                        // Create death object and call callback
                        EntityDeath death = new EntityDeath(ref, position, deathInfo, store, world);
                        callback.accept(death);
                        
                    } catch (Exception e) {
                        LOGGER.atWarning().log("Error in onEntityDeath callback: " + e.getMessage());
                    }
                }
                
                @Override
                public void onComponentSet(@Nonnull Ref<EntityStore> ref, DeathComponent oldComponent,
                                          @Nonnull DeathComponent newComponent, @Nonnull Store<EntityStore> store,
                                          @Nonnull CommandBuffer<EntityStore> commandBuffer) {
                    // Not used
                }
                
                @Override
                public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
                    // Not used
                }
            };
            
            EntityStore.REGISTRY.registerSystem(system);
            LOGGER.atInfo().log("Registered onEntityDeath ECS system");
            
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to register onEntityDeath system: " + e.getMessage());
        }
    }
    
    /**
     * Entity death information.
     */
    public static class EntityDeath {
        private final Ref<EntityStore> entityRef;
        private final Vector3d position;
        private final Damage damageInfo;
        private final Store<EntityStore> store;
        private final World world;
        
        public EntityDeath(Ref<EntityStore> entityRef, Vector3d position, Damage damageInfo, Store<EntityStore> store, World world) {
            this.entityRef = entityRef;
            this.position = position;
            this.damageInfo = damageInfo;
            this.store = store;
            this.world = world;
        }
        
        /**
         * Get the entity reference that died.
         */
        public Ref<EntityStore> getEntityRef() {
            return entityRef;
        }
        
        /**
         * Get the store to access entity components.
         */
        public Store<EntityStore> getStore() {
            return store;
        }
        
        /**
         * Get the position where the entity died.
         */
        @Nullable
        public Vector3d getPosition() {
            return position;
        }
        
        /**
         * Get the damage information (source and cause).
         */
        @Nullable
        public Damage getDamageInfo() {
            return damageInfo;
        }
        
        /**
         * Get the damage source (what killed the entity).
         * Returns EntitySource, EnvironmentSource, CommandSource, or ProjectileSource.
         */
        @Nullable
        public Damage.Source getDamageSource() {
            return damageInfo != null ? damageInfo.getSource() : null;
        }
        
        /**
         * Check if the entity was killed by another entity.
         */
        public boolean wasKilledByEntity() {
            return getDamageSource() instanceof Damage.EntitySource;
        }
        
        /**
         * Check if the entity was killed by a projectile.
         */
        public boolean wasKilledByProjectile() {
            return getDamageSource() instanceof Damage.ProjectileSource;
        }
        
        /**
         * Check if the entity was killed by environment (fall, drowning, etc.).
         */
        public boolean wasKilledByEnvironment() {
            return getDamageSource() instanceof Damage.EnvironmentSource;
        }
        
        /**
         * Get the killer entity if killed by an entity.
         */
        @Nullable
        public Ref<EntityStore> getKillerRef() {
            if (getDamageSource() instanceof Damage.EntitySource entitySource) {
                return entitySource.getRef();
            }
            return null;
        }
        
        /**
         * Get the killer entity reference if killed by an entity.
         */
        @Nullable
        public Ref<EntityStore> getKiller() {
            return getKillerRef();
        }
        
        /**
         * Get the environment type if killed by environment.
         */
        @Nullable
        public String getEnvironmentType() {
            if (getDamageSource() instanceof Damage.EnvironmentSource envSource) {
                return envSource.getType();
            }
            return null;
        }
        
        /**
         * Get the name of the entity that died.
         * Returns player username for players, NPC name for NPCs, or "Unknown" if unavailable.
         */
        public String getEntityName() {
            return getNameFromRef(entityRef);
        }
        
        /**
         * Get the name of the killer entity.
         * Returns player username for players, NPC name for NPCs, or null if not killed by entity.
         */
        @Nullable
        public String getKillerName() {
            Ref<EntityStore> killerRef = getKillerRef();
            return killerRef != null ? getNameFromRef(killerRef) : null;
        }
        
        /**
         * Check if the entity that died is a player.
         */
        public boolean isPlayer() {
            return isPlayerRef(entityRef);
        }
        
        /**
         * Check if the killer is a player.
         */
        public boolean isKillerPlayer() {
            Ref<EntityStore> killerRef = getKillerRef();
            return killerRef != null && isPlayerRef(killerRef);
        }
        
        /**
         * Helper method to extract name from an entity reference.
         * For players, returns username. For NPCs, returns the role name (e.g., "Cow", "Skeleton_Fighter").
         */
        private String getNameFromRef(Ref<EntityStore> ref) {
            if (ref == null || !ref.isValid()) {
                return "Unknown";
            }
            
            try {
                // Check if it's a player first
                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = 
                    store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                
                if (playerRef != null) {
                    String username = playerRef.getUsername();
                    return username != null ? username : "Unknown Player";
                }
                
                // For NPCs, get the entity from world and use EntityHelper.getEntityType()
                // which extracts the role name from the NPC's role object
                com.hypixel.hytale.server.core.entity.UUIDComponent uuidComp = 
                    store.getComponent(ref, com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
                
                if (uuidComp != null && world != null) {
                    java.util.UUID uuid = uuidComp.getUuid();
                    com.hypixel.hytale.server.core.entity.Entity entity = world.getEntity(uuid);
                    
                    if (entity != null) {
                        // Use EntityHelper to get the NPC role name
                        return org.hytaledevlib.lib.EntityHelper.getEntityType(entity);
                    }
                }
                
                return "Unknown";
            } catch (Exception e) {
                return "Unknown";
            }
        }
        
        /**
         * Helper method to check if a reference is a player.
         */
        private boolean isPlayerRef(Ref<EntityStore> ref) {
            if (ref == null || !ref.isValid()) {
                return false;
            }
            
            try {
                com.hypixel.hytale.server.core.universe.PlayerRef playerRef = 
                    store.getComponent(ref, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                return playerRef != null;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
