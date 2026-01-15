package org.hytaledevlib.lib;

import java.util.Map;

/**
 * Block name to ID mapping system for Hytale.
 * Allows working with block names instead of numeric IDs, similar to Minecraft modding.
 * 
 * <p>This class delegates to the hardcoded {@link BlockIds} class for fast, efficient lookups.
 * 
 * <p>Usage:
 * <pre>
 * // Get block ID by name
 * int stoneId = BlockMapping.getBlockId("Rock_Stone");
 * 
 * // Get block name by ID
 * String blockName = BlockMapping.getBlockName(684);
 * 
 * // Check if block exists
 * if (BlockMapping.hasBlock("Soil_Grass")) {
 *     // Block exists
 * }
 * </pre>
 */
public class BlockMapping {
    
    /**
     * Get the block ID for a given block name.
     * 
     * @param blockName The block name (e.g., "Rock_Stone", "Soil_Grass")
     * @return The block ID, or -1 if not found
     */
    public static int getBlockId(String blockName) {
        return BlockIds.getId(blockName);
    }
    
    /**
     * Get the block name for a given block ID.
     * 
     * @param blockId The block ID
     * @return The block name, or null if not found
     */
    public static String getBlockName(int blockId) {
        return BlockIds.getName(blockId);
    }
    
    /**
     * Check if a block with the given name exists.
     * 
     * @param blockName The block name to check
     * @return true if the block exists, false otherwise
     */
    public static boolean hasBlock(String blockName) {
        return BlockIds.hasBlock(blockName);
    }
    
    /**
     * Check if a block with the given ID exists.
     * 
     * @param blockId The block ID to check
     * @return true if the block exists, false otherwise
     */
    public static boolean hasBlock(int blockId) {
        return BlockIds.hasBlock(blockId);
    }
    
    /**
     * Get all registered block names.
     * 
     * @return A map of all block names to their IDs
     */
    public static Map<String, Integer> getAllBlocks() {
        return BlockIds.getAllBlocks();
    }
    
    /**
     * Get the total number of registered blocks.
     * 
     * @return The number of blocks
     */
    public static int getBlockCount() {
        return BlockIds.getBlockCount();
    }
}
