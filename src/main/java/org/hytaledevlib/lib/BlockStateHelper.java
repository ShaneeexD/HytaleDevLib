package org.hytaledevlib.lib;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Helper class for working with block states in Hytale.
 * 
 * Note: BlockState is deprecated in Hytale, but some blocks still use it for
 * storing additional data (like chests, crafting benches, etc.). This helper
 * provides utilities for working with these state objects.
 * 
 * For most blocks, you only need BlockHelper to get/set block types.
 * Use this helper when you need to work with blocks that have additional state data.
 */
public class BlockStateHelper {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    /**
     * Check if a block at the given position has state data.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if the block has state data
     */
    public static boolean hasState(World world, int x, int y, int z) {
        try {
            WorldChunk chunk = WorldHelper.getChunk(world, x >> 5, z >> 5);
            if (chunk == null) return false;
            
            return chunk.getState(x & 31, y, z & 31) != null;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error checking block state: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the block state at a specific position.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return The BlockState object, or null if no state exists
     */
    @Nullable
    public static BlockState getState(World world, int x, int y, int z) {
        try {
            WorldChunk chunk = WorldHelper.getChunk(world, x >> 5, z >> 5);
            if (chunk == null) return null;
            
            return chunk.getState(x & 31, y, z & 31);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting block state: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the block state at a specific position using a Vector3i.
     * 
     * @param world The world
     * @param position The block position
     * @return The BlockState object, or null if no state exists
     */
    @Nullable
    public static BlockState getState(World world, Vector3i position) {
        return getState(world, position.getX(), position.getY(), position.getZ());
    }
    
    /**
     * Set or update the block state at a specific position.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param state The BlockState to set (or null to remove state)
     * @return true if successful
     */
    public static boolean setState(World world, int x, int y, int z, @Nullable BlockState state) {
        try {
            WorldChunk chunk = WorldHelper.getChunk(world, x >> 5, z >> 5);
            if (chunk == null) return false;
            
            chunk.setState(x & 31, y, z & 31, state);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error setting block state: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove the block state at a specific position.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if successful
     */
    public static boolean removeState(World world, int x, int y, int z) {
        return setState(world, x, y, z, null);
    }
    
    /**
     * Get the block state reference for ECS operations.
     * This is useful for advanced operations that need to work with the component system.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return The Ref<ChunkStore> for the block state, or null if no state exists
     */
    @Nullable
    public static Ref<ChunkStore> getStateReference(World world, int x, int y, int z) {
        try {
            WorldChunk chunk = WorldHelper.getChunk(world, x >> 5, z >> 5);
            if (chunk == null) return null;
            
            BlockState state = chunk.getState(x & 31, y, z & 31);
            return state != null ? state.getReference() : null;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting block state reference: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the world position of a block state.
     * 
     * @param state The block state
     * @return The world position as Vector3i, or null if state is invalid
     */
    @Nullable
    public static Vector3i getBlockPosition(BlockState state) {
        try {
            return state.getBlockPosition();
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error getting block position from state: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a block state is of a specific type.
     * Useful for checking if a block is a chest, crafting bench, etc.
     * 
     * @param state The block state
     * @param stateClass The class to check against
     * @return true if the state is an instance of the specified class
     */
    public static boolean isStateType(BlockState state, Class<? extends BlockState> stateClass) {
        return state != null && stateClass.isInstance(state);
    }
    
    /**
     * Ensure a block has state data created for it.
     * This will create the appropriate state object based on the block type's configuration.
     * 
     * @param world The world
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return The created or existing BlockState, or null if the block doesn't support states
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static BlockState ensureState(World world, int x, int y, int z) {
        try {
            WorldChunk chunk = WorldHelper.getChunk(world, x >> 5, z >> 5);
            if (chunk == null) return null;
            
            // Check if state already exists
            BlockState existing = chunk.getState(x & 31, y, z & 31);
            if (existing != null) return existing;
            
            // Try to create state based on block type
            return BlockState.ensureState(chunk, x & 31, y, z & 31);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error ensuring block state: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Mark a block state as needing to be saved.
     * Call this after modifying state data to ensure it persists.
     * 
     * @param state The block state to mark
     * @return true if successful
     */
    public static boolean markNeedsSave(BlockState state) {
        try {
            state.markNeedsSave();
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error marking state for save: " + e.getMessage());
            return false;
        }
    }
}
