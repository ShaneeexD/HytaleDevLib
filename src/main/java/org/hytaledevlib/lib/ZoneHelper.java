package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Helper class for zone/region management and discovery tracking.
 * Provides utilities for tracking player zone discoveries and querying zone information.
 */
public class ZoneHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Store discovered zones per player (username -> Set of zone names)
    private static final Map<String, Set<String>> playerDiscoveredZones = new ConcurrentHashMap<>();
    
    // Store current zone per player (username -> zone name)
    private static final Map<String, String> playerCurrentZone = new ConcurrentHashMap<>();
    
    /**
     * Initialize zone discovery tracking for a world.
     * 
     * Note: This method is kept for API compatibility but zone discovery events
     * should be registered using EcsEventHelper.onZoneDiscovery() directly.
     * See TestPlugin.registerEcsEventTests() for an example.
     * 
     * @param world The world to track zones in
     */
    public static void initializeZoneTracking(World world) {
        LOGGER.at(Level.INFO).log("Zone tracking initialized for world: " + world.getName());
        LOGGER.at(Level.INFO).log("Note: Use EcsEventHelper.onZoneDiscovery() to register zone discovery events");
    }
    
    /**
     * Get the current zone a player is in.
     * 
     * @param player The player entity
     * @return The zone name, or null if not in a tracked zone
     */
    @Nullable
    public static String getCurrentZone(com.hypixel.hytale.server.core.entity.Entity player) {
        try {
            if (!(player instanceof Player)) {
                return null;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return null;
            }
            return playerCurrentZone.get(username);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting current zone: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the current zone for a player.
     * This is typically called automatically by zone tracking systems.
     * 
     * @param player The player entity
     * @param zoneName The zone name
     */
    public static void setCurrentZone(com.hypixel.hytale.server.core.entity.Entity player, String zoneName) {
        try {
            if (!(player instanceof Player)) {
                return;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return;
            }
            
            if (zoneName == null) {
                playerCurrentZone.remove(username);
            } else {
                playerCurrentZone.put(username, zoneName);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error setting current zone: " + e.getMessage());
        }
    }
    
    /**
     * Check if a player is in a specific zone.
     * 
     * @param player The player entity
     * @param zoneName The zone name to check
     * @return true if the player is in the specified zone
     */
    public static boolean isInZone(com.hypixel.hytale.server.core.entity.Entity player, String zoneName) {
        try {
            String currentZone = getCurrentZone(player);
            return zoneName != null && zoneName.equals(currentZone);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking if in zone: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all zones a player has discovered.
     * 
     * @param player The player entity
     * @return List of discovered zone names
     */
    public static List<String> getDiscoveredZones(com.hypixel.hytale.server.core.entity.Entity player) {
        try {
            if (!(player instanceof Player)) {
                return Collections.emptyList();
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return Collections.emptyList();
            }
            
            Set<String> zones = playerDiscoveredZones.get(username);
            
            if (zones == null) {
                return Collections.emptyList();
            }
            
            return new ArrayList<>(zones);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting discovered zones: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Mark a zone as discovered for a player.
     * 
     * @param player The player entity
     * @param zoneName The zone name to mark as discovered
     * @return true if this is a new discovery, false if already discovered
     */
    public static boolean discoverZone(com.hypixel.hytale.server.core.entity.Entity player, String zoneName) {
        try {
            if (!(player instanceof Player) || zoneName == null) {
                return false;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return false;
            }
            
            Set<String> zones = playerDiscoveredZones.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
            
            boolean wasNew = zones.add(zoneName);
            
            if (wasNew) {
                LOGGER.at(Level.INFO).log("Player " + username + " discovered zone: " + zoneName);
            }
            
            return wasNew;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error discovering zone: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a player has discovered a specific zone.
     * 
     * @param player The player entity
     * @param zoneName The zone name to check
     * @return true if the player has discovered this zone
     */
    public static boolean hasDiscoveredZone(com.hypixel.hytale.server.core.entity.Entity player, String zoneName) {
        try {
            if (!(player instanceof Player) || zoneName == null) {
                return false;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return false;
            }
            
            Set<String> zones = playerDiscoveredZones.get(username);
            
            return zones != null && zones.contains(zoneName);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking discovered zone: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all players currently in a specific zone.
     * 
     * @param world The world to search in
     * @param zoneName The zone name
     * @return List of players in the zone
     */
    public static List<com.hypixel.hytale.server.core.entity.Entity> getPlayersInZone(World world, String zoneName) {
        List<com.hypixel.hytale.server.core.entity.Entity> playersInZone = new ArrayList<>();
        
        try {
            if (zoneName == null) {
                return playersInZone;
            }
            
            List<com.hypixel.hytale.server.core.entity.Entity> allPlayers = EntityHelper.getAllEntities(world);
            
            // Filter to only players
            List<com.hypixel.hytale.server.core.entity.Entity> players = new ArrayList<>();
            for (com.hypixel.hytale.server.core.entity.Entity entity : allPlayers) {
                if (entity instanceof Player) {
                    players.add(entity);
                }
            }
            
            for (com.hypixel.hytale.server.core.entity.Entity player : players) {
                if (isInZone(player, zoneName)) {
                    playersInZone.add(player);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting players in zone: " + e.getMessage());
        }
        
        return playersInZone;
    }
    
    /**
     * Get the number of zones a player has discovered.
     * 
     * @param player The player entity
     * @return The count of discovered zones
     */
    public static int getDiscoveredZoneCount(com.hypixel.hytale.server.core.entity.Entity player) {
        try {
            if (!(player instanceof Player)) {
                return 0;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return 0;
            }
            
            Set<String> zones = playerDiscoveredZones.get(username);
            
            return zones != null ? zones.size() : 0;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting discovered zone count: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Clear all discovered zones for a player.
     * Useful for resetting player progress.
     * 
     * @param player The player entity
     */
    public static void clearDiscoveredZones(com.hypixel.hytale.server.core.entity.Entity player) {
        try {
            if (!(player instanceof Player)) {
                return;
            }
            
            String username = EntityHelper.getName(player);
            if (username == null) {
                return;
            }
            
            playerDiscoveredZones.remove(username);
            playerCurrentZone.remove(username);
            
            LOGGER.at(Level.INFO).log("Cleared discovered zones for player " + username);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error clearing discovered zones: " + e.getMessage());
        }
    }
    
    /**
     * Get all unique zones that have been discovered by any player.
     * 
     * @return Set of all discovered zone names
     */
    public static Set<String> getAllDiscoveredZones() {
        Set<String> allZones = new HashSet<>();
        
        try {
            for (Set<String> zones : playerDiscoveredZones.values()) {
                allZones.addAll(zones);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting all discovered zones: " + e.getMessage());
        }
        
        return allZones;
    }
    
    /**
     * Get the total number of players who have discovered a specific zone.
     * 
     * @param zoneName The zone name
     * @return The count of players who discovered this zone
     */
    public static int getZoneDiscoveryCount(String zoneName) {
        int count = 0;
        
        try {
            if (zoneName == null) {
                return 0;
            }
            
            for (Set<String> zones : playerDiscoveredZones.values()) {
                if (zones.contains(zoneName)) {
                    count++;
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting zone discovery count: " + e.getMessage());
        }
        
        return count;
    }
}
