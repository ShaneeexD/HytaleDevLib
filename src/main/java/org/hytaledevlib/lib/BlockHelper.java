package org.hytaledevlib.lib;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for block manipulation and queries in Hytale.
 * Provides utilities for getting, setting, and searching for blocks in the world.
 *
 * <p><b>IMPORTANT:</b> Hytale chunks are 32x32 blocks. This helper uses
 * {@link com.hypixel.hytale.math.util.ChunkUtil} to compute chunk indexes and local coordinates.
 * If a chunk is not present in the world's {@code ChunkStore}, reads will return 0 and writes
 * will fail (return false).</p>
 */
public class BlockHelper {
    
    private static volatile java.lang.reflect.Method CACHED_META_GETTER;
    private static volatile java.lang.reflect.Method CACHED_DEFAULT_META_GETTER;
    
    /**
     * Get the block name (ID string) from a numeric block ID.
     * 
     * @param blockId The numeric block ID
     * @return The block name/ID string (e.g., "hytale:blocks/stone"), or null if not found
     */
    public static String getBlockName(int blockId) {
        try {
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            return blockType != null ? blockType.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static int getDefaultBlockMeta(int blockId) {
        try {
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null) {
                return 0;
            }

            java.lang.reflect.Method getter = CACHED_DEFAULT_META_GETTER;
            if (getter == null) {
                getter = findDefaultMetaGetter(blockType.getClass());
                CACHED_DEFAULT_META_GETTER = getter;
            }

            if (getter == null) {
                return 0;
            }

            Object result = getter.invoke(blockType);
            if (result instanceof Integer) {
                return (Integer) result;
            }

            if (result != null) {
                for (String methodName : new String[] {"getStateId", "getId", "getIndex"}) {
                    try {
                        java.lang.reflect.Method m = result.getClass().getMethod(methodName);
                        Object r = m.invoke(result);
                        if (r instanceof Integer) {
                            return (Integer) r;
                        }
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get the block name at a specific position.
     * 
     * @param world The world
     * @param position The position to check
     * @return The block name/ID string, or null if not found or chunk not loaded
     */
    public static String getBlockName(World world, Vector3d position) {
        return getBlockName(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the block name at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The block name/ID string, or null if not found or chunk not loaded
     */
    public static String getBlockName(World world, int x, int y, int z) {
        int blockId = getBlock(world, x, y, z);
        return blockId != 0 ? getBlockName(blockId) : null;
    }
    
    /**
     * Get the block ID for a given block name using the game's native BlockType asset map.
     * This allows working with block names instead of numeric IDs.
     * 
     * @param blockName The block name (e.g., "Rock_Stone", "Soil_Grass")
     * @return The block ID, or -1 if not found
     */
    public static int getBlockId(String blockName) {
        try {
            int index = BlockType.getAssetMap().getIndex(blockName);
            return index != Integer.MIN_VALUE ? index : -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Get a block by name at a specific position.
     * 
     * @param world The world
     * @param position The position to check
     * @return The block name, or null if not found or chunk not loaded
     */
    public static String getBlockByName(World world, Vector3d position) {
        return getBlockName(world, position);
    }
    
    /**
     * Set a block by name at a specific position.
     * 
     * @param world The world
     * @param position The position to set
     * @param blockName The block name (e.g., "Rock_Stone", "Soil_Grass")
     * @return true if successful, false otherwise
     */
    public static boolean setBlockByName(World world, Vector3d position, String blockName) {
        int blockId = getBlockId(blockName);
        if (blockId == -1) {
            return false; // Block name not found
        }
        return setBlock(world, position, blockId);
    }
    
    /**
     * Set a block by name at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param blockName The block name (e.g., "Rock_Stone", "Soil_Grass")
     * @return true if successful, false otherwise
     */
    public static boolean setBlockByName(World world, int x, int y, int z, String blockName) {
        int blockId = getBlockId(blockName);
        if (blockId == -1) {
            return false; // Block name not found
        }
        return setBlock(world, x, y, z, blockId);
    }
    
    /**
     * Get the block ID at a specific position.
     * 
     * @param world The world
     * @param position The position to check
     * @return The block ID at that position, or 0 (air) if the chunk is not loaded
     */
    public static int getBlock(World world, Vector3d position) {
        return getBlock(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the block ID at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The block ID at those coordinates, or 0 (air) if the chunk is not loaded
     */
    public static int getBlock(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 320) {
            return 0; // Air or out of bounds
        }
        
        try {
            // Calculate chunk index from block coordinates (Hytale chunks are 32x32)
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            
            // Use ChunkStore to get BlockChunk component directly
            com.hypixel.hytale.server.core.universe.world.storage.ChunkStore chunkStore = world.getChunkStore();
            BlockChunk blockChunk = chunkStore.getChunkComponent(chunkPos, BlockChunk.getComponentType());
            
            if (blockChunk == null) {
                return 0; // Chunk not accessible
            }
            
            // Get block within chunk (local coordinates)
            int localX = x & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            return blockChunk.getBlock(localX, y, localZ);
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Set a block at a specific position.
     * 
     * @param world The world
     * @param position The position to set the block
     * @param blockId The block ID to set
     * @return true if the block was set successfully, false otherwise
     */
    public static boolean setBlock(World world, Vector3d position, int blockId) {
        return setBlock(world, (int) position.getX(), (int) position.getY(), (int) position.getZ(), blockId);
    }
    
    /**
     * Set a block at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param blockId The block ID to set
     * @return true if the block was set successfully, false otherwise
     */
    public static boolean setBlock(World world, int x, int y, int z, int blockId) {
        // Use rotation=0 (no rotation) and filler=0 (no filler)
        return setBlock(world, x, y, z, blockId, 0, 0);
    }

    /**
     * Set a block at specific coordinates with rotation and filler.
     * This method also sends a ServerSetBlock packet to notify all clients.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param blockId The block ID to set
     * @param rotation Block rotation (0-23, typically 0 for no rotation)
     * @param filler Filler block data (typically 0)
     * @return true if the block was set successfully, false otherwise
     */
    public static boolean setBlock(World world, int x, int y, int z, int blockId, int rotation, int filler) {
        if (world == null || y < 0 || y >= 320) {
            return false;
        }
        
        try {
            // Get BlockChunk for this position
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            com.hypixel.hytale.server.core.universe.world.storage.ChunkStore chunkStore = world.getChunkStore();
            BlockChunk blockChunk = chunkStore.getChunkComponent(chunkPos, BlockChunk.getComponentType());
            
            if (blockChunk == null) {
                return false;
            }
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            
            // Set the block in the chunk (this invalidates the section cache)
            boolean success = blockChunk.setBlock(localX, y, localZ, blockId, rotation, filler);
            
            if (success) {
                // Send ServerSetBlock packet to all players who have this chunk loaded
                sendBlockUpdateToClients(world, x, y, z, blockId, (short) filler, (byte) rotation);
            }
            
            return success;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Send a block update packet to all clients who have the chunk loaded.
     */
    private static void sendBlockUpdateToClients(World world, int x, int y, int z, int blockId, short filler, byte rotation) {
        try {
            // Create the ServerSetBlock packet
            com.hypixel.hytale.protocol.packets.world.ServerSetBlock packet = 
                new com.hypixel.hytale.protocol.packets.world.ServerSetBlock(x, y, z, blockId, filler, rotation);
            
            // Use WorldNotificationHandler to send to all players with this chunk loaded
            world.getNotificationHandler().sendPacketIfChunkLoaded(packet, x, z);
        } catch (Exception e) {
            // Silently fail - block was set but notification failed
        }
    }
    
    /**
     * Get the block metadata at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The block metadata, or 0 if not accessible
     */
    public static int getBlockMeta(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 320) {
            return 0;
        }
        
        try {
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            com.hypixel.hytale.server.core.universe.world.storage.ChunkStore chunkStore = world.getChunkStore();
            BlockChunk blockChunk = chunkStore.getChunkComponent(chunkPos, BlockChunk.getComponentType());
            if (blockChunk == null) {
                return 0;
            }
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            
            java.lang.reflect.Method metaGetter = CACHED_META_GETTER;
            if (metaGetter == null) {
                metaGetter = findMetaGetter(blockChunk.getClass());
                CACHED_META_GETTER = metaGetter;
            }
            if (metaGetter == null) {
                return 0;
            }
            
            Object result = metaGetter.invoke(blockChunk, localX, y, localZ);
            if (result instanceof Integer) {
                return (Integer) result;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static java.lang.reflect.Method findMetaGetter(Class<?> blockChunkClass) {
        try {
            String[] candidates = new String[] {
                "getMeta", "getBlockMeta", "getBlockMetadata", "getMetadata", "getBlockData", "getData"
            };
            for (String name : candidates) {
                try {
                    return blockChunkClass.getMethod(name, int.class, int.class, int.class);
                } catch (NoSuchMethodException ignored) {
                }
            }
            
            for (java.lang.reflect.Method m : blockChunkClass.getMethods()) {
                if (m.getParameterCount() != 3) {
                    continue;
                }
                if (m.getReturnType() != int.class) {
                    continue;
                }
                Class<?>[] p = m.getParameterTypes();
                if (p[0] == int.class && p[1] == int.class && p[2] == int.class) {
                    String n = m.getName().toLowerCase();
                    if (n.contains("meta") || n.contains("data")) {
                        return m;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static java.lang.reflect.Method findDefaultMetaGetter(Class<?> blockTypeClass) {
        try {
            String[] candidates = new String[] {
                "getDefaultMeta",
                "getDefaultMetadata",
                "getDefaultStateId",
                "getDefaultBlockStateId",
                "getDefaultState",
                "getDefaultBlockState"
            };
            for (String name : candidates) {
                try {
                    return blockTypeClass.getMethod(name);
                } catch (NoSuchMethodException ignored) {
                }
            }

            for (java.lang.reflect.Method m : blockTypeClass.getMethods()) {
                if (m.getParameterCount() != 0) {
                    continue;
                }
                String n = m.getName().toLowerCase();
                if (n.contains("default") && (n.contains("meta") || n.contains("state"))) {
                    return m;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Boolean trySetBlockViaWorld(World world, int x, int y, int z, int blockId, int metadata, int flags) {
        try {
            Class<?> wc = world.getClass();
            
            try {
                java.lang.reflect.Method m = wc.getMethod("setBlock", int.class, int.class, int.class, int.class, int.class, int.class);
                Object r = m.invoke(world, x, y, z, blockId, metadata, flags);
                if (r instanceof Boolean) {
                    return (Boolean) r;
                }
                return true;
            } catch (NoSuchMethodException ignored) {
            }
            
            try {
                java.lang.reflect.Method m = wc.getMethod("setBlock", int.class, int.class, int.class, int.class);
                Object r = m.invoke(world, x, y, z, blockId);
                if (r instanceof Boolean) {
                    return (Boolean) r;
                }
                return true;
            } catch (NoSuchMethodException ignored) {
            }
            
            try {
                java.lang.reflect.Method m = wc.getMethod("setBlock", int.class, int.class, int.class, int.class, int.class);
                Object r = m.invoke(world, x, y, z, blockId, flags);
                if (r instanceof Boolean) {
                    return (Boolean) r;
                }
                return true;
            } catch (NoSuchMethodException ignored) {
            }
            
        } catch (Exception ignored) {
        }
        
        return null;
    }
    
    /**
     * Check if a block at the given position is air (block ID 0).
     * 
     * @param world The world
     * @param position The position to check
     * @return true if the block is air, false otherwise
     */
    public static boolean isAir(World world, Vector3d position) {
        return getBlock(world, position) == 0;
    }
    
    /**
     * Check if a block at the given coordinates is air (block ID 0).
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the block is air, false otherwise
     */
    public static boolean isAir(World world, int x, int y, int z) {
        return getBlock(world, x, y, z) == 0;
    }
    
    /**
     * Replace all blocks of one type with another in a rectangular region (by name).
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param oldBlockName The block name to replace (e.g., "Soil_Dirt")
     * @param newBlockName The block name to replace with (e.g., "Soil_Grass")
     * @return The number of blocks replaced, or -1 if block names are invalid
     */
    public static int replaceBlocksInRegionByName(World world, Vector3d pos1, Vector3d pos2, String oldBlockName, String newBlockName) {
        int oldBlockId = getBlockId(oldBlockName);
        int newBlockId = getBlockId(newBlockName);
        
        if (oldBlockId == -1 || newBlockId == -1) {
            return -1; // Invalid block names
        }
        
        return replaceBlocksInRegion(world, pos1, pos2, oldBlockId, newBlockId);
    }
    
    /**
     * Fill a rectangular region with a specific block type (by name).
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param blockName The block name to fill with (e.g., "Rock_Stone")
     * @return The number of blocks set, or -1 if block name is invalid
     */
    public static int fillRegionByName(World world, Vector3d pos1, Vector3d pos2, String blockName) {
        int blockId = getBlockId(blockName);
        
        if (blockId == -1) {
            return -1; // Invalid block name
        }
        
        return fillRegion(world, pos1, pos2, blockId);
    }
    
    /**
     * Find all positions of a specific block type within a radius (by name).
     * 
     * @param world The world
     * @param center The center position
     * @param radius The search radius
     * @param blockName The block name to search for (e.g., "Ore_Diamond")
     * @return A list of positions where the block was found, or empty list if block name is invalid
     */
    public static List<Vector3i> findNearbyBlocksByName(World world, Vector3d center, int radius, String blockName) {
        int blockId = getBlockId(blockName);
        
        if (blockId == -1) {
            return new ArrayList<>(); // Invalid block name
        }
        
        return findNearbyBlocks(world, center, radius, blockId);
    }
    
    /**
     * Replace all blocks of one type with another in a rectangular region.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param oldBlockId The block ID to replace
     * @param newBlockId The block ID to replace with
     * @return The number of blocks replaced
     */
    public static int replaceBlocksInRegion(World world, Vector3d pos1, Vector3d pos2, int oldBlockId, int newBlockId) {
        if (world == null || pos1 == null || pos2 == null) {
            return 0;
        }
        
        // Get min and max coordinates
        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());
        
        // Clamp Y to world bounds
        minY = Math.max(0, minY);
        maxY = Math.min(319, maxY);
        
        int replacedCount = 0;
        
        // Iterate through all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (getBlock(world, x, y, z) == oldBlockId) {
                        if (setBlock(world, x, y, z, newBlockId)) {
                            replacedCount++;
                        }
                    }
                }
            }
        }
        
        return replacedCount;
    }
    
    /**
     * Fill a rectangular region with a specific block type.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param blockId The block ID to fill with
     * @return The number of blocks set
     */
    public static int fillRegion(World world, Vector3d pos1, Vector3d pos2, int blockId) {
        if (world == null || pos1 == null || pos2 == null) {
            return 0;
        }
        
        // Get min and max coordinates
        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());
        
        // Clamp Y to world bounds
        minY = Math.max(0, minY);
        maxY = Math.min(319, maxY);
        
        int setCount = 0;
        
        // Fill all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (setBlock(world, x, y, z, blockId)) {
                        setCount++;
                    }
                }
            }
        }
        
        return setCount;
    }
    
    /**
     * Find all positions of a specific block type within a radius.
     * 
     * @param world The world
     * @param center The center position to search from
     * @param radius The search radius
     * @param blockId The block ID to search for
     * @return List of positions where the block was found
     */
    public static List<Vector3i> findNearbyBlocks(World world, Vector3d center, int radius, int blockId) {
        List<Vector3i> foundBlocks = new ArrayList<>();
        
        if (world == null || center == null || radius <= 0) {
            return foundBlocks;
        }
        
        int centerX = (int) center.getX();
        int centerY = (int) center.getY();
        int centerZ = (int) center.getZ();
        
        // Search in a cube around the center
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = Math.max(0, centerY - radius); y <= Math.min(319, centerY + radius); y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    // Check if within spherical radius
                    double distSq = Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) + Math.pow(z - centerZ, 2);
                    if (distSq <= radius * radius) {
                        if (getBlock(world, x, y, z) == blockId) {
                            foundBlocks.add(new Vector3i(x, y, z));
                        }
                    }
                }
            }
        }
        
        return foundBlocks;
    }
    
    /**
     * Get all block positions within a rectangular region.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @return List of all block positions in the region with their block IDs
     */
    public static List<BlockPosition> getBlocksInRegion(World world, Vector3d pos1, Vector3d pos2) {
        List<BlockPosition> blocks = new ArrayList<>();
        
        if (world == null || pos1 == null || pos2 == null) {
            return blocks;
        }
        
        // Get min and max coordinates
        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());
        
        // Clamp Y to world bounds
        minY = Math.max(0, minY);
        maxY = Math.min(319, maxY);
        
        // Get all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int blockId = getBlock(world, x, y, z);
                    blocks.add(new BlockPosition(x, y, z, blockId));
                }
            }
        }
        
        return blocks;
    }
    
    /**
     * Count how many blocks of a specific type exist in a region.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @param blockId The block ID to count
     * @return The number of matching blocks found
     */
    public static int countBlocksInRegion(World world, Vector3d pos1, Vector3d pos2, int blockId) {
        if (world == null || pos1 == null || pos2 == null) {
            return 0;
        }
        
        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());
        
        minY = Math.max(0, minY);
        maxY = Math.min(319, maxY);
        
        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (getBlock(world, x, y, z) == blockId) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Simple class to hold block position and ID information.
     */
    public static class BlockPosition {
        public final int x;
        public final int y;
        public final int z;
        public final int blockId;
        
        public BlockPosition(int x, int y, int z, int blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
        }
        
        public Vector3i toVector3i() {
            return new Vector3i(x, y, z);
        }
        
        public Vector3d toVector3d() {
            return new Vector3d(x, y, z);
        }
        
        @Override
        public String toString() {
            return String.format("BlockPosition{x=%d, y=%d, z=%d, blockId=%d}", x, y, z, blockId);
        }
    }
}
