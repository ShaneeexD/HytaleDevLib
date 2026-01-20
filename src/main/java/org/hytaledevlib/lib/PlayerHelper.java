package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
     * Get the Player component from an entity.
     * Internal helper method used by other helpers.
     * 
     * @param entity The entity
     * @return The Player component, or null if not a player
     */
    @Nullable
    static Player getPlayerComponent(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }
        return null;
    }

    /**
     * Check if an entity is a player.
     * 
     * @param entity The entity to check
     * @return true if the entity is a player
     */
    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }
    
    /**
     * Get a player's skin data containing all cosmetic information.
     * The PlayerSkin contains:
     * - Body characteristics
     * - Face features (eyes, mouth, ears, facial hair, eyebrows)
     * - Clothing (pants, tops, shoes, gloves)
     * - Accessories (head, face, ear accessories, cape)
     * 
     * Note: This returns the skin data, not a rendered image. The avatar image
     * you see in menus is rendered client-side from this data.
     * 
     * @param entity The player entity
     * @return PlayerSkin object with all cosmetic data, or null if not available
     */
    @Nullable
    public static PlayerSkin getPlayerSkin(Entity entity) {
        try {
            Ref<EntityStore> ref = entity.getReference();
            if (ref == null) return null;
            
            Store<EntityStore> store = ref.getStore();
            PlayerSkinComponent skinComponent = 
                store.getComponent(ref, PlayerSkinComponent.getComponentType());
            
            return skinComponent != null ? skinComponent.getPlayerSkin() : null;
        } catch (Exception e) {
            return null;
        }
    }

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
     * Set the player's game mode.
     * 
     * This method uses the Player class's internal setGameMode method which:
     * - Fires a ChangeGameModeEvent (can be cancelled)
     * - Updates movement manager
     * - Sends game mode packet to client
     * - Runs game mode switch handlers
     * 
     * @param world The world the player is in
     * @param entity The entity (must be a Player)
     * @param gameMode The GameMode to set (CREATIVE, ADVENTURE)
     * @return true if game mode was changed successfully, false if not a player or event was cancelled
     */
    public static boolean setGameMode(World world, Entity entity, GameMode gameMode) {
        if (!(entity instanceof Player)) {
            return false;
        }

        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            
            // Get the player's entity reference
            Ref<EntityStore> playerRef = entity.getReference();
            if (playerRef == null) {
                LOGGER.at(Level.WARNING).log("Could not get entity reference for player");
                return false;
            }
            
            // Call Player.setGameMode static method
            Player.setGameMode(playerRef, gameMode, store);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error setting game mode: " + e.getMessage());
            return false;
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

    /**
     * Result object containing information about the block a player is looking at.
     */
    public static class LookingAtResult {
        private final Vector3i blockPosition;
        private final BlockType blockType;
        private final double distance;

        public LookingAtResult(Vector3i blockPosition, BlockType blockType, double distance) {
            this.blockPosition = blockPosition;
            this.blockType = blockType;
            this.distance = distance;
        }

        /**
         * Get the position of the block.
         * @return Block position
         */
        public Vector3i getBlockPosition() {
            return blockPosition;
        }

        /**
         * Get the block type.
         * @return BlockType
         */
        public BlockType getBlockType() {
            return blockType;
        }

        /**
         * Get the block type ID string.
         * @return Block type ID (e.g., "Rock_Stone")
         */
        public String getBlockId() {
            return blockType != null ? blockType.getId() : null;
        }

        /**
         * Get the distance to the block.
         * @return Distance in blocks
         */
        public double getDistance() {
            return distance;
        }

        /**
         * Check if a block was found.
         * @return true if a block was found
         */
        public boolean hasBlock() {
            return blockPosition != null && blockType != null;
        }
    }

    /**
     * Get the block a player is looking at with distance information.
     * This is a simple 1-2 line method to get what a player is looking at.
     * 
     * 
     * @param world The world
     * @param entity The player entity  
     * @param checkDistance Maximum distance to check (e.g., 0.5 for close, 5.0 for far)
     * @return LookingAtResult containing block info and distance, or empty result if no block found
     */
    @SuppressWarnings("deprecation")
    public static LookingAtResult getLookingAt(World world, Entity entity, double checkDistance) {
        if (!(entity instanceof Player)) {
            return new LookingAtResult(null, null, 0.0);
        }

        try {
            Player player = (Player) entity;
            
            // Get the block the player is looking at
            Vector3i targetBlockPos = com.hypixel.hytale.server.core.util.TargetUtil.getTargetBlock(
                player.getPlayerRef().getReference(), 
                checkDistance, 
                player.getPlayerRef().getReference().getStore()
            );

            if (targetBlockPos != null) {
                BlockType blockType = world.getBlockType(targetBlockPos);
                return new LookingAtResult(targetBlockPos, blockType, checkDistance);
            }

            return new LookingAtResult(null, null, 0.0);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting looking at block: " + e.getMessage());
            return new LookingAtResult(null, null, 0.0);
        }
    }

    /**
     * Get the block a player is looking at with default distance of 5.0 blocks.
     * 
     * @param world The world
     * @param entity The player entity
     * @return LookingAtResult containing block info and distance
     */
    public static LookingAtResult getLookingAt(World world, Entity entity) {
        return getLookingAt(world, entity, 5.0);
    }
}
