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
                    
                    // Register LootHelper test
                    //registerLootHelperTest(world);
                    
                    // Register ContainerHelper test (uses existing ECS events)
                    registerContainerHelperTest(world);
                    
                    // Generate block list for wiki
                    // LOGGER.at(Level.INFO).log("Generating block list for wiki...");
                    // String blockListPath = "wiki/BlockList.md";
                    // int blockCount = org.hytaledevlib.tools.BlockListGenerator.generateBlockList(blockListPath);
                    // LOGGER.at(Level.INFO).log("Block count returned: " + blockCount);
                    // if (blockCount > 0) {
                    //     LOGGER.at(Level.INFO).log("✓ Generated block list with " + blockCount + " blocks at: " + blockListPath);
                    // } else {
                    //     LOGGER.at(Level.WARNING).log("✗ Failed to generate block list (returned " + blockCount + ")");
                    // }
                    
                    // Test all helpers: Wait 100 ticks for player to fully load
                    LOGGER.at(Level.INFO).log("Testing helpers: Will run tests in 100 ticks (5 seconds)...");
                    WorldHelper.waitTicks(world, 100, () -> {
                        // testInventoryHelper(world);
                        // testPlayerHelper(world);
                        // testEntityHelper(world);
                        // testUIHelper(world);
                        // testBlockStateHelper(world);
                        //testZoneHelper(world);
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
    
    private void testUIHelper(World world) {
        LOGGER.at(Level.INFO).log("=== UIHelper Tests Starting ===");
        
        try {
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Test 1: Hide specific HUD components
            // Test 0: Log player skin data
            LOGGER.at(Level.INFO).log("Test 0: Retrieving player skin data...");
            com.hypixel.hytale.protocol.PlayerSkin skin = org.hytaledevlib.lib.PlayerHelper.getPlayerSkin(player);
            if (skin != null) {
                LOGGER.at(Level.INFO).log("=== Player Appearance Data ===");
                LOGGER.at(Level.INFO).log("  Body Characteristic: " + (skin.bodyCharacteristic != null ? skin.bodyCharacteristic : "None"));
                LOGGER.at(Level.INFO).log("  Face: " + (skin.face != null ? skin.face : "None"));
                LOGGER.at(Level.INFO).log("  Eyes: " + (skin.eyes != null ? skin.eyes : "None"));
                LOGGER.at(Level.INFO).log("  Ears: " + (skin.ears != null ? skin.ears : "None"));
                LOGGER.at(Level.INFO).log("  Mouth: " + (skin.mouth != null ? skin.mouth : "None"));
                LOGGER.at(Level.INFO).log("  Facial Hair: " + (skin.facialHair != null ? skin.facialHair : "None"));
                LOGGER.at(Level.INFO).log("  Haircut: " + (skin.haircut != null ? skin.haircut : "None"));
                LOGGER.at(Level.INFO).log("  Eyebrows: " + (skin.eyebrows != null ? skin.eyebrows : "None"));
                LOGGER.at(Level.INFO).log("  Underwear: " + (skin.underwear != null ? skin.underwear : "None"));
                LOGGER.at(Level.INFO).log("  Pants: " + (skin.pants != null ? skin.pants : "None"));
                LOGGER.at(Level.INFO).log("  Overpants: " + (skin.overpants != null ? skin.overpants : "None"));
                LOGGER.at(Level.INFO).log("  Undertop: " + (skin.undertop != null ? skin.undertop : "None"));
                LOGGER.at(Level.INFO).log("  Overtop: " + (skin.overtop != null ? skin.overtop : "None"));
                LOGGER.at(Level.INFO).log("  Shoes: " + (skin.shoes != null ? skin.shoes : "None"));
                LOGGER.at(Level.INFO).log("  Gloves: " + (skin.gloves != null ? skin.gloves : "None"));
                LOGGER.at(Level.INFO).log("  Head Accessory: " + (skin.headAccessory != null ? skin.headAccessory : "None"));
                LOGGER.at(Level.INFO).log("  Face Accessory: " + (skin.faceAccessory != null ? skin.faceAccessory : "None"));
                LOGGER.at(Level.INFO).log("  Ear Accessory: " + (skin.earAccessory != null ? skin.earAccessory : "None"));
                LOGGER.at(Level.INFO).log("  Skin Feature: " + (skin.skinFeature != null ? skin.skinFeature : "None"));
                LOGGER.at(Level.INFO).log("  Cape: " + (skin.cape != null ? skin.cape : "None"));
                LOGGER.at(Level.INFO).log("==============================");
            } else {
                LOGGER.at(Level.WARNING).log("  Could not retrieve player skin data!");
            }
            
            LOGGER.at(Level.INFO).log("Test 1: Hiding hotbar, health, and stamina HUD components...");
            boolean hideSuccess = org.hytaledevlib.lib.UIHelper.hideHudComponents(player, 
                com.hypixel.hytale.protocol.packets.interface_.HudComponent.Hotbar,
                com.hypixel.hytale.protocol.packets.interface_.HudComponent.Health,
                com.hypixel.hytale.protocol.packets.interface_.HudComponent.Stamina);
            LOGGER.at(Level.INFO).log("  Result: " + (hideSuccess ? "✓ Success" : "✗ Failed"));
            
            // Test 2: Show them back after 3 seconds
            WorldHelper.waitTicks(world, 60, () -> {
                LOGGER.at(Level.INFO).log("Test 2: Showing hotbar, health, and stamina back...");
                boolean showSuccess = org.hytaledevlib.lib.UIHelper.showHudComponents(player,
                    com.hypixel.hytale.protocol.packets.interface_.HudComponent.Hotbar,
                    com.hypixel.hytale.protocol.packets.interface_.HudComponent.Health,
                    com.hypixel.hytale.protocol.packets.interface_.HudComponent.Stamina);
                LOGGER.at(Level.INFO).log("  Result: " + (showSuccess ? "✓ Success" : "✗ Failed"));
            });
            

            // Test 5: Check visible HUD components
            java.util.Set<com.hypixel.hytale.protocol.packets.interface_.HudComponent> visibleComponents = 
                org.hytaledevlib.lib.UIHelper.getVisibleHudComponents(player);
            if (visibleComponents != null) {
                LOGGER.at(Level.INFO).log("Test 5: Currently visible HUD components: " + visibleComponents.size());
            }
            
            LOGGER.at(Level.INFO).log("=== UIHelper Tests Complete ===");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("UIHelper test error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void testBlockStateHelper(World world) {
        LOGGER.at(Level.INFO).log("=== BlockStateHelper Tests Starting ===");
        
        try {
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Get player position and calculate position behind them
            com.hypixel.hytale.math.vector.Vector3d playerPos = org.hytaledevlib.lib.EntityHelper.getPosition(player);
            if (playerPos == null) {
                LOGGER.at(Level.WARNING).log("Could not get player position!");
                return;
            }
            
            // Place chest 2 blocks behind player (assuming they're facing +Z)
            int chestX = (int) Math.floor(playerPos.x);
            int chestY = (int) Math.floor(playerPos.y);
            int chestZ = (int) Math.floor(playerPos.z - 2);
            
            LOGGER.at(Level.INFO).log("Test 1: Placing chest at (" + chestX + ", " + chestY + ", " + chestZ + ")...");
            
            // Place the chest block
            boolean placed = org.hytaledevlib.lib.BlockHelper.setBlockByName(world, chestX, chestY, chestZ, "Furniture_Desert_Chest_Small");
            if (!placed) {
                LOGGER.at(Level.WARNING).log("  Failed to place chest block!");
                return;
            }
            LOGGER.at(Level.INFO).log("  ✓ Chest block placed");
            
            // Wait a tick for the block to fully initialize
            WorldHelper.waitTicks(world, 2, () -> {
                try {
                    LOGGER.at(Level.INFO).log("Test 2: Accessing chest state and adding items...");
                    
                    // Ensure the chest has state data
                    com.hypixel.hytale.server.core.universe.world.meta.BlockState state = 
                        org.hytaledevlib.lib.BlockStateHelper.ensureState(world, chestX, chestY, chestZ);
                    
                    if (state == null) {
                        LOGGER.at(Level.WARNING).log("  Failed to get/create chest state!");
                        return;
                    }
                    
                    // Check if it's an ItemContainerState (chest)
                    if (!(state instanceof com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState)) {
                        LOGGER.at(Level.WARNING).log("  Block state is not an ItemContainerState!");
                        return;
                    }
                    
                    com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState chestState = 
                        (com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState) state;
                    
                    LOGGER.at(Level.INFO).log("  ✓ Got chest container state");
                    
                    // Get the item container
                    com.hypixel.hytale.server.core.inventory.container.ItemContainer container = chestState.getItemContainer();
                    if (container == null) {
                        LOGGER.at(Level.WARNING).log("  Chest has no item container!");
                        return;
                    }
                    
                    // Use ItemHelper to easily add items to the chest in random slots
                    int itemsAdded = org.hytaledevlib.lib.ItemHelper.fillContainerRandom(container,
                        "Furniture_Crude_Torch", 1,
                        "Ingredient_Bone_Fragment", 10
                    );
                    
                    if (itemsAdded == 2) {
                        LOGGER.at(Level.INFO).log("  ✓ Added 1 torch and 10 bone fragments to chest in random slots using ItemHelper");
                        
                        // Mark the state as needing to be saved
                        org.hytaledevlib.lib.BlockStateHelper.markNeedsSave(chestState);
                        LOGGER.at(Level.INFO).log("  ✓ Marked chest state for saving");
                        
                        // Use ItemHelper to get info about the chest
                        int torchCount = org.hytaledevlib.lib.ItemHelper.countItemInContainer(container, "Furniture_Crude_Torch");
                        int boneCount = org.hytaledevlib.lib.ItemHelper.countItemInContainer(container, "Ingredient_Bone_Fragment");
                        LOGGER.at(Level.INFO).log("  Chest contains: " + torchCount + " torches, " + boneCount + " bone fragments");
                    } else {
                        LOGGER.at(Level.WARNING).log("  Failed to add all items (added " + itemsAdded + "/2)");
                    }
                    
                    LOGGER.at(Level.INFO).log("=== BlockStateHelper Tests Complete ===");
                    LOGGER.at(Level.INFO).log("Go check the chest behind you!");
                    
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Error in chest item test: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("BlockStateHelper test error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void testZoneHelper(World world) {
        LOGGER.at(Level.INFO).log("=== ZoneHelper Tests Starting ===");
        
        try {
            // Initialize zone tracking
            LOGGER.at(Level.INFO).log("Test 1: Initializing zone tracking...");
            org.hytaledevlib.lib.ZoneHelper.initializeZoneTracking(world);
            LOGGER.at(Level.INFO).log("  ✓ Zone tracking initialized");
            
            if (WorldHelper.getPlayerCount(world) == 0) {
                LOGGER.at(Level.WARNING).log("No players found in world!");
                return;
            }
            
            com.hypixel.hytale.server.core.entity.Entity player = world.getPlayers().iterator().next();
            
            // Test 2: Manually discover some zones
            LOGGER.at(Level.INFO).log("Test 2: Manually discovering zones...");
            boolean discovered1 = org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Forest");
            boolean discovered2 = org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Mountain");
            boolean discovered3 = org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Cave");
            
            if (discovered1 && discovered2 && discovered3) {
                LOGGER.at(Level.INFO).log("  ✓ Discovered 3 test zones");
            } else {
                LOGGER.at(Level.WARNING).log("  Some zones were not marked as new discoveries");
            }
            
            // Test 3: Try to discover the same zone again (should return false)
            LOGGER.at(Level.INFO).log("Test 3: Testing duplicate discovery...");
            boolean duplicate = org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Forest");
            if (!duplicate) {
                LOGGER.at(Level.INFO).log("  ✓ Correctly identified duplicate discovery");
            } else {
                LOGGER.at(Level.WARNING).log("  Failed: Zone was marked as new when it shouldn't be");
            }
            
            // Test 4: Get discovered zones
            LOGGER.at(Level.INFO).log("Test 4: Getting discovered zones...");
            java.util.List<String> discoveredZones = org.hytaledevlib.lib.ZoneHelper.getDiscoveredZones(player);
            LOGGER.at(Level.INFO).log("  Player has discovered " + discoveredZones.size() + " zones:");
            for (String zone : discoveredZones) {
                LOGGER.at(Level.INFO).log("    - " + zone);
            }
            
            // Test 5: Check if player has discovered specific zones
            LOGGER.at(Level.INFO).log("Test 5: Checking specific zone discoveries...");
            boolean hasForest = org.hytaledevlib.lib.ZoneHelper.hasDiscoveredZone(player, "Test_Forest");
            boolean hasDesert = org.hytaledevlib.lib.ZoneHelper.hasDiscoveredZone(player, "Test_Desert");
            LOGGER.at(Level.INFO).log("  Has discovered Test_Forest: " + hasForest + " (should be true)");
            LOGGER.at(Level.INFO).log("  Has discovered Test_Desert: " + hasDesert + " (should be false)");
            
            // Test 6: Get discovery count
            LOGGER.at(Level.INFO).log("Test 6: Getting discovery count...");
            int count = org.hytaledevlib.lib.ZoneHelper.getDiscoveredZoneCount(player);
            LOGGER.at(Level.INFO).log("  ✓ Player has discovered " + count + " zones");
            
            // Test 7: Set current zone
            LOGGER.at(Level.INFO).log("Test 7: Setting current zone...");
            org.hytaledevlib.lib.ZoneHelper.setCurrentZone(player, "Test_Forest");
            String currentZone = org.hytaledevlib.lib.ZoneHelper.getCurrentZone(player);
            if ("Test_Forest".equals(currentZone)) {
                LOGGER.at(Level.INFO).log("  ✓ Current zone set to: " + currentZone);
            } else {
                LOGGER.at(Level.WARNING).log("  Failed to set current zone");
            }
            
            // Test 8: Check if player is in zone
            LOGGER.at(Level.INFO).log("Test 8: Checking if player is in zone...");
            boolean inForest = org.hytaledevlib.lib.ZoneHelper.isInZone(player, "Test_Forest");
            boolean inDesert = org.hytaledevlib.lib.ZoneHelper.isInZone(player, "Test_Desert");
            LOGGER.at(Level.INFO).log("  Is in Test_Forest: " + inForest + " (should be true)");
            LOGGER.at(Level.INFO).log("  Is in Test_Desert: " + inDesert + " (should be false)");
            
            // Test 9: Get all players in zone
            LOGGER.at(Level.INFO).log("Test 9: Getting players in zone...");
            java.util.List<com.hypixel.hytale.server.core.entity.Entity> playersInForest = 
                org.hytaledevlib.lib.ZoneHelper.getPlayersInZone(world, "Test_Forest");
            LOGGER.at(Level.INFO).log("  Players in Test_Forest: " + playersInForest.size());
            
            // Test 10: Get all discovered zones (global)
            LOGGER.at(Level.INFO).log("Test 10: Getting all discovered zones (global)...");
            java.util.Set<String> allZones = org.hytaledevlib.lib.ZoneHelper.getAllDiscoveredZones();
            LOGGER.at(Level.INFO).log("  Total unique zones discovered: " + allZones.size());
            for (String zone : allZones) {
                int discoveryCount = org.hytaledevlib.lib.ZoneHelper.getZoneDiscoveryCount(zone);
                LOGGER.at(Level.INFO).log("    - " + zone + " (discovered by " + discoveryCount + " players)");
            }
            
            // Test 11: Clear discovered zones
            LOGGER.at(Level.INFO).log("Test 11: Testing clear discovered zones...");
            org.hytaledevlib.lib.ZoneHelper.clearDiscoveredZones(player);
            int countAfterClear = org.hytaledevlib.lib.ZoneHelper.getDiscoveredZoneCount(player);
            if (countAfterClear == 0) {
                LOGGER.at(Level.INFO).log("  ✓ Successfully cleared all discovered zones");
            } else {
                LOGGER.at(Level.WARNING).log("  Failed to clear zones (count: " + countAfterClear + ")");
            }
            
            // Re-discover zones for further testing
            LOGGER.at(Level.INFO).log("Test 12: Re-discovering zones for ECS event test...");
            org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Forest");
            org.hytaledevlib.lib.ZoneHelper.discoverZone(player, "Test_Mountain");
            LOGGER.at(Level.INFO).log("  ✓ Re-discovered 2 zones");
            
            LOGGER.at(Level.INFO).log("=== ZoneHelper Tests Complete ===");
            LOGGER.at(Level.INFO).log("");
            LOGGER.at(Level.INFO).log("Summary:");
            LOGGER.at(Level.INFO).log("  ✓ Zone tracking initialization");
            LOGGER.at(Level.INFO).log("  ✓ Manual zone discovery");
            LOGGER.at(Level.INFO).log("  ✓ Duplicate discovery detection");
            LOGGER.at(Level.INFO).log("  ✓ Get discovered zones list");
            LOGGER.at(Level.INFO).log("  ✓ Check specific zone discovery");
            LOGGER.at(Level.INFO).log("  ✓ Get discovery count");
            LOGGER.at(Level.INFO).log("  ✓ Set/get current zone");
            LOGGER.at(Level.INFO).log("  ✓ Check if player in zone");
            LOGGER.at(Level.INFO).log("  ✓ Get players in zone");
            LOGGER.at(Level.INFO).log("  ✓ Get all discovered zones (global)");
            LOGGER.at(Level.INFO).log("  ✓ Clear discovered zones");
            LOGGER.at(Level.INFO).log("");
            LOGGER.at(Level.INFO).log("Note: Zone discovery events are tracked via EcsEventHelper.onZoneDiscovery()");
            LOGGER.at(Level.INFO).log("Explore the world to trigger real zone discoveries!");
            
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("ZoneHelper test error: " + e.getMessage());
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
        
        // Test onPlayerJoinWorldWithUUID - Testing if this works
        org.hytaledevlib.lib.EventHelper.onPlayerJoinWorldWithUUID(this, (world, uuid, username) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Player joined: " + username + " (UUID: " + uuid + ") in world: " + world.getName());
        });
        
        // Test onPlayerChat - WORKS
        org.hytaledevlib.lib.EventHelper.onPlayerChat(this, (username, message) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Chat from " + username + ": " + message);
        });
        
        // Test onPlayerDisconnect - Testing if this works
        org.hytaledevlib.lib.EventHelper.onPlayerDisconnect(this, (uuid, username) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Player disconnected: " + username + " (UUID: " + uuid + ")");
        });
        
        // Test onItemDrop - WORKS (with correct quantity)
        org.hytaledevlib.lib.EventHelper.onItemDrop(this, (itemId, quantity) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Item dropped: " + quantity + "x " + itemId);
        });
        
        // Test onItemPickup - WORKS
        org.hytaledevlib.lib.EventHelper.onItemPickup(this, (itemId, quantity) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Item picked up: " + quantity + "x " + itemId);
        });
        
        // Test onCraftRecipe - Testing inventory transaction detection
        org.hytaledevlib.lib.EventHelper.onCraftRecipe(this, (outputItemId, quantity) -> {
            LOGGER.at(Level.INFO).log("[EventTest] Item crafted: " + quantity + "x " + outputItemId);
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
        
        // Test onBlockPlace - ECS event (container auto-registration commented out for testing)
        org.hytaledevlib.lib.EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block placed at " + position + " - Item: " + itemId);
            
            // COMMENTED OUT: Testing interaction-based registration only
            /*
            // Check if this is a container block and auto-register it
            if (org.hytaledevlib.lib.ContainerHelper.isContainerType(itemId)) {
                LOGGER.at(Level.INFO).log("🔍 Container block placed detected: " + itemId + " at " + position);
                LOGGER.at(Level.INFO).log("   Waiting 2 ticks for block state to initialize...");
                
                // Wait for block state to be created
                WorldHelper.waitTicks(world, 2, () -> {
                    // Register the container with the callback defined in registerContainerHelperTest
                    boolean success = registerContainerAtPosition(world, position, itemId);
                    if (success) {
                        LOGGER.at(Level.INFO).log("✅ Successfully auto-registered container at " + position);
                        LOGGER.at(Level.INFO).log("   Type: " + itemId);
                        LOGGER.at(Level.INFO).log("   Total tracked: " + org.hytaledevlib.lib.ContainerHelper.getTrackedContainerCount(world));
                    } else {
                        LOGGER.at(Level.WARNING).log("❌ Failed to auto-register container at " + position);
                        LOGGER.at(Level.WARNING).log("   Type: " + itemId);
                        LOGGER.at(Level.WARNING).log("   Reason: No ItemContainerState found at position");
                    }
                });
            }
            */
        });
        
        // Test onBlockDamage - ECS event (mining progress)
        org.hytaledevlib.lib.EcsEventHelper.onBlockDamage(world, (position, blockTypeId, currentDamage, damage, itemInHand) -> {
            String tool = itemInHand != null ? itemInHand : "Hand";
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block damage at " + position + " - Type: " + blockTypeId + 
                " | Current: " + String.format("%.2f", currentDamage) + 
                " | Damage: " + String.format("%.2f", damage) + 
                " | Tool: " + tool);
        });
        
        // Test onZoneDiscovery - ECS event (zone exploration)
        org.hytaledevlib.lib.EcsEventHelper.onZoneDiscovery(world, (discoveryInfo) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] Zone discovered: " + discoveryInfo.zoneName());
            LOGGER.at(Level.INFO).log("  Region: " + discoveryInfo.regionName());
            LOGGER.at(Level.INFO).log("  Major: " + discoveryInfo.major());
            if (discoveryInfo.icon() != null) {
                LOGGER.at(Level.INFO).log("  Icon: " + discoveryInfo.icon());
            }
        });
        
        // Test onBlockInteract - ECS event (also handles existing container registration)
        org.hytaledevlib.lib.EcsEventHelper.onBlockInteract(world, (position, blockTypeId) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block interacted: " + blockTypeId + " at " + position);
            
            // Check if this is a container block and register it
            if (org.hytaledevlib.lib.ContainerHelper.isContainerType(blockTypeId)) {
                int currentCount = org.hytaledevlib.lib.ContainerHelper.getTrackedContainerCount(world);
                
                // Register the container when player interacts with it
                boolean success = org.hytaledevlib.lib.ContainerHelper.onContainerChange(world, position, (transaction) -> {
                    LOGGER.at(Level.INFO).log("CONTAINER TRANSACTION DETECTED");
                    LOGGER.at(Level.INFO).log("Action: " + transaction.getAction());
                    if (transaction.getItemId() != null) {
                        LOGGER.at(Level.INFO).log("Item: " + transaction.getItemId());
                        LOGGER.at(Level.INFO).log("Quantity: " + transaction.getQuantity());
                    }
                    LOGGER.at(Level.INFO).log("Currently tracking: " + org.hytaledevlib.lib.ContainerHelper.getTrackedContainerCount(world) + " container(s)");
                });
                
                int newCount = org.hytaledevlib.lib.ContainerHelper.getTrackedContainerCount(world);
                
                if (success && newCount > currentCount) {
                    LOGGER.at(Level.INFO).log("Registered existing container at " + position);
                    LOGGER.at(Level.INFO).log("Type: " + blockTypeId);
                    LOGGER.at(Level.INFO).log("Total tracked: " + newCount);
                } else if (success && newCount == currentCount) {
                    LOGGER.at(Level.INFO).log("Container already registered at " + position);
                } else {
                    LOGGER.at(Level.WARNING).log("Failed to register container at " + position);
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("ECS EventHelper tests registered!");
        LOGGER.at(Level.INFO).log("  ✓ Block breaking (filters out Empty blocks)");
        LOGGER.at(Level.INFO).log("  ✓ Block placing");
        LOGGER.at(Level.INFO).log("  ✓ Block damage (mining progress tracking)");
        LOGGER.at(Level.INFO).log("  ✓ Zone discovery (map exploration)");
        LOGGER.at(Level.INFO).log("  ✓ Block interaction (existing container registration)");
    }
    
    /**
     * Register LootHelper test - makes Rock_Stone drop iron swords.
     */
    private void registerLootHelperTest(World world) {
        LOGGER.at(Level.INFO).log("Registering LootHelper test...");
        
        // Make stone blocks drop iron swords instead of stone
        org.hytaledevlib.lib.LootHelper.registerBlockLootReplacement(world, "Rock_Stone", (pos, blockType) -> {
            return java.util.Arrays.asList(
                new org.hytaledevlib.lib.LootHelper.ItemDrop("Weapon_Sword_Iron", 1)
            );
        });
        
        LOGGER.at(Level.INFO).log("LootHelper test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Rock_Stone now drops Weapon_Sword_Iron");
        LOGGER.at(Level.INFO).log("  ✓ Break a stone block to test!");
    }
    
    /**
     * Register ContainerHelper test - sets up container tracking via interaction.
     */
    private void registerContainerHelperTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering ContainerHelper test...");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("✅ ContainerHelper test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Interaction-based registration enabled");
        LOGGER.at(Level.INFO).log("  ✓ Tracking 64 container types:");
        LOGGER.at(Level.INFO).log("    - 18 workbenches (Alchemy, Furnace, etc.)");
        LOGGER.at(Level.INFO).log("    - 46 chests (all variants)");
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("📝 To test:");
        LOGGER.at(Level.INFO).log("  1. Interact with any chest or workbench (right-click or F key)");
        LOGGER.at(Level.INFO).log("  2. Add/remove items using normal clicks or shift-click");
        LOGGER.at(Level.INFO).log("  3. Watch for CONTAINER TRANSACTION logs");
        LOGGER.at(Level.INFO).log("  4. Works with existing containers from previous worlds!");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
}
