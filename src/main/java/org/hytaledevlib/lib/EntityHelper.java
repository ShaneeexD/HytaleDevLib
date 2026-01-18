package org.hytaledevlib.lib;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper utilities for working with entities.
 * Provides fundamental operations for entity management and queries.
 * 
 * Note: Some methods use deprecated API calls that are currently the only
 * available methods in the early Hytale API. These will be updated when
 * replacement methods become available.
 */
@SuppressWarnings("deprecation")
public class EntityHelper {
    
    /**
     * Get a player entity by their name.
     * 
     * @param world The world to search in
     * @param name Player name (case-insensitive)
     * @return Player entity, or null if not found
     */
    public static Entity getPlayerByName(World world, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        for (Entity player : world.getPlayers()) {
            String playerName = player.getLegacyDisplayName();
            if (playerName != null && playerName.equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Get a player entity by their UUID.
     * 
     * @param world The world to search in
     * @param uuid Player UUID
     * @return Player entity, or null if not found
     */
    public static Entity getPlayerByUUID(World world, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        for (Entity player : world.getPlayers()) {
            if (uuid.equals(player.getUuid())) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Get the position of an entity.
     * 
     * @param entity The entity
     * @return Position vector, or null if entity has no transform
     */
    public static Vector3d getPosition(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        TransformComponent transform = entity.getTransformComponent();
        return transform != null ? transform.getPosition() : null;
    }
    
    /**
     * Teleport an entity to a specific position.
     * 
     * @param entity The entity to teleport
     * @param position Target position
     * @return true if successful
     */
    public static boolean teleport(Entity entity, Vector3d position) {
        if (entity == null || position == null) {
            return false;
        }
        
        try {
            TransformComponent transform = entity.getTransformComponent();
            if (transform != null) {
                transform.teleportPosition(position);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    
    /**
     * Teleport an entity to specific coordinates.
     * 
     * @param entity The entity to teleport
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if successful
     */
    public static boolean teleport(Entity entity, double x, double y, double z) {
        return teleport(entity, new Vector3d(x, y, z));
    }
    
    /**
     * Calculate the distance between two entities.
     * 
     * @param entity1 First entity
     * @param entity2 Second entity
     * @return Distance in blocks, or -1 if either entity has no position
     */
    public static double getDistance(Entity entity1, Entity entity2) {
        Vector3d pos1 = getPosition(entity1);
        Vector3d pos2 = getPosition(entity2);
        
        if (pos1 == null || pos2 == null) {
            return -1;
        }
        
        return pos1.distanceTo(pos2);
    }
    
    /**
     * Calculate the distance between an entity and a position.
     * 
     * @param entity The entity
     * @param position The position
     * @return Distance in blocks, or -1 if entity has no position
     */
    public static double getDistance(Entity entity, Vector3d position) {
        Vector3d entityPos = getPosition(entity);
        
        if (entityPos == null || position == null) {
            return -1;
        }
        
        return entityPos.distanceTo(position);
    }
    
    /**
     * Get all players within a certain radius of a position.
     * 
     * @param world The world
     * @param center Center position
     * @param radius Radius in blocks
     * @return List of players within radius
     */
    public static List<Entity> getPlayersInRadius(World world, Vector3d center, double radius) {
        List<Entity> result = new ArrayList<>();
        
        if (center == null || radius < 0) {
            return result;
        }
        
        double radiusSquared = radius * radius;
        
        for (Entity player : world.getPlayers()) {
            Vector3d playerPos = getPosition(player);
            if (playerPos != null) {
                double distanceSquared = center.distanceSquaredTo(playerPos);
                if (distanceSquared <= radiusSquared) {
                    result.add(player);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get all players within a certain radius of an entity.
     * 
     * @param entity Center entity
     * @param radius Radius in blocks
     * @return List of players within radius (excluding the center entity if it's a player)
     */
    public static List<Entity> getPlayersInRadius(Entity entity, double radius) {
        Vector3d center = getPosition(entity);
        if (center == null) {
            return new ArrayList<>();
        }
        
        List<Entity> players = getPlayersInRadius(entity.getWorld(), center, radius);
        players.remove(entity); // Remove the center entity if it's in the list
        return players;
    }
    
    /**
     * Check if an entity is within a certain distance of a position.
     * 
     * @param entity The entity
     * @param position The position
     * @param distance Maximum distance
     * @return true if within distance
     */
    public static boolean isWithinDistance(Entity entity, Vector3d position, double distance) {
        double actualDistance = getDistance(entity, position);
        return actualDistance >= 0 && actualDistance <= distance;
    }
    
    /**
     * Check if two entities are within a certain distance of each other.
     * 
     * @param entity1 First entity
     * @param entity2 Second entity
     * @param distance Maximum distance
     * @return true if within distance
     */
    public static boolean isWithinDistance(Entity entity1, Entity entity2, double distance) {
        double actualDistance = getDistance(entity1, entity2);
        return actualDistance >= 0 && actualDistance <= distance;
    }
    
    /**
     * Get the name of an entity (player username for players, entity type for others).
     * For players, retrieves the username from the PlayerRef component.
     * For other entities, attempts to retrieve from DisplayNameComponent.
     * Falls back to entity class name if no display name is available.
     * 
     * @param entity The entity
     * @return Entity name or type
     */
    public static String getName(Entity entity) {
        if (entity == null) {
            return "Unknown";
        }
        
        try {
            World world = entity.getWorld();
            if (world == null) {
                return entity.getClass().getSimpleName();
            }
            
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = 
                world.getEntityStore().getStore();
            
            // Try PlayerRef for players
            if (isPlayer(entity)) {
                try {
                    com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, 
                        com.hypixel.hytale.server.core.universe.PlayerRef> playerRefType = 
                        com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType();
                    
                    com.hypixel.hytale.server.core.universe.PlayerRef playerRef = 
                        store.getComponent(entity.getReference(), playerRefType);
                    
                    if (playerRef != null) {
                        String username = playerRef.getUsername();
                        if (username != null && !username.isEmpty()) {
                            return username;
                        }
                    }
                } catch (Exception e) {
                    // Fall through to DisplayNameComponent
                }
            }
            
            // Try DisplayNameComponent for all entities (including NPCs)
            try {
                com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, 
                    com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent> displayNameType = 
                    com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent.getComponentType();
                
                com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent displayNameComp = 
                    store.getComponent(entity.getReference(), displayNameType);
                
                if (displayNameComp != null && displayNameComp.getDisplayName() != null) {
                    // The Message object is actually a FormattedMessage - use reflection to access rawText
                    Object message = displayNameComp.getDisplayName();
                    try {
                        java.lang.reflect.Field rawTextField = message.getClass().getField("rawText");
                        String rawText = (String) rawTextField.get(message);
                        if (rawText != null && !rawText.isEmpty()) {
                            return rawText;
                        }
                    } catch (Exception e) {
                        // If reflection fails, continue to fallbacks
                    }
                }
            } catch (Exception e) {
                // Fall through to legacy method
            }
            
        } catch (Exception e) {
            // Fall through to legacy method
        }
        
        // Fallback to legacy display name
        String name = entity.getLegacyDisplayName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        
        // Final fallback: return entity type (class name)
        // This is useful for NPCs and other entities that don't have display names set
        return entity.getClass().getSimpleName();
    }
    
    /**
     * Check if an entity is a player.
     * This checks if the entity is in the world's player list.
     * 
     * @param entity The entity
     * @return true if entity is a player
     */
    public static boolean isPlayer(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        World world = entity.getWorld();
        if (world == null) {
            return false;
        }
        
        return world.getPlayers().contains(entity);
    }
    
    /**
     * Check if an entity still exists in the world (not removed).
     * 
     * @param entity The entity
     * @return true if entity exists and is not removed
     */
    public static boolean exists(Entity entity) {
        return entity != null && !entity.wasRemoved();
    }
    
    /**
     * Get the entity type identifier for NPCs.
     * For NPCs, this returns the role index which identifies the specific NPC type.
     * For other entities, returns the class name.
     * 
     * @param entity The entity
     * @return Entity type identifier (e.g., "NPC_798" for NPCs, "ItemEntity" for items)
     */
    public static String getEntityType(Entity entity) {
        if (entity == null) {
            return "Unknown";
        }
        
        String className = entity.getClass().getSimpleName();
        
        // For NPCEntity, try to get the role name from the role object
        if ("NPCEntity".equals(className)) {
            try {
                // Access the role field
                java.lang.reflect.Field roleField = entity.getClass().getDeclaredField("role");
                roleField.setAccessible(true);
                Object role = roleField.get(entity);
                
                if (role != null) {
                    // Try to get the roleName field
                    try {
                        java.lang.reflect.Field roleNameField = role.getClass().getDeclaredField("roleName");
                        roleNameField.setAccessible(true);
                        String roleName = (String) roleNameField.get(role);
                        
                        if (roleName != null && !roleName.isEmpty()) {
                            return roleName;
                        }
                    } catch (Exception e) {
                        // If roleName fails, try roleIndex as fallback
                    }
                    
                    // Fallback: try to get roleIndex
                    try {
                        java.lang.reflect.Field roleIndexField = role.getClass().getDeclaredField("roleIndex");
                        roleIndexField.setAccessible(true);
                        int roleIndex = roleIndexField.getInt(role);
                        
                        if (roleIndex != Integer.MIN_VALUE && roleIndex >= 0) {
                            return "NPC_" + roleIndex;
                        }
                        if (roleIndex == Integer.MIN_VALUE) {
                            return "NPCEntity_Uninitialized";
                        }
                    } catch (Exception e) {
                        // Fall through to class name
                    }
                }
            } catch (Exception e) {
                // Fall through to class name
            }
        }
        
        return className;
    }
    
    /**
     * Get all loaded entities in the world.
     * 
     * This method accesses the EntityStore's internal entity map to retrieve
     * all currently loaded entities. It uses reflection to access the entitiesByUuid
     * field and converts the Refs to Entity objects.
     * 
     * @param world The world
     * @return List of all loaded entities
     */
    public static java.util.List<Entity> getAllEntities(World world) {
        java.util.List<Entity> entities = new java.util.ArrayList<>();
        
        if (world == null) {
            return entities;
        }
        
        try {
            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
            
            // Access the entitiesByUuid map using reflection
            java.lang.reflect.Field entitiesField = entityStore.getClass().getDeclaredField("entitiesByUuid");
            entitiesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>> entitiesMap = 
                (java.util.Map<java.util.UUID, com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>>) 
                entitiesField.get(entityStore);
            
            // Convert UUIDs to Entities
            for (java.util.UUID uuid : entitiesMap.keySet()) {
                try {
                    Entity entity = world.getEntity(uuid);
                    if (entity != null && !entity.wasRemoved()) {
                        entities.add(entity);
                    }
                } catch (Exception e) {
                    // Skip invalid entities
                }
            }
            
        } catch (Exception e) {
            // If reflection fails, fall back to players only
            try {
                for (Entity player : world.getPlayers()) {
                    entities.add(player);
                }
            } catch (Exception ex) {
                // Return empty list
            }
        }
        
        return entities;
    }
    
    /**
     * Find the closest entity to a given position.
     * Searches through all loaded entities in the world.
     * 
     * @param world The world
     * @param position The position to search from
     * @param excludeEntity Optional entity to exclude from search (e.g., the source entity)
     * @return Closest entity, or null if none found
     */
    public static Entity getClosestEntity(World world, Vector3d position, Entity excludeEntity) {
        if (world == null || position == null) {
            return null;
        }
        
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : getAllEntities(world)) {
            // Skip the excluded entity
            if (excludeEntity != null && entity.equals(excludeEntity)) {
                continue;
            }
            
            double distance = getDistance(entity, position);
            if (distance >= 0 && distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        
        return closest;
    }
    
    /**
     * Find the closest entity to a given position.
     * Searches through all loaded entities in the world.
     * 
     * @param world The world
     * @param position The position to search from
     * @return Closest entity, or null if none found
     */
    public static Entity getClosestEntity(World world, Vector3d position) {
        return getClosestEntity(world, position, null);
    }
    
    /**
     * Get the player's respawn/home position.
     * 
     * This retrieves the player's respawn position (typically their bed location or world spawn).
     * Uses the same logic as HomeOrSpawnPoint.respawnPlayer() to get the player's home base.
     * 
     * @param player The player entity
     * @return The player's respawn position as a Transform, or null if not available
     */
    public static com.hypixel.hytale.math.vector.Transform getPlayerRespawnPosition(Entity player) {
        if (player == null || !isPlayer(player)) {
            return null;
        }
        
        try {
            World world = player.getWorld();
            if (world == null) {
                return null;
            }
            
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerRef = 
                player.getReference();
            String worldName = world.getName();
            
            com.hypixel.hytale.server.core.universe.world.storage.EntityStore entityStore = world.getEntityStore();
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = 
                entityStore.getStore();
            
            // Call Player.getRespawnPosition() static method
            com.hypixel.hytale.math.vector.Transform respawnTransform = 
                com.hypixel.hytale.server.core.entity.entities.Player.getRespawnPosition(
                    playerRef, 
                    worldName, 
                    store
                );
            
            return respawnTransform;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the player's respawn/home position as a Vector3d.
     * 
     * Convenience method that returns just the position vector instead of the full Transform.
     * 
     * @param player The player entity
     * @return The player's respawn position, or null if not available
     */
    public static com.hypixel.hytale.math.vector.Vector3d getPlayerHome(Entity player) {
        com.hypixel.hytale.math.vector.Transform transform = getPlayerRespawnPosition(player);
        if (transform != null) {
            return transform.getPosition();
        }
        return null;
    }
    
    /**
     * Teleport a player to their home/respawn location.
     * 
     * @param player The player to teleport
     * @return true if teleport was successful, false otherwise
     */
    public static boolean teleportPlayerHome(Entity player) {
        com.hypixel.hytale.math.vector.Vector3d homePos = getPlayerHome(player);
        if (homePos != null) {
            return teleport(player, homePos);
        }
        return false;
    }
    
    /**
     * Find the closest entity to another entity.
     * 
     * @param entity The entity to search from
     * @param maxDistance Maximum search distance (in blocks)
     * @return Closest entity within range, or null if none found
     */
    public static Entity getClosestEntity(Entity entity, double maxDistance) {
        Vector3d pos = getPosition(entity);
        if (pos == null) {
            return null;
        }
        
        World world = entity.getWorld();
        Entity closest = getClosestEntity(world, pos);
        
        // Exclude the entity itself and check distance
        if (closest != null && closest.equals(entity)) {
            return null;
        }
        
        if (closest != null && getDistance(entity, closest) <= maxDistance) {
            return closest;
        }
        
        return null;
    }
    
    /**
     * Spawn an NPC entity by role name at a specific position.
     * This uses the internal NPCPlugin to properly spawn entities with all required components.
     * 
     * @param world The world to spawn in
     * @param roleName The role name (e.g., "Cow", "Deer_Doe", "Skeleton_Fighter")
     * @param position The spawn position
     * @return The spawned entity, or null if spawning failed
     */
    public static Entity spawnNPC(World world, String roleName, Vector3d position) {
        return spawnNPC(world, roleName, position, null);
    }
    
    /**
     * Spawn an NPC entity by role name at a specific position with rotation.
     * This uses the internal NPCPlugin to properly spawn entities with all required components.
     * 
     * @param world The world to spawn in
     * @param roleName The role name (e.g., "Cow", "Deer_Doe", "Skeleton_Fighter")
     * @param position The spawn position
     * @param yaw The yaw rotation in radians (null for default)
     * @return The spawned entity, or null if spawning failed
     */
    public static Entity spawnNPC(World world, String roleName, Vector3d position, Float yaw) {
        if (world == null || roleName == null || position == null) {
            return null;
        }
        
        try {
            // Get NPCPlugin instance
            com.hypixel.hytale.server.npc.NPCPlugin npcPlugin = com.hypixel.hytale.server.npc.NPCPlugin.get();
            
            // Get the role index from the role name
            int roleIndex = npcPlugin.getIndex(roleName);
            if (roleIndex < 0) {
                return null; // Role not found
            }
            
            // Create rotation vector (yaw only, pitch and roll are 0)
            com.hypixel.hytale.math.vector.Vector3f rotation = yaw != null 
                ? new com.hypixel.hytale.math.vector.Vector3f(0.0f, yaw, 0.0f)
                : new com.hypixel.hytale.math.vector.Vector3f(0.0f, 0.0f, 0.0f);
            
            // Get the entity store
            com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = 
                world.getEntityStore().getStore();
            
            // Spawn the entity using NPCPlugin
            it.unimi.dsi.fastutil.Pair<
                com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>,
                com.hypixel.hytale.server.npc.entities.NPCEntity
            > result = npcPlugin.spawnEntity(store, roleIndex, position, rotation, null, null);
            
            if (result == null) {
                return null;
            }
            
            // Return the NPCEntity (which extends Entity)
            return result.second();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Spawn an NPC entity at coordinates.
     * 
     * @param world The world to spawn in
     * @param roleName The role name (e.g., "Cow", "Deer_Doe", "Skeleton_Fighter")
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The spawned entity, or null if spawning failed
     */
    public static Entity spawnNPC(World world, String roleName, double x, double y, double z) {
        return spawnNPC(world, roleName, new Vector3d(x, y, z), null);
    }
    
    /**
     * Spawn an NPC entity at coordinates with rotation.
     * 
     * @param world The world to spawn in
     * @param roleName The role name (e.g., "Cow", "Deer_Doe", "Skeleton_Fighter")
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param yaw The yaw rotation in radians (null for default)
     * @return The spawned entity, or null if spawning failed
     */
    public static Entity spawnNPC(World world, String roleName, double x, double y, double z, Float yaw) {
        return spawnNPC(world, roleName, new Vector3d(x, y, z), yaw);
    }
    
    /**
     * Get the UUID of an entity.
     * Uses the deprecated getUuid() method as it's currently the only available method.
     * 
     * @param entity The entity
     * @return The entity's UUID, or null if entity is null
     */
    public static UUID getUUID(Entity entity) {
        return entity != null ? entity.getUuid() : null;
    }
    
    /**
     * Get all entities in a world.
     * Uses the deprecated getPlayers() method for now.
     * 
     * @param world The world
     * @return List of all entities (currently only players)
     */
    public static List<Entity> getEntities(World world) {
        List<Entity> entities = new ArrayList<>();
        if (world != null) {
            // Currently only returns players as there's no getEntities() method
            for (Entity player : world.getPlayers()) {
                entities.add(player);
            }
        }
        return entities;
    }
}
