package org.hytaledevlib.lib;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

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
     * Send a fluid update packet to all clients who have the chunk loaded.
     * Uses the same pattern as sendBlockUpdateToClients.
     */
    private static void sendFluidUpdateToClients(World world, int x, int y, int z, int fluidId, byte level) {
        try {
            // Try to create and send a fluid update packet
            // The exact packet class/constructor may vary, so we catch any errors
            Object packet = createFluidPacket(x, y, z, fluidId, level);
            if (packet instanceof com.hypixel.hytale.protocol.ToClientPacket) {
                System.out.println("[BlockHelper] Sending fluid update packet for fluid ID " + fluidId + " at (" + x + ", " + y + ", " + z + ")");
                world.getNotificationHandler().sendPacketIfChunkLoaded((com.hypixel.hytale.protocol.ToClientPacket) packet, x, z);
                System.out.println("[BlockHelper] Packet sent successfully");
            } else {
                System.out.println("[BlockHelper] WARNING: Could not create fluid packet (packet was null or not a Packet instance)");
            }
        } catch (Exception e) {
            System.out.println("[BlockHelper] ERROR sending fluid update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Attempt to create a fluid update packet using reflection to handle API changes.
     */
    private static Object createFluidPacket(int x, int y, int z, int fluidId, byte level) {
        // Preferred path: a single-cell fluid update packet.
        try {
            return new com.hypixel.hytale.protocol.packets.world.ServerSetFluid(x, y, z, fluidId, level);
        } catch (Throwable ignored) {
            // Fallback below for environments where ServerSetFluid shape differs.
        }

        try {
            // Try SetFluids packet (bulk update with single change)
            Class<?> setFluidsClass = Class.forName("com.hypixel.hytale.protocol.packets.world.SetFluids");
            
            int chunkX = ChunkUtil.chunkCoordinate(x);
            int chunkY = ChunkUtil.chunkCoordinate(y);
            int chunkZ = ChunkUtil.chunkCoordinate(z);
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = y & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            
            int positionIndex = ChunkUtil.indexBlock(localX, localY, localZ);
            
            // Use the actual constructor: SetFluids(int chunkX, int chunkY, int chunkZ, byte[] data)
            // The byte array contains the fluid data encoded
            try {
                // Create a byte array to hold the fluid data
                // Format appears to be: position indices + fluid ID + level
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
                
                // Write the number of changes
                dos.writeInt(1); // 1 change
                
                // Write the position index
                dos.writeInt(positionIndex);
                
                // Write fluid ID
                dos.writeInt(fluidId);
                
                // Write fluid level
                dos.writeByte(level);
                
                byte[] data = baos.toByteArray();
                
                System.out.println("[BlockHelper] Falling back to SetFluids packet with byte array (length: " + data.length + ")");
                
                return setFluidsClass.getConstructor(int.class, int.class, int.class, byte[].class)
                    .newInstance(chunkX, chunkY, chunkZ, data);
                    
            } catch (NoSuchMethodException e) {
                System.out.println("[BlockHelper] Constructor (int, int, int, byte[]) not found!");
                return null;
            }
        } catch (ClassNotFoundException e) {
            System.out.println("[BlockHelper] SetFluids fallback class not found");
            return null;
        } catch (Exception e) {
            System.out.println("[BlockHelper] Error creating packet: " + e.getMessage());
            e.printStackTrace();
            return null;
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
    
    // ==================== FLUID DETECTION METHODS ====================
    
    /**
     * Get the fluid ID at a specific position.
     * Fluids are stored separately from blocks in Hytale's chunk system.
     * 
     * @param world The world
     * @param position The position to check
     * @return The fluid ID, or 0 if no fluid present
     */
    public static int getFluidId(World world, Vector3d position) {
        return getFluidId(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the fluid ID at specific coordinates.
     * Fluids use a separate FluidSection component system.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The fluid ID, or 0 if no fluid present
     */
    public static int getFluidId(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 320) {
            return 0;
        }
        
        try {
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            ChunkStore chunkStore = world.getChunkStore();
            
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkPos);
            if (chunkRef == null || !chunkRef.isValid()) {
                return 0;
            }
            
            Store<ChunkStore> store = chunkRef.getStore();
            ChunkColumn chunkColumn = store.getComponent(chunkRef, ChunkColumn.getComponentType());
            if (chunkColumn == null) {
                return 0;
            }
            
            Ref<ChunkStore> sectionRef = chunkColumn.getSection(ChunkUtil.chunkCoordinate(y));
            if (sectionRef == null || !sectionRef.isValid()) {
                return 0;
            }
            
            FluidSection fluidSection = store.getComponent(sectionRef, FluidSection.getComponentType());
            if (fluidSection == null) {
                return 0;
            }
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = y & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            return fluidSection.getFluidId(localX, localY, localZ);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get the fluid level at a specific position.
     * Fluid levels range from 0-255, where higher values indicate fuller fluid blocks.
     * 
     * @param world The world
     * @param position The position to check
     * @return The fluid level (0-255), or 0 if no fluid present
     */
    public static byte getFluidLevel(World world, Vector3d position) {
        return getFluidLevel(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the fluid level at specific coordinates.
     * Fluid levels range from 0-255, where higher values indicate fuller fluid blocks.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The fluid level (0-255), or 0 if no fluid present
     */
    public static byte getFluidLevel(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 320) {
            return 0;
        }
        
        try {
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            ChunkStore chunkStore = world.getChunkStore();
            
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkPos);
            if (chunkRef == null || !chunkRef.isValid()) {
                return 0;
            }
            
            Store<ChunkStore> store = chunkRef.getStore();
            ChunkColumn chunkColumn = store.getComponent(chunkRef, ChunkColumn.getComponentType());
            if (chunkColumn == null) {
                return 0;
            }
            
            Ref<ChunkStore> sectionRef = chunkColumn.getSection(ChunkUtil.chunkCoordinate(y));
            if (sectionRef == null || !sectionRef.isValid()) {
                return 0;
            }
            
            FluidSection fluidSection = store.getComponent(sectionRef, FluidSection.getComponentType());
            if (fluidSection == null) {
                return 0;
            }
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = y & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            return fluidSection.getFluidLevel(localX, localY, localZ);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check if a position contains any fluid.
     * 
     * @param world The world
     * @param position The position to check
     * @return true if any fluid is present, false otherwise
     */
    public static boolean hasFluid(World world, Vector3d position) {
        return getFluidId(world, position) != 0;
    }
    
    /**
     * Check if a position contains any fluid.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if any fluid is present, false otherwise
     */
    public static boolean hasFluid(World world, int x, int y, int z) {
        return getFluidId(world, x, y, z) != 0;
    }
    
    /**
     * Get the Fluid asset at a specific position.
     * 
     * @param world The world
     * @param position The position to check
     * @return The Fluid object, or null if no fluid present
     */
    public static Fluid getFluid(World world, Vector3d position) {
        return getFluid(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the Fluid asset at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The Fluid object, or null if no fluid present
     */
    public static Fluid getFluid(World world, int x, int y, int z) {
        int fluidId = getFluidId(world, x, y, z);
        if (fluidId == 0) {
            return null;
        }
        
        try {
            // Use Fluid.getAssetMap(), NOT BlockType.getAssetMap()!
            return Fluid.getAssetMap().getAsset(fluidId);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the name of the fluid at a position.
     * 
     * @param world The world
     * @param position The position to check
     * @return The fluid name/ID string, or null if no fluid present
     */
    public static String getFluidName(World world, Vector3d position) {
        return getFluidName(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Get the name of the fluid at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return The fluid name/ID string, or null if no fluid present
     */
    public static String getFluidName(World world, int x, int y, int z) {
        Fluid fluid = getFluid(world, x, y, z);
        return fluid != null ? fluid.getId() : null;
    }
    
    /**
     * Check if a position contains water.
     * Checks if the fluid name contains "water" (case-insensitive).
     * 
     * @param world The world
     * @param position The position to check
     * @return true if water is present, false otherwise
     */
    public static boolean isWater(World world, Vector3d position) {
        return isWater(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Check if a position contains water.
     * Checks if the fluid name contains "water" (case-insensitive).
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if water is present, false otherwise
     */
    public static boolean isWater(World world, int x, int y, int z) {
        Fluid fluid = getFluid(world, x, y, z);
        if (fluid == null) {
            return false;
        }
        
        String fluidName = fluid.getId().toLowerCase();
        return fluidName.contains("water");
    }
    
    /**
     * Check if a position contains lava.
     * Checks if the fluid name contains "lava" (case-insensitive).
     * 
     * @param world The world
     * @param position The position to check
     * @return true if lava is present, false otherwise
     */
    public static boolean isLava(World world, Vector3d position) {
        return isLava(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Check if a position contains lava.
     * Checks if the fluid name contains "lava" (case-insensitive).
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if lava is present, false otherwise
     */
    public static boolean isLava(World world, int x, int y, int z) {
        Fluid fluid = getFluid(world, x, y, z);
        if (fluid == null) {
            return false;
        }
        
        String fluidName = fluid.getId().toLowerCase();
        return fluidName.contains("lava");
    }
    
    /**
     * Find the surface Y coordinate of a fluid at X,Z coordinates.
     * Searches upward from startY to find the top of the fluid.
     * 
     * @param world The world
     * @param x X coordinate
     * @param z Z coordinate
     * @param startY Starting Y coordinate to search from
     * @return The Y coordinate of the fluid surface, or -1 if no fluid found
     */
    public static int findFluidSurface(World world, int x, int z, int startY) {
        if (world == null || startY < 0 || startY >= 320) {
            return -1;
        }
        
        // Check if current position has fluid
        if (getFluidId(world, x, startY, z) != 0) {
            // Search upward for the top
            int y = startY;
            while (y < 319 && getFluidId(world, x, y + 1, z) != 0) {
                y++;
            }
            return y;
        }
        
        return -1;
    }
    
    /**
     * Find all fluid blocks in a region.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @return List of positions containing fluids
     */
    public static List<FluidPosition> findFluidsInRegion(World world, Vector3d pos1, Vector3d pos2) {
        List<FluidPosition> fluids = new ArrayList<>();
        
        if (world == null || pos1 == null || pos2 == null) {
            return fluids;
        }
        
        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());
        
        minY = Math.max(0, minY);
        maxY = Math.min(319, maxY);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int fluidId = getFluidId(world, x, y, z);
                    if (fluidId != 0) {
                        byte fluidLevel = getFluidLevel(world, x, y, z);
                        fluids.add(new FluidPosition(x, y, z, fluidId, fluidLevel));
                    }
                }
            }
        }
        
        return fluids;
    }
    
    /**
     * Count how many fluid blocks exist in a region.
     * 
     * @param world The world
     * @param pos1 First corner of the region
     * @param pos2 Second corner of the region
     * @return The number of fluid blocks found
     */
    public static int countFluidsInRegion(World world, Vector3d pos1, Vector3d pos2) {
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
                    if (getFluidId(world, x, y, z) != 0) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    // ==================== FLUID SETTING METHODS ====================
    
    /**
     * Set fluid at a specific position.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param fluidId The fluid ID (0 to remove fluid)
     * @param level Fluid level (0-15, where 15 is full block)
     * @return true if the fluid was changed, false otherwise
     */
    public static boolean setFluid(World world, int x, int y, int z, int fluidId, byte level) {
        if (world == null || y < 0 || y >= 320) {
            return false;
        }
        
        try {
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            ChunkStore chunkStore = world.getChunkStore();
            
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkPos);
            if (chunkRef == null || !chunkRef.isValid()) {
                return false;
            }
            
            Store<ChunkStore> store = chunkRef.getStore();
            ChunkColumn chunkColumn = store.getComponent(chunkRef, ChunkColumn.getComponentType());
            if (chunkColumn == null) {
                return false;
            }
            
            Ref<ChunkStore> sectionRef = chunkColumn.getSection(ChunkUtil.chunkCoordinate(y));
            if (sectionRef == null || !sectionRef.isValid()) {
                return false;
            }
            
            FluidSection fluidSection = store.ensureAndGetComponent(sectionRef, FluidSection.getComponentType());
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localY = y & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            
            boolean changed = fluidSection.setFluid(localX, localY, localZ, fluidId, level);
            
            if (changed) {
                WorldChunk worldChunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
                if (worldChunk != null) {
                    worldChunk.markNeedsSaving();
                    worldChunk.setTicking(x, y, z, true);
                }
                
                // Send fluid update to clients (same pattern as setBlock)
                sendFluidUpdateToClients(world, x, y, z, fluidId, level);
            }
            
            return changed;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set fluid at a position using a Fluid object.
     * 
     * @param world The world
     * @param position The position
     * @param fluid The Fluid object (null to remove)
     * @param level Fluid level (0-15)
     * @return true if changed
     */
    public static boolean setFluid(World world, Vector3d position, Fluid fluid, byte level) {
        return setFluid(world, (int) position.getX(), (int) position.getY(), (int) position.getZ(), fluid, level);
    }
    
    /**
     * Set fluid at specific coordinates using a Fluid object.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param fluid The Fluid object (null to remove)
     * @param level Fluid level (0-15)
     * @return true if changed
     */
    public static boolean setFluid(World world, int x, int y, int z, Fluid fluid, byte level) {
        if (fluid == null) {
            return setFluid(world, x, y, z, 0, (byte) 0);
        }
        
        int fluidId = Fluid.getAssetMap().getIndex(fluid.getId());
        return setFluid(world, x, y, z, fluidId, level);
    }
    
    /**
     * Place a full water block at a position.
     * 
     * @param world The world
     * @param position The position
     * @return true if water was placed
     */
    public static boolean placeWater(World world, Vector3d position) {
        return placeWater(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Place a full water block at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if water was placed
     */
    public static boolean placeWater(World world, int x, int y, int z) {
        if (world == null || y < 0 || y >= 320) {
            return false;
        }
        
        try {
            // Natural water = Empty block (air, ID 0) + Fluid data
            
            // Get water fluid first
            Fluid water = Fluid.getAssetMap().getAsset("Water_Source");
            if (water == null) {
                water = Fluid.getAssetMap().getAsset("water");
            }
            
            if (water == null) {
                System.out.println("[BlockHelper] ERROR: Could not find water fluid!");
                return false;
            }
            
            int waterFluidId = Fluid.getAssetMap().getIndex(water.getId());
            
            // Step 1: Set block to AIR on server (WITHOUT sending packet yet)
            long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
            ChunkStore chunkStore = world.getChunkStore();
            BlockChunk blockChunk = chunkStore.getChunkComponent(chunkPos, BlockChunk.getComponentType());
            
            if (blockChunk == null) {
                System.out.println("[BlockHelper] Failed to get block chunk");
                return false;
            }
            
            int localX = x & ChunkUtil.SIZE_MASK;
            int localZ = z & ChunkUtil.SIZE_MASK;
            
            // Set air block on server
            boolean blockSet = blockChunk.setBlock(localX, y, localZ, 0, 0, 0);
            if (!blockSet) {
                System.out.println("[BlockHelper] Failed to set air block for water");
                return false;
            }
            
            // Step 2: Set fluid data on server (WITHOUT sending packet yet)
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkPos);
            if (chunkRef == null || !chunkRef.isValid()) {
                System.out.println("[BlockHelper] Failed to get chunk reference");
                return false;
            }
            
            Store<ChunkStore> store = chunkRef.getStore();
            ChunkColumn chunkColumn = store.getComponent(chunkRef, ChunkColumn.getComponentType());
            if (chunkColumn == null) {
                System.out.println("[BlockHelper] Failed to get chunk column");
                return false;
            }
            
            Ref<ChunkStore> sectionRef = chunkColumn.getSection(ChunkUtil.chunkCoordinate(y));
            if (sectionRef == null || !sectionRef.isValid()) {
                System.out.println("[BlockHelper] Failed to get section reference");
                return false;
            }
            
            FluidSection fluidSection = store.ensureAndGetComponent(sectionRef, FluidSection.getComponentType());
            
            int localY = y & ChunkUtil.SIZE_MASK;
            
            // Set fluid on server - use level 1 for water source (not 15)
            boolean fluidSet = fluidSection.setFluid(localX, localY, localZ, waterFluidId, (byte) 1);
            
            if (!fluidSet) {
                System.out.println("[BlockHelper] Failed to set fluid data for water");
                return false;
            }
            
            // Mark chunk for saving and enable ticking
            WorldChunk worldChunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
            if (worldChunk != null) {
                worldChunk.markNeedsSaving();
                worldChunk.setTicking(x, y, z, true);
            }
            
            System.out.println("[BlockHelper] Server-side water placed (empty block + fluid ID " + waterFluidId + ")");
            
            // Step 3: Send packets to clients - FLUID FIRST, then BLOCK
            // Use level 1 for water source (matches natural water sources)
            sendFluidUpdateToClients(world, x, y, z, waterFluidId, (byte) 1);
            System.out.println("[BlockHelper] Sent fluid update to clients");
            
            sendBlockUpdateToClients(world, x, y, z, 0, (short) 0, (byte) 0);
            System.out.println("[BlockHelper] Sent block update (air) to clients");
            
            System.out.println("[BlockHelper] Successfully placed water with client updates");
            return true;
            
        } catch (Exception e) {
            System.out.println("[BlockHelper] Error placing water: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Place a full lava block at a position.
     * 
     * @param world The world
     * @param position The position
     * @return true if lava was placed
     */
    public static boolean placeLava(World world, Vector3d position) {
        return placeLava(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Place a full lava block at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if lava was placed
     */
    public static boolean placeLava(World world, int x, int y, int z) {
        // Use Lava_Source for source blocks (ID: 6)
        Fluid lava = Fluid.getAssetMap().getAsset("Lava_Source");
        if (lava == null) {
            // Fallback to flowing lava (ID: 11)
            lava = Fluid.getAssetMap().getAsset("lava");
        }
        
        if (lava == null) {
            System.out.println("[BlockHelper] ERROR: Could not find lava fluid in asset map!");
            return false;
        }
        
        return setFluid(world, x, y, z, lava, (byte) 15);
    }
    
    /**
     * Remove fluid at a position.
     * 
     * @param world The world
     * @param position The position
     * @return true if fluid was removed
     */
    public static boolean removeFluid(World world, Vector3d position) {
        return removeFluid(world, (int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
    
    /**
     * Remove fluid at specific coordinates.
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if fluid was removed
     */
    public static boolean removeFluid(World world, int x, int y, int z) {
        return setFluid(world, x, y, z, 0, (byte) 0);
    }
    
    /**
     * Class to hold fluid position and data information.
     */
    public static class FluidPosition {
        public final int x;
        public final int y;
        public final int z;
        public final int fluidId;
        public final byte fluidLevel;
        
        public FluidPosition(int x, int y, int z, int fluidId, byte fluidLevel) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.fluidId = fluidId;
            this.fluidLevel = fluidLevel;
        }
        
        public Vector3i toVector3i() {
            return new Vector3i(x, y, z);
        }
        
        public Vector3d toVector3d() {
            return new Vector3d(x, y, z);
        }
        
        @Override
        public String toString() {
            return String.format("FluidPosition{x=%d, y=%d, z=%d, fluidId=%d, fluidLevel=%d}", 
                x, y, z, fluidId, fluidLevel);
        }
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
