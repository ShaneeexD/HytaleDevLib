package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * ParticleHelper provides simplified methods for spawning particle effects in Hytale.
 * 
 * Features:
 * - Spawn particles at positions or entities
 * - Control visibility (all nearby players, specific players, or single player)
 * - Customize particle scale
 * - Automatic player detection within range
 * 
 * Particle Lifetimes:
 * - Particle duration is controlled by the particle system definition itself
 * - Most particle systems are designed to play once and disappear automatically
 * - Some particles (like ambient effects) may loop indefinitely by design
 * - See ParticleList.md for all available particle system IDs (535 total)
 * 
 * Example particle system IDs:
 * - "Impact_Fire" - Fire impact effect
 * - "Dust_Sparkles" - Sparkle dust particles
 * - "Water_Sprint" - Water splash from sprinting
 * - "Explosion_Medium" - Medium explosion effect
 * - "Block_Hit_Crystal" - Crystal block hit particles
 * - And 530+ more defined in Hytale's particle assets
 */
public class ParticleHelper {
    private static final Logger LOGGER = System.getLogger(ParticleHelper.class.getName());
    private static final double DEFAULT_PARTICLE_DISTANCE = 75.0;
    
    /**
     * Spawn a particle effect at a specific position, visible to all nearby players.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID (e.g., "Sparkle", "Smoke", "Flame")
     * @param position The position to spawn particles at
     */
    public static void spawnParticle(World world, String particleSystemId, Vector3i position) {
        spawnParticle(world, particleSystemId, new Vector3d(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5));
    }
    
    /**
     * Spawn a particle effect at a specific position, visible to all nearby players.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID (e.g., "Sparkle", "Smoke", "Flame")
     * @param position The position to spawn particles at
     */
    public static void spawnParticle(World world, String particleSystemId, Vector3d position) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            ParticleUtil.spawnParticleEffect(particleSystemId, position, store);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particle '" + particleSystemId + "': " + e.getMessage());
        }
    }
    
    /**
     * Spawn a particle effect at a specific position with custom scale.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID
     * @param position The position to spawn particles at
     * @param scale The scale multiplier (1.0 = normal size, 2.0 = double size)
     */
    public static void spawnParticle(World world, String particleSystemId, Vector3d position, float scale) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            
            // Get nearby players
            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource = 
                store.getResource(EntityModule.get().getPlayerSpatialResourceType());
            ObjectList<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(position, DEFAULT_PARTICLE_DISTANCE, playerRefs);
            
            // Spawn particle with custom scale
            ParticleUtil.spawnParticleEffect(
                particleSystemId,
                position.getX(), position.getY(), position.getZ(),
                0.0f, 0.0f, 0.0f, // No rotation
                scale,
                null, // No custom color
                null, // No source entity
                playerRefs,
                store
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn scaled particle '" + particleSystemId + "': " + e.getMessage());
        }
    }
    
    /**
     * Spawn a particle effect at an entity's position, visible to all nearby players.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID
     * @param entity The entity to spawn particles at
     */
    public static void spawnParticleAtEntity(World world, String particleSystemId, Entity entity) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ref = entity.getReference();
            TransformComponent transform = ComponentHelper.getComponent(store, ref, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d position = transform.getPosition();
                spawnParticle(world, particleSystemId, position);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particle at entity: " + e.getMessage());
        }
    }
    
    /**
     * Spawn a particle effect visible only to a specific player.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID
     * @param position The position to spawn particles at
     * @param player The player entity who should see the particles
     */
    public static void spawnParticleForPlayer(World world, String particleSystemId, Vector3d position, Entity player) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ref = player.getReference();
            PlayerRef playerRef = ComponentHelper.getComponent(store, ref, PlayerRef.getComponentType());
            
            if (playerRef != null) {
                List<Ref<EntityStore>> playerRefs = new ArrayList<>();
                playerRefs.add(ref);
                
                ParticleUtil.spawnParticleEffect(
                    particleSystemId,
                    position.getX(), position.getY(), position.getZ(),
                    null, // No source entity
                    playerRefs,
                    store
                );
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particle for player: " + e.getMessage());
        }
    }
    
    /**
     * Spawn a particle effect at a block position.
     * Useful for visual feedback on block interactions.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID
     * @param blockPosition The block position
     */
    public static void spawnParticleAtBlock(World world, String particleSystemId, Vector3i blockPosition) {
        Vector3d position = new Vector3d(
            blockPosition.getX() + 0.5,
            blockPosition.getY() + 0.5,
            blockPosition.getZ() + 0.5
        );
        spawnParticle(world, particleSystemId, position);
    }
    
    /**
     * Spawn a particle effect at an entity's feet position.
     * Useful for ground-based effects like footstep particles.
     * 
     * @param world The world to spawn particles in
     * @param particleSystemId The particle system ID
     * @param entity The entity to spawn particles at
     */
    public static void spawnParticleAtEntityFeet(World world, String particleSystemId, Entity entity) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ref = entity.getReference();
            TransformComponent transform = ComponentHelper.getComponent(store, ref, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d position = transform.getPosition();
                // Spawn at feet (slightly below entity center)
                Vector3d feetPosition = new Vector3d(position.getX(), position.getY() - 0.9, position.getZ());
                spawnParticle(world, particleSystemId, feetPosition);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to spawn particle at entity feet: " + e.getMessage());
        }
    }
    
}
