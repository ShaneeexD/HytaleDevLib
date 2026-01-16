package org.hytaledevlib.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import org.hytaledevlib.lib.WorldHelper;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Test plugin to verify HytaleDevLib helper utilities.
 * Tests the tick tracking system with logging.
 */
public class TestPlugin extends JavaPlugin {
    
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private World world;
    
    public TestPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.at(Level.INFO).log("HytaleDevLib Test Plugin v" + this.getManifest().getVersion().toString() + " loaded!");
    }
    
    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up HytaleDevLib Test Plugin...");
        
        // Register player join event to capture world reference
        this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
            LOGGER.at(Level.INFO).log("=== Player joined world! ===");
            
            // Capture world from first player join
            if (world == null) {
                try {
                    // Get world directly from event
                    world = event.getWorld();
                    LOGGER.at(Level.INFO).log("World captured: " + world.getName());
                    
                    // Register ECS event helpers (must be done after we have a world)
                    registerEcsEventTests(world);
                    
                    // Test all helpers: Wait 100 ticks for player to fully load
                    LOGGER.at(Level.INFO).log("Testing all helpers: Will run tests in 100 ticks (5 seconds)...");
                    WorldHelper.waitTicks(world, 100, () -> {
                        testInventoryHelper(world);
                        testPlayerHelper(world);
                        testEntityHelper(world);
                    });
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Could not capture world: " + e.getMessage());
                }
            }
        });
        
        // Register new event tests
        registerEventTests();
        
        LOGGER.at(Level.INFO).log("Test plugin setup complete! Waiting for player to join...");
    }
    
    private void testInventoryHelper(World world) {
        LOGGER.at(Level.INFO).log("=== InventoryHelper Tests Starting ===");
        
        try {
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Test 1: Give items
            LOGGER.at(Level.INFO).log("Test 1: Giving 10 Ingredient_Bone_Fragment to player...");
            boolean gaveItems = org.hytaledevlib.lib.InventoryHelper.giveItem(player, "Ingredient_Bone_Fragment", 10);
            LOGGER.at(Level.INFO).log("  Result: " + (gaveItems ? "✓ Success" : "✗ Failed"));
            
            // Test 2: Count items
            int diamondCount = org.hytaledevlib.lib.InventoryHelper.countItem(player, "Ingredient_Bone_Fragment");
            LOGGER.at(Level.INFO).log("Test 2: Player has " + diamondCount + " Ingredient_Bone_Fragments");
            
            // Test 3: Check if has items
            boolean hasDiamonds = org.hytaledevlib.lib.InventoryHelper.hasItem(player, "Ingredient_Bone_Fragment", 5);
            LOGGER.at(Level.INFO).log("Test 3: Has at least 5 Ingredient_Bone_Fragment? " + (hasDiamonds ? "✓ Yes" : "✗ No"));
            
            // Test 4: Get active hotbar item
            com.hypixel.hytale.server.core.inventory.ItemStack activeItem = org.hytaledevlib.lib.InventoryHelper.getActiveHotbarItem(player);
            if (activeItem != null) {
                LOGGER.at(Level.INFO).log("Test 4: Active hotbar item: " + activeItem.getItemId() + " x" + activeItem.getQuantity());
            } else {
                LOGGER.at(Level.INFO).log("Test 4: No active hotbar item (empty slot)");
            }
            
            // Test 5: Get empty slot count
            int emptySlots = org.hytaledevlib.lib.InventoryHelper.getEmptySlotCount(player);
            LOGGER.at(Level.INFO).log("Test 5: Empty inventory slots: " + emptySlots);
            
            // Test 6: Check if inventory is full
            boolean isFull = org.hytaledevlib.lib.InventoryHelper.isInventoryFull(player);
            LOGGER.at(Level.INFO).log("Test 6: Inventory full? " + (isFull ? "Yes" : "No"));
            
            // Test 7: Get all items
            java.util.List<com.hypixel.hytale.server.core.inventory.ItemStack> allItems = org.hytaledevlib.lib.InventoryHelper.getAllItems(player);
            LOGGER.at(Level.INFO).log("Test 7: Total items in inventory: " + allItems.size());
            
            LOGGER.at(Level.INFO).log("=== InventoryHelper Tests Complete ===");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("InventoryHelper test error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void testPlayerHelper(World world) {
        LOGGER.at(Level.INFO).log("=== PlayerHelper Tests Starting ===");
        
        try {
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Test 1: Get game mode
            com.hypixel.hytale.protocol.GameMode gameMode = org.hytaledevlib.lib.PlayerHelper.getGameMode(player);
            LOGGER.at(Level.INFO).log("Test 1: Player game mode: " + (gameMode != null ? gameMode.toString() : "null"));
            
            // Test 2: Send message
            boolean sentMessage = org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "Hello from PlayerHelper test!");
            LOGGER.at(Level.INFO).log("Test 2: Sent message to player: " + (sentMessage ? "✓ Success" : "✗ Failed"));
            
            // Test 3: Check permission
            boolean hasTestPerm = org.hytaledevlib.lib.PlayerHelper.hasPermission(player, "test.permission");
            LOGGER.at(Level.INFO).log("Test 3: Has 'test.permission': " + (hasTestPerm ? "Yes" : "No"));
            
            // Test 4: Check permission with default
            boolean hasAdminPerm = org.hytaledevlib.lib.PlayerHelper.hasPermission(player, "admin.permission", false);
            LOGGER.at(Level.INFO).log("Test 4: Has 'admin.permission' (default false): " + (hasAdminPerm ? "Yes" : "No"));
            
            LOGGER.at(Level.INFO).log("=== PlayerHelper Tests Complete ===");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("PlayerHelper test error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void testEntityHelper(World world) {
        LOGGER.at(Level.INFO).log("=== EntityHelper Tests Starting ===");
        
        try {
            // Get the first player in the world
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            // Get first player
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Test 1: Get player name and UUID (using PlayerRef)
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(player);
            java.util.UUID playerUuid = player.getUuid();
            
            LOGGER.at(Level.INFO).log("=== Player Info ===");
            LOGGER.at(Level.INFO).log("Name: " + playerName + " (from PlayerRef.getUsername())");
            LOGGER.at(Level.INFO).log("UUID: " + playerUuid);
            
            // Test 2: Get player position
            com.hypixel.hytale.math.vector.Vector3d playerPos = org.hytaledevlib.lib.EntityHelper.getPosition(player);
            if (playerPos != null) {
                LOGGER.at(Level.INFO).log("Position: X=" + playerPos.getX() + ", Y=" + playerPos.getY() + ", Z=" + playerPos.getZ());
            }
            
            // Test 3: Test player lookup by name
            com.hypixel.hytale.server.core.entity.Entity foundPlayer = org.hytaledevlib.lib.EntityHelper.getPlayerByName(world, playerName);
            if (foundPlayer != null) {
                LOGGER.at(Level.INFO).log("✓ Successfully found player by name!");
            }
            
            // Test 4: Test player lookup by UUID
            com.hypixel.hytale.server.core.entity.Entity foundByUuid = org.hytaledevlib.lib.EntityHelper.getPlayerByUUID(world, playerUuid);
            if (foundByUuid != null) {
                LOGGER.at(Level.INFO).log("✓ Successfully found player by UUID!");
            }
            
            // Test 5: Check if entity is a player
            boolean isPlayer = org.hytaledevlib.lib.EntityHelper.isPlayer(player);
            LOGGER.at(Level.INFO).log("Is player: " + isPlayer);
            
            // Test 6: Check if entity exists
            boolean exists = org.hytaledevlib.lib.EntityHelper.exists(player);
            LOGGER.at(Level.INFO).log("Entity exists: " + exists);
            
            LOGGER.at(Level.INFO).log("=== Entity Iteration Tests ===");
            
            // Test 7: Get ALL loaded entities (using reflection)
            java.util.List<com.hypixel.hytale.server.core.entity.Entity> allEntities = 
                org.hytaledevlib.lib.EntityHelper.getAllEntities(world);
            LOGGER.at(Level.INFO).log("Total loaded entities: " + allEntities.size());
            
            // Log first 10 entities with their types
            LOGGER.at(Level.INFO).log("First 10 entities:");
            int count = 0;
            for (com.hypixel.hytale.server.core.entity.Entity entity : allEntities) {
                if (count >= 10) break;
                String entityName = org.hytaledevlib.lib.EntityHelper.getName(entity);
                String entityClass = entity.getClass().getSimpleName();
                String entityType = org.hytaledevlib.lib.EntityHelper.getEntityType(entity);
                boolean entityIsPlayer = org.hytaledevlib.lib.EntityHelper.isPlayer(entity);
                LOGGER.at(Level.INFO).log("  " + (count + 1) + ". " + entityClass + 
                    " - Type: " + entityType + 
                    " - Name: " + entityName + 
                    " - IsPlayer: " + entityIsPlayer);
                
                count++;
            }
            
            // Test 8: Find closest entity to player (excluding the player itself)
            if (playerPos != null) {
                com.hypixel.hytale.server.core.entity.Entity closestEntity = 
                    org.hytaledevlib.lib.EntityHelper.getClosestEntity(world, playerPos, player);
                if (closestEntity != null) {
                    double distance = org.hytaledevlib.lib.EntityHelper.getDistance(player, closestEntity);
                    String closestName = org.hytaledevlib.lib.EntityHelper.getName(closestEntity);
                    LOGGER.at(Level.INFO).log("Closest entity (excluding player): " + closestName + 
                        " (" + closestEntity.getClass().getSimpleName() + ") at " + 
                        String.format("%.2f", distance) + " blocks");
                } else {
                    LOGGER.at(Level.INFO).log("No entities found nearby");
                }
            }
            
            // Test 9: Spawn NPCs near player
            LOGGER.at(Level.INFO).log("=== Testing NPC Spawning ===");
            if (playerPos != null) {
                // Spawn a cow 5 blocks in front of player
                com.hypixel.hytale.math.vector.Vector3d cowPos = new com.hypixel.hytale.math.vector.Vector3d(
                    playerPos.getX() + 5, playerPos.getY(), playerPos.getZ()
                );
                com.hypixel.hytale.server.core.entity.Entity cow = org.hytaledevlib.lib.EntityHelper.spawnNPC(world, "Cow", cowPos);
                if (cow != null) {
                    LOGGER.at(Level.INFO).log("✓ Successfully spawned a Cow at " + cowPos);
                } else {
                    LOGGER.at(Level.WARNING).log("✗ Failed to spawn Cow");
                }
                
                // Spawn a deer 5 blocks to the right
                com.hypixel.hytale.math.vector.Vector3d deerPos = new com.hypixel.hytale.math.vector.Vector3d(
                    playerPos.getX(), playerPos.getY(), playerPos.getZ() + 5
                );
                com.hypixel.hytale.server.core.entity.Entity deer = org.hytaledevlib.lib.EntityHelper.spawnNPC(world, "Deer_Doe", deerPos);
                if (deer != null) {
                    LOGGER.at(Level.INFO).log("✓ Successfully spawned a Deer at " + deerPos);
                } else {
                    LOGGER.at(Level.WARNING).log("✗ Failed to spawn Deer");
                }
                
                // Spawn a chicken 5 blocks behind with rotation
                com.hypixel.hytale.math.vector.Vector3d chickenPos = new com.hypixel.hytale.math.vector.Vector3d(
                    playerPos.getX() - 5, playerPos.getY(), playerPos.getZ()
                );
                com.hypixel.hytale.server.core.entity.Entity chicken = org.hytaledevlib.lib.EntityHelper.spawnNPC(
                    world, "Chicken", chickenPos, (float) Math.PI
                );
                if (chicken != null) {
                    LOGGER.at(Level.INFO).log("✓ Successfully spawned a Chicken with rotation at " + chickenPos);
                } else {
                    LOGGER.at(Level.WARNING).log("✗ Failed to spawn Chicken");
                }
                
                LOGGER.at(Level.INFO).log("NPC spawning test complete!");
            }
            
            // Test 10: Time and Day System
            LOGGER.at(Level.INFO).log("=== Testing Time and Day System ===");
            
            // Get current time information
            java.time.LocalDateTime gameDateTime = org.hytaledevlib.lib.WorldHelper.getGameDateTime(world);
            if (gameDateTime != null) {
                LOGGER.at(Level.INFO).log("Current game date/time: " + gameDateTime);
                LOGGER.at(Level.INFO).log("Year: " + org.hytaledevlib.lib.WorldHelper.getYear(world));
                LOGGER.at(Level.INFO).log("Day of year: " + org.hytaledevlib.lib.WorldHelper.getDayOfYear(world));
                LOGGER.at(Level.INFO).log("Current hour: " + org.hytaledevlib.lib.WorldHelper.getCurrentHour(world));
            }
            
            // Get day/night information
            float dayProgress = org.hytaledevlib.lib.WorldHelper.getDayProgress(world);
            double sunlight = org.hytaledevlib.lib.WorldHelper.getSunlightFactor(world);
            int moonPhase = org.hytaledevlib.lib.WorldHelper.getMoonPhase(world);
            boolean isDaytime = org.hytaledevlib.lib.WorldHelper.isDaytime(world);
            
            LOGGER.at(Level.INFO).log("Day progress: " + String.format("%.2f", dayProgress * 100) + "%");
            LOGGER.at(Level.INFO).log("Sunlight factor: " + String.format("%.2f", sunlight));
            LOGGER.at(Level.INFO).log("Moon phase: " + moonPhase);
            LOGGER.at(Level.INFO).log("Is daytime: " + isDaytime);
            LOGGER.at(Level.INFO).log("Is nighttime: " + org.hytaledevlib.lib.WorldHelper.isNighttime(world));
            
            LOGGER.at(Level.INFO).log("Time and day system test complete!");
            
            LOGGER.at(Level.INFO).log("=== BlockHelper Tests Starting ===");
            
            // Test 10: Change block under player
            if (playerPos != null) {
                int blockX = (int) Math.floor(playerPos.getX());
                int blockY = (int) Math.floor(playerPos.getY()) - 1; // Block below player
                int blockZ = (int) Math.floor(playerPos.getZ());
                
                // Calculate chunk coordinates for debugging
                int chunkX = ChunkUtil.chunkCoordinate(blockX);
                int chunkZ = ChunkUtil.chunkCoordinate(blockZ);
                long chunkPos = ChunkUtil.indexChunk(chunkX, chunkZ);
                
                LOGGER.at(Level.INFO).log("Player position: " + playerPos);
                LOGGER.at(Level.INFO).log("Block coordinates: (" + blockX + ", " + blockY + ", " + blockZ + ")");
                LOGGER.at(Level.INFO).log("Chunk coordinates: (" + chunkX + ", " + chunkZ + ") = " + chunkPos);
                
                // Check chunk status
                com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk chunk = world.getChunkIfLoaded(chunkPos);
                LOGGER.at(Level.INFO).log("Chunk loaded: " + (chunk != null));
                if (chunk == null) {
                    chunk = world.getChunkIfInMemory(chunkPos);
                    LOGGER.at(Level.INFO).log("Chunk in memory: " + (chunk != null));
                }
                
                // Check ChunkStore
                com.hypixel.hytale.server.core.universe.world.storage.ChunkStore chunkStore = world.getChunkStore();
                LOGGER.at(Level.INFO).log("ChunkStore: " + (chunkStore != null));
                
                if (chunkStore != null) {
                    // Check if chunk is in ChunkStore
                    it.unimi.dsi.fastutil.longs.LongSet chunkIndexes = chunkStore.getChunkIndexes();
                    LOGGER.at(Level.INFO).log("Total chunks in store: " + chunkIndexes.size());
                    LOGGER.at(Level.INFO).log("Chunk " + chunkPos + " in store: " + chunkIndexes.contains(chunkPos));
                    
                    // Show first 10 chunks in store to see the pattern
                    LOGGER.at(Level.INFO).log("First 10 chunks in store:");
                    int chunkCount = 0;
                    for (long storedChunk : chunkIndexes) {
                        if (chunkCount >= 10) break;
                        int storedChunkX = (int) (storedChunk >> 32);
                        int storedChunkZ = (int) storedChunk;
                        LOGGER.at(Level.INFO).log("  Chunk " + chunkCount + ": pos=" + storedChunk + " coords=(" + storedChunkX + ", " + storedChunkZ + ")");
                        chunkCount++;
                    }
                    
                    // Try to get BlockChunk component
                    com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk blockChunk = 
                        chunkStore.getChunkComponent(chunkPos, 
                            com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk.getComponentType());
                    LOGGER.at(Level.INFO).log("BlockChunk from ChunkStore: " + (blockChunk != null));
                    
                    // If chunk not in store, suggest moving to spawn
                    if (!chunkIndexes.contains(chunkPos)) {
                        LOGGER.at(Level.WARNING).log("⚠ Your chunk is not in the ChunkStore!");
                        LOGGER.at(Level.WARNING).log("⚠ BlockHelper can only access chunks that are loaded in the store.");
                        LOGGER.at(Level.WARNING).log("⚠ Chunk index is computed with ChunkUtil (32x32 chunks). This usually means the area hasn't been generated/loaded into ChunkStore yet.");
                    }
                }
                
                // Get current block
                int currentBlock = org.hytaledevlib.lib.BlockHelper.getBlock(world, blockX, blockY, blockZ);
                String blockName = org.hytaledevlib.lib.BlockHelper.getBlockName(currentBlock);
                int currentMeta = org.hytaledevlib.lib.BlockHelper.getBlockMeta(world, blockX, blockY, blockZ);
                LOGGER.at(Level.INFO).log("Block under player: " + currentBlock + " (" + blockName + ") meta=" + currentMeta);
                
                // Set block to Rock_Stone_Cobble using name-based method (ID 224 - a solid, visible block)
                int cobbleId = org.hytaledevlib.lib.BlockHelper.getBlockId("Rock_Stone_Cobble");
                int cobbleDefaultMeta = org.hytaledevlib.lib.BlockHelper.getDefaultBlockMeta(cobbleId);
                LOGGER.at(Level.INFO).log("Default meta for Rock_Stone_Cobble (" + cobbleId + ") = " + cobbleDefaultMeta);
                boolean setSuccess = org.hytaledevlib.lib.BlockHelper.setBlockByName(world, blockX, blockY, blockZ, "Rock_Stone_Cobble");
                if (setSuccess) {
                    LOGGER.at(Level.INFO).log("✓ Changed block under player to Rock_Stone_Cobble");
                    // Verify the change
                    int newBlock = org.hytaledevlib.lib.BlockHelper.getBlock(world, blockX, blockY, blockZ);
                    String newBlockName = org.hytaledevlib.lib.BlockHelper.getBlockName(newBlock);
                    int newMeta = org.hytaledevlib.lib.BlockHelper.getBlockMeta(world, blockX, blockY, blockZ);
                    LOGGER.at(Level.INFO).log("Block is now: " + newBlock + " (" + newBlockName + ") meta=" + newMeta);
                } else {
                    LOGGER.at(Level.WARNING).log("✗ Failed to change block under player (block name may not exist or chunk not loaded)");
                }
                
                // Also test with Soil_Grass (ID 640) - another common solid block
                LOGGER.at(Level.INFO).log("Testing with Soil_Grass...");
                int grassId = org.hytaledevlib.lib.BlockHelper.getBlockId("Soil_Grass");
                int grassDefaultMeta = org.hytaledevlib.lib.BlockHelper.getDefaultBlockMeta(grassId);
                LOGGER.at(Level.INFO).log("Default meta for Soil_Grass (" + grassId + ") = " + grassDefaultMeta);
                boolean grassSuccess = org.hytaledevlib.lib.BlockHelper.setBlockByName(world, blockX + 1, blockY, blockZ, "Soil_Grass");
                if (grassSuccess) {
                    int grassBlock = org.hytaledevlib.lib.BlockHelper.getBlock(world, blockX + 1, blockY, blockZ);
                    String grassName = org.hytaledevlib.lib.BlockHelper.getBlockName(grassBlock);
                    int grassMeta = org.hytaledevlib.lib.BlockHelper.getBlockMeta(world, blockX + 1, blockY, blockZ);
                    LOGGER.at(Level.INFO).log("✓ Set adjacent block to: " + grassBlock + " (" + grassName + ") meta=" + grassMeta);
                }
                
                // Test with Rock_Stone (ID 1032) - the base stone block
                LOGGER.at(Level.INFO).log("Testing with Rock_Stone...");
                int stoneId = org.hytaledevlib.lib.BlockHelper.getBlockId("Rock_Stone");
                int stoneDefaultMeta = org.hytaledevlib.lib.BlockHelper.getDefaultBlockMeta(stoneId);
                LOGGER.at(Level.INFO).log("Default meta for Rock_Stone (" + stoneId + ") = " + stoneDefaultMeta);
                boolean stoneSuccess = org.hytaledevlib.lib.BlockHelper.setBlockByName(world, blockX - 1, blockY, blockZ, "Rock_Stone");
                if (stoneSuccess) {
                    int stoneBlock = org.hytaledevlib.lib.BlockHelper.getBlock(world, blockX - 1, blockY, blockZ);
                    String stoneName = org.hytaledevlib.lib.BlockHelper.getBlockName(stoneBlock);
                    int stoneMeta = org.hytaledevlib.lib.BlockHelper.getBlockMeta(world, blockX - 1, blockY, blockZ);
                    LOGGER.at(Level.INFO).log("✓ Set adjacent block to: " + stoneBlock + " (" + stoneName + ") meta=" + stoneMeta);
                }
            }
            
            // Test 11: Find blocks within radius
            if (playerPos != null) {
                int searchRadius = 10;
                LOGGER.at(Level.INFO).log("=== Scanning blocks within " + searchRadius + " block radius ===");
                
                // Get all blocks in a region around player
                com.hypixel.hytale.math.vector.Vector3d corner1 = new com.hypixel.hytale.math.vector.Vector3d(
                    playerPos.getX() - searchRadius,
                    Math.max(0, playerPos.getY() - searchRadius),
                    playerPos.getZ() - searchRadius
                );
                com.hypixel.hytale.math.vector.Vector3d corner2 = new com.hypixel.hytale.math.vector.Vector3d(
                    playerPos.getX() + searchRadius,
                    Math.min(319, playerPos.getY() + searchRadius),
                    playerPos.getZ() + searchRadius
                );
                
                java.util.List<org.hytaledevlib.lib.BlockHelper.BlockPosition> blocks = 
                    org.hytaledevlib.lib.BlockHelper.getBlocksInRegion(world, corner1, corner2);
                
                // Count different block types
                java.util.Map<Integer, Integer> blockCounts = new java.util.HashMap<>();
                for (org.hytaledevlib.lib.BlockHelper.BlockPosition block : blocks) {
                    blockCounts.put(block.blockId, blockCounts.getOrDefault(block.blockId, 0) + 1);
                }
                
                LOGGER.at(Level.INFO).log("Found " + blocks.size() + " total blocks in radius");
                LOGGER.at(Level.INFO).log("Block type distribution:");
                int blockTypeCount = 0;
                for (java.util.Map.Entry<Integer, Integer> entry : blockCounts.entrySet()) {
                    if (blockTypeCount >= 10) break; // Show top 10
                    String name = org.hytaledevlib.lib.BlockHelper.getBlockName(entry.getKey());
                    LOGGER.at(Level.INFO).log("  Block ID " + entry.getKey() + " (" + name + "): " + entry.getValue() + " blocks");
                    blockTypeCount++;
                }
                // Count air blocks
                int airCount = blockCounts.getOrDefault(0, 0);
                double airPercentage = (airCount * 100.0) / blocks.size();
                LOGGER.at(Level.INFO).log("Air blocks: " + airCount + " (" + String.format("%.1f", airPercentage) + "%)");
            }
            
            LOGGER.at(Level.INFO).log("=== BlockHelper Tests Complete ===");
            LOGGER.at(Level.INFO).log("=== All Tests Complete ===");
            
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error during EntityHelper tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startTickTests() {
        LOGGER.at(Level.INFO).log("Starting tick tracking tests...");
        
        // Test: Log EVERY tick
        WorldHelper.onTick(world, currentTick -> {
            WorldHelper.log(world, "[Test] Tick: " + currentTick);
        });
        
        /* Interval tests - commented out for now
        // Test 1: Log every second (20 ticks)
        WorldHelper.onTickInterval(world, 20, currentTick -> {
            WorldHelper.log(world, "[Test] Tick: " + currentTick + " (1 second interval)");
        });
        
        // Test 2: Log every 5 seconds (100 ticks)
        WorldHelper.onTickInterval(world, 100, currentTick -> {
            WorldHelper.logWarning(world, "[Test] Tick: " + currentTick + " (5 second interval)");
        });
        
        // Test 3: Log every 10 seconds (200 ticks)
        WorldHelper.onTickInterval(world, 200, currentTick -> {
            WorldHelper.logError(world, "[Test] Tick: " + currentTick + " (10 second interval)");
            
            // Also test player count
            int playerCount = WorldHelper.getPlayerCount(world);
            WorldHelper.log(world, "[Test] Current player count: " + playerCount);
        });
        */
        
        LOGGER.at(Level.INFO).log("Tick tracking test registered successfully!");
        LOGGER.at(Level.INFO).log("Watch the logs - will log EVERY tick!");
    }
    
    /**
     * Register tests for all the new EventHelper methods.
     * These will log when events are triggered so you can verify they work.
     */
    private void registerEventTests() {
        LOGGER.at(Level.INFO).log("Registering EventHelper tests...");
        
        // Test onPlayerChat - WORKS
        org.hytaledevlib.lib.EventHelper.onPlayerChat(this, (username, message) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Chat from " + username + ": " + message);
        });
        
        // Test onPlayerDisconnect - Testing if this works
        org.hytaledevlib.lib.EventHelper.onPlayerDisconnect(this, (username) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Player disconnected: " + username);
        });
        
        // Test onItemDrop - WORKS (with correct quantity)
        org.hytaledevlib.lib.EventHelper.onItemDrop(this, (itemId, quantity) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Item dropped: " + quantity + "x " + itemId);
        });
        
        // Test onItemPickup - WORKS
        org.hytaledevlib.lib.EventHelper.onItemPickup(this, (itemId, quantity) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Item picked up: " + quantity + "x " + itemId);
        });
        
        LOGGER.at(Level.INFO).log("EventHelper tests registered! Working events:");
        LOGGER.at(Level.INFO).log("  ✓ Sending chat messages");
        LOGGER.at(Level.INFO).log("  ✓ Dropping items (with correct quantity)");
        LOGGER.at(Level.INFO).log("  ✓ Picking up items");
        LOGGER.at(Level.INFO).log("  ✓ Player disconnect");
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("Note: Block breaking/placing use EcsEventHelper (see below)");
        LOGGER.at(Level.INFO).log("Block interaction (F key), crafting, and gamemode changes");
        LOGGER.at(Level.INFO).log("are not available through simple events.");
    }
    
    /**
     * Register ECS event tests using the new EcsEventHelper.
     * These must be registered after we have a World instance.
     */
    private void registerEcsEventTests(World world) {
        LOGGER.at(Level.INFO).log("Registering ECS EventHelper tests...");
        
        // Test onBlockBreak - ECS event
        org.hytaledevlib.lib.EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block broken at " + position + " - Type: " + blockTypeId);
        });
        
        // Test onBlockPlace - ECS event
        org.hytaledevlib.lib.EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block placed at " + position + " - Item: " + itemId);
        });
        
        LOGGER.at(Level.INFO).log("ECS EventHelper tests registered!");
        LOGGER.at(Level.INFO).log("  ✓ Block breaking (filters out Empty blocks)");
        LOGGER.at(Level.INFO).log("  ✓ Block placing");
    }
}
