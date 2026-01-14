package org.grounditems.lib;

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
     * Get the name of an entity (player username for players).
     * For players, retrieves the username from the PlayerRef component.
     * 
     * @param entity The entity
     * @return Player username, or "Unknown" if not available
     */
    public static String getName(Entity entity) {
        if (entity == null) {
            return "Unknown";
        }
        
        // Try to get PlayerRef component for players
        try {
            World world = entity.getWorld();
            if (world != null && isPlayer(entity)) {
                com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store = 
                    world.getEntityStore().getStore();
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
            }
        } catch (Exception e) {
            // Fall through to legacy method
        }
        
        // Fallback to legacy display name
        String name = entity.getLegacyDisplayName();
        return (name != null && !name.isEmpty()) ? name : "Unknown";
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
}
