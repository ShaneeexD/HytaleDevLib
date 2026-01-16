package org.hytaledevlib.lib;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * PlayerHelper provides simplified player-specific utilities.
 * 
 * Note: This helper focuses on simple, direct operations available on the Player class.
 * For more complex operations like health, effects, and velocity, you'll need to work
 * with the ECS system directly or wait for future helper expansions.
 */
public class PlayerHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Get the player's current game mode.
     * 
     * @param entity The entity (must be a Player)
     * @return The player's GameMode, or null if not a player or error occurs
     */
    @Nullable
    public static GameMode getGameMode(Entity entity) {
        if (!(entity instanceof Player)) {
            return null;
        }

        try {
            Player player = (Player) entity;
            return player.getGameMode();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting game mode: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send a message to a player.
     * 
     * @param entity The entity (must be a Player)
     * @param message The message to send
     * @return true if message was sent successfully
     */
    public static boolean sendMessage(Entity entity, String message) {
        if (!(entity instanceof Player)) {
            return false;
        }

        try {
            Player player = (Player) entity;
            player.sendMessage(Message.raw(message));
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error sending message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a formatted message to a player.
     * 
     * @param entity The entity (must be a Player)
     * @param message The Message object to send
     * @return true if message was sent successfully
     */
    public static boolean sendMessage(Entity entity, Message message) {
        if (!(entity instanceof Player)) {
            return false;
        }

        try {
            Player player = (Player) entity;
            player.sendMessage(message);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error sending message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a player has a specific permission.
     * 
     * @param entity The entity (must be a Player)
     * @param permission The permission string to check
     * @return true if the player has the permission
     */
    public static boolean hasPermission(Entity entity, String permission) {
        if (!(entity instanceof Player)) {
            return false;
        }

        try {
            Player player = (Player) entity;
            return player.hasPermission(permission);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a player has a specific permission with a default value.
     * 
     * @param entity The entity (must be a Player)
     * @param permission The permission string to check
     * @param defaultValue The default value if permission is not set
     * @return true if the player has the permission or default value
     */
    public static boolean hasPermission(Entity entity, String permission, boolean defaultValue) {
        if (!(entity instanceof Player)) {
            return defaultValue;
        }

        try {
            Player player = (Player) entity;
            return player.hasPermission(permission, defaultValue);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking permission: " + e.getMessage());
            return defaultValue;
        }
    }
}
