package org.hytaledevlib.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.GameMode;
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
    
    // Track which players have which wells open
    private final java.util.Map<String, com.hypixel.hytale.math.vector.Vector3i> openWells = new java.util.concurrent.ConcurrentHashMap<>();
    
    public TestPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.at(Level.INFO).log("HytaleDevLib Test Plugin v" + this.getManifest().getVersion().toString() + " loaded!");
    }
    
    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Setting up HytaleDevLib Test Plugin...");
        
        // Setup economy system
        setupEconomySystem();
        
        // Register example quests
        registerExampleQuests();
        
        // Load saved data
        loadPersistedData();
        
        // Register ItemHelper dispatcher FIRST (before any player joins)
        org.hytaledevlib.lib.ItemHelper.register(this);
        
        // Register item interaction examples
        registerItemInteractions();
        
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
                    
                    // Start continuous well checking system
                    startWellCheckingSystem(world);
                    
                    // Register ParticleHelper test
                    registerParticleHelperTest(world);
                    
                    // Register LootHelper test
                    //registerLootHelperTest(world);
                    
                    // Register ContainerHelper test (uses existing ECS events)
                    registerContainerHelperTest(world);
                    
                    // Register EquipmentHelper test
                    registerEquipmentHelperTest(world);
                    
                    // Register DeathHelper test
                    registerDeathHelperTest(world);
                    
                    // Register mob loot test (uses DeathHelper callback)
                    registerMobLootTest(world);
                    
                    // Setup quest tracking system
                    setupQuestTracking(world);

                    registerWaterPlacementTest(world);
                    
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
                    
                    // Setup periodic auto-save every 5 minutes (6000 ticks)
                    org.hytaledevlib.lib.WorldHelper.onTickInterval(world, 6000, (tick) -> {
                        LOGGER.at(Level.INFO).log("🔄 Auto-saving quest and economy data...");
                        savePersistedData();
                    });
                    
                    // Test all helpers: Wait 100 ticks for player to fully load
                    LOGGER.at(Level.INFO).log("Testing helpers: Will run tests in 100 ticks (5 seconds)...");
                    WorldHelper.waitTicks(world, 100, () -> {
                        LOGGER.at(Level.INFO).log("=== 100 ticks elapsed, starting tick tests now! ===");
                        // testInventoryHelper(world);
                        // testPlayerHelper(world);
                        // testEntityHelper(world);
                        // testUIHelper(world);
                    });
                    
                    // Test game mode switching for player Se7inity
                    testGameModeSwitching(world);
                    
                    // Test item entity teleportation
                    testItemEntityTeleport(world);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Could not capture world: " + e.getMessage());
                }
            }
        });
        
        // Register new event tests
        registerEventTests();
        
        // Register quest commands
        this.getCommandRegistry().registerCommand(new QuestStartCommand());
        this.getCommandRegistry().registerCommand(new QuestListCommand());
        LOGGER.at(Level.INFO).log("✅ Quest commands registered: /queststart, /questlist");
        
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
        
        // Disabled tick-based cooldown override - doesn't work effectively
        
        LOGGER.at(Level.INFO).log("Tick tracking test registered successfully!");
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
            
            // Register equipment tracking for this player
            for (com.hypixel.hytale.server.core.entity.Entity entity : world.getPlayers()) {
                if (entity instanceof com.hypixel.hytale.server.core.entity.LivingEntity livingEntity) {
                    if (org.hytaledevlib.lib.EntityHelper.getUUID(entity).equals(uuid)) {
                        LOGGER.at(Level.INFO).log("Registering equipment tracking for player: " + username);
                        
                        org.hytaledevlib.lib.EquipmentHelper.onEquipmentChange(world, livingEntity, (change, container) -> {
                            String slotType = change.getSlotType().toString();
                            String action = change.isEquipping() ? "EQUIPPED" : "UNEQUIPPED";
                            
                            LOGGER.at(Level.INFO).log("EQUIPMENT CHANGE DETECTED");
                            LOGGER.at(Level.INFO).log("Slot Type: " + slotType);
                            LOGGER.at(Level.INFO).log("Slot Index: " + change.getSlotIndex());
                            LOGGER.at(Level.INFO).log("Action: " + action);
                            
                            // Show armor slot name if it's armor
                            if (change.getSlotType() == org.hytaledevlib.lib.EquipmentHelper.EquipmentSlotType.ARMOR) {
                                com.hypixel.hytale.protocol.ItemArmorSlot armorSlot = change.getArmorSlot();
                                if (armorSlot != null) {
                                    LOGGER.at(Level.INFO).log("Armor Slot: " + armorSlot.name() + " (Head/Chest/Hands/Legs)");
                                }
                            }
                            
                            // Show old item
                            if (change.getOldItemId() != null) {
                                LOGGER.at(Level.INFO).log("Old Item: " + change.getOldItemId());
                                LOGGER.at(Level.INFO).log("Old Quantity: " + change.getOldQuantity());
                            } else {
                                LOGGER.at(Level.INFO).log("Old Item: (empty)");
                            }
                            
                            // Show new item
                            if (change.getNewItemId() != null) {
                                LOGGER.at(Level.INFO).log("New Item: " + change.getNewItemId());
                                LOGGER.at(Level.INFO).log("New Quantity: " + change.getNewQuantity());
                            } else {
                                LOGGER.at(Level.INFO).log("New Item: (empty)");
                            }
                            
                            // EXAMPLE: Cancel equipping specific items (uncomment to test)
                            // Prevent equipping any item with "Leather" in the name
                            
                            if (change.isEquipping() && change.getNewItemId() != null) {
                                if (change.getNewItemId().contains("Armor_Adamantite_Head")) {
                                    LOGGER.at(Level.INFO).log("CANCELLED: Cannot equip leather items!");
                                    change.setCancelled(true);
                                }
                            }
                            
                            
                            LOGGER.at(Level.INFO).log("---");
                        });
                        break;
                    }
                }
            }
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
        org.hytaledevlib.lib.EventHelper.onItemDrop(this, (itemId, quantity, playerEntity) -> {
            String playerName = playerEntity != null ? org.hytaledevlib.lib.EntityHelper.getName(playerEntity) : "Unknown";
            LOGGER.at(Level.INFO).log("[EventTest] " + playerName + " dropped: " + quantity + "x " + itemId);
        });
        
        // Test onItemPickup - WORKS
        org.hytaledevlib.lib.EventHelper.onItemPickup(this, (itemId, quantity, playerEntity) -> {
            String playerName = playerEntity != null ? org.hytaledevlib.lib.EntityHelper.getName(playerEntity) : "Unknown";
            LOGGER.at(Level.INFO).log("[EventTest] " + playerName + " picked up: " + quantity + "x " + itemId);
        });
        
        // Test onCraftRecipe - Testing inventory transaction detection
        org.hytaledevlib.lib.EventHelper.onCraftRecipe(this, (outputItemId, quantity, playerEntity) -> {
            String playerName = playerEntity != null ? org.hytaledevlib.lib.EntityHelper.getName(playerEntity) : "Unknown";
            LOGGER.at(Level.INFO).log("[EventTest] " + playerName + " crafted: " + quantity + "x " + outputItemId);
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
        
        // Test onBlockDamage - Drain health and stamina when damaging/mining blocks
       /** org.hytaledevlib.lib.EcsEventHelper.onBlockDamage(world, (position, blockTypeId, currentDamage, damage, itemInHand, playerEntity) -> {
            if (playerEntity != null) {
                // Drain 5 health and stamina per damage tick
                float newHealth = org.hytaledevlib.lib.StatsHelper.addStat(playerEntity, "Health", -5.0f);
                float newStamina = org.hytaledevlib.lib.StatsHelper.addStat(playerEntity, "Stamina", -5.0f);
                
                LOGGER.at(Level.INFO).log("[StatsTest] Block damaged: " + blockTypeId + 
                    " | Damage: " + damage + "/" + currentDamage +
                    " | Health: " + newHealth + " | Stamina: " + newStamina);
            }
        });
        */
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
        
        // Test onBlockDamage - Combined mining speed multiplier and cooldown modification
        org.hytaledevlib.lib.EcsEventHelper.onBlockDamage(world, (context) -> {
            String gatherType = context.getGatherType();
            String tool = context.getItemInHand();
            
            // Safety check for player entity
            if (context.getPlayerEntity() == null) {
                return;
            }
            
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(context.getPlayerEntity());
            
            LOGGER.at(Level.INFO).log("🔨 onBlockDamage fired! Player: " + playerName + ", Tool: " + tool + ", GatherType: " + gatherType);
            
            // TEST 1: Pickaxe works faster on "Rocks" - use high damage multiplier
            if ("Tool_Pickaxe_Crude".equals(tool) && "Rocks".equals(gatherType)) {
                // Use very high damage multiplier so blocks break in 1-2 hits
                context.setMiningSpeedMultiplier(5.0f);
                
                LOGGER.at(Level.INFO).log("⚡ SUPER PICKAXE! Player " + playerName + " mining " + context.getBlockTypeId());
                LOGGER.at(Level.INFO).log("   5x damage multiplier - blocks break in 1-2 hits!");
            }
            
            // TEST 2: Axe works faster on wood
            if ("Tool_Axe_Crude".equals(tool) && "Wood".equals(gatherType)) {
                context.setMiningSpeedMultiplier(3.0f);
                
                LOGGER.at(Level.INFO).log("🪓 FAST AXE! Player " + playerName + " chopping " + context.getBlockTypeId());
                LOGGER.at(Level.INFO).log("   3x damage multiplier for wood!");
            }
        });
        
        // Test onZoneDiscovery - ECS event (zone exploration) with player entity
        org.hytaledevlib.lib.EcsEventHelper.onZoneDiscovery(world, (discoveryInfo, playerEntity) -> {
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(playerEntity);
            LOGGER.at(Level.INFO).log("[EcsEventTest] Zone discovered by " + playerName + ": " + discoveryInfo.zoneName());
            LOGGER.at(Level.INFO).log("  Region: " + discoveryInfo.regionName());
            LOGGER.at(Level.INFO).log("  Major: " + discoveryInfo.major());
            if (discoveryInfo.icon() != null) {
                LOGGER.at(Level.INFO).log("  Icon: " + discoveryInfo.icon());
            }
        });
        
        // Test onBlockInteract - ECS event (also handles existing container registration) with player entity
        org.hytaledevlib.lib.EcsEventHelper.onBlockInteract(world, (position, blockTypeId, playerEntity) -> {
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(playerEntity);
            LOGGER.at(Level.INFO).log("[EcsEventTest] Block interacted by " + playerName + ": " + blockTypeId + " at " + position);
            
            // Check if this is the well bench - fill buckets in output slot
            if ("Bench_Well".equals(blockTypeId)) {
                try {
                    LOGGER.at(Level.INFO).log("========================================");
                    LOGGER.at(Level.INFO).log("🪣 WELL BENCH DETECTED!");
                    LOGGER.at(Level.INFO).log("Position: " + position);
                    LOGGER.at(Level.INFO).log("Block Type: " + blockTypeId);
                    
                    // Get the block state
                    LOGGER.at(Level.INFO).log("Step 1: Getting block state...");
                    com.hypixel.hytale.server.core.universe.world.meta.BlockState state = 
                        org.hytaledevlib.lib.BlockStateHelper.ensureState(world, position.x, position.y, position.z);
                    
                    if (state == null) {
                        LOGGER.at(Level.WARNING).log("  ❌ Block state is NULL!");
                        return;
                    }
                    LOGGER.at(Level.INFO).log("  ✓ Got block state: " + state.getClass().getSimpleName());
                    
                    if (!(state instanceof com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState)) {
                        LOGGER.at(Level.WARNING).log("  ❌ Block state is NOT ProcessingBenchState!");
                        LOGGER.at(Level.WARNING).log("  Actual type: " + state.getClass().getName());
                        return;
                    }
                    LOGGER.at(Level.INFO).log("  ✓ State is ProcessingBenchState");
                    
                    com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState wellState = 
                        (com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState) state;
                    
                    // Get the combined container (includes input, fuel, and output)
                    LOGGER.at(Level.INFO).log("Step 2: Getting combined item container...");
                    com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer combinedContainer = wellState.getItemContainer();
                    if (combinedContainer == null) {
                        LOGGER.at(Level.WARNING).log("  ❌ Combined container is NULL!");
                        return;
                    }
                    
                    // Access individual containers
                    LOGGER.at(Level.INFO).log("  ✓ Got combined container!");
                    LOGGER.at(Level.INFO).log("  Number of sub-containers: " + combinedContainer.getContainersSize());
                    
                    // The output container is the last one in the combined container
                    // Order: fuel (0), input (1), output (2)
                    com.hypixel.hytale.server.core.inventory.container.ItemContainer container = 
                        combinedContainer.getContainer(2); // Output container
                    if (container == null) {
                        LOGGER.at(Level.WARNING).log("  ❌ Output container is NULL!");
                        return;
                    }
                    
                    short capacity = container.getCapacity();
                    LOGGER.at(Level.INFO).log("  ✓ Got container! Capacity: " + capacity);
                    
                    // Log ALL slots and their contents
                    LOGGER.at(Level.INFO).log("Step 3: Scanning all slots...");
                    for (short i = 0; i < capacity; i++) {
                        com.hypixel.hytale.server.core.inventory.ItemStack stack = container.getItemStack(i);
                        if (stack != null) {
                            LOGGER.at(Level.INFO).log("  Slot " + i + ": " + stack.getItemId() + " x" + stack.getQuantity());
                        } else {
                            LOGGER.at(Level.INFO).log("  Slot " + i + ": EMPTY");
                        }
                    }
                    
                    // Check all slots for custom well buckets and convert them
                    LOGGER.at(Level.INFO).log("Step 4: Looking for Container_Bucket_Filled_Water_Well...");
                    int bucketsFound = 0;
                    int bucketsFilled = 0;
                    
                    for (short i = 0; i < capacity; i++) {
                        com.hypixel.hytale.server.core.inventory.ItemStack stack = container.getItemStack(i);
                        if (stack != null && "Container_Bucket_Filled_Water_Well".equals(stack.getItemId())) {
                            bucketsFound++;
                            LOGGER.at(Level.INFO).log("  ✓ FOUND well bucket in slot " + i + "!");
                            LOGGER.at(Level.INFO).log("    Current ID: " + stack.getItemId());
                            LOGGER.at(Level.INFO).log("    Quantity: " + stack.getQuantity());
                            
                            try {
                                LOGGER.at(Level.INFO).log("    Step 4a: Temporarily disabling output filter...");
                                // Change filter from ALLOW_OUTPUT_ONLY to ALLOW_ALL to allow modifications
                                container.setGlobalFilter(com.hypixel.hytale.server.core.inventory.container.filter.FilterType.ALLOW_ALL);
                                LOGGER.at(Level.INFO).log("    ✓ Filter changed to ALLOW_ALL");
                                
                                LOGGER.at(Level.INFO).log("    Step 4b: Creating vanilla water-filled bucket...");
                                com.hypixel.hytale.server.core.inventory.ItemStack emptyBucket = 
                                    new com.hypixel.hytale.server.core.inventory.ItemStack("Container_Bucket", stack.getQuantity());
                                com.hypixel.hytale.server.core.inventory.ItemStack filledBucket = emptyBucket.withState("Filled_Water");
                                LOGGER.at(Level.INFO).log("    ✓ Created filled bucket: " + filledBucket.getItemId());
                                
                                LOGGER.at(Level.INFO).log("    Step 4c: Setting filled bucket in slot " + i + "...");
                                container.setItemStackForSlot(i, filledBucket);
                                LOGGER.at(Level.INFO).log("    ✓ Item set in slot!");
                                
                                LOGGER.at(Level.INFO).log("    Step 4d: Restoring output filter...");
                                // Restore the output-only filter
                                container.setGlobalFilter(com.hypixel.hytale.server.core.inventory.container.filter.FilterType.ALLOW_OUTPUT_ONLY);
                                LOGGER.at(Level.INFO).log("    ✓ Filter restored to ALLOW_OUTPUT_ONLY");
                                
                                // Verify the change persisted
                                com.hypixel.hytale.server.core.inventory.ItemStack verifyStack = container.getItemStack(i);
                                LOGGER.at(Level.INFO).log("    Step 4e: Verifying change...");
                                if (verifyStack != null && filledBucket.getItemId().equals(verifyStack.getItemId())) {
                                    LOGGER.at(Level.INFO).log("    ✓ SUCCESS! Slot now contains: " + verifyStack.getItemId());
                                    bucketsFilled++;
                                    LOGGER.at(Level.INFO).log("  💧 Successfully converted bucket in slot " + i + "!");
                                } else if (verifyStack != null) {
                                    LOGGER.at(Level.WARNING).log("    ❌ Change reverted! Slot contains: " + verifyStack.getItemId());
                                } else {
                                    LOGGER.at(Level.WARNING).log("    ❌ Slot is now empty!");
                                }
                            } catch (Exception e) {
                                LOGGER.at(Level.WARNING).log("  ❌ Failed to convert bucket in slot " + i + ": " + e.getMessage());
                                e.printStackTrace();
                                // Try to restore filter even if there was an error
                                try {
                                    container.setGlobalFilter(com.hypixel.hytale.server.core.inventory.container.filter.FilterType.ALLOW_OUTPUT_ONLY);
                                } catch (Exception ex) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    
                    LOGGER.at(Level.INFO).log("Step 5: Summary");
                    LOGGER.at(Level.INFO).log("  Buckets found: " + bucketsFound);
                    LOGGER.at(Level.INFO).log("  Buckets filled: " + bucketsFilled);
                    
                    if (bucketsFound > 0) {
                        if (bucketsFilled > 0) {
                            LOGGER.at(Level.INFO).log("  Marking state for save...");
                            org.hytaledevlib.lib.BlockStateHelper.markNeedsSave(wellState);
                            LOGGER.at(Level.INFO).log("  ✓ State marked for save");
                            org.hytaledevlib.lib.PlayerHelper.sendMessage(playerEntity, "§aFilled " + bucketsFilled + " bucket(s) with water!");
                        }
                    } else {
                        LOGGER.at(Level.INFO).log("  No Container_Bucket_Filled_Water_Well found in any slot");
                    }
                    
                    LOGGER.at(Level.INFO).log("========================================");
                    
                    // Track that this player has this well open
                    openWells.put(playerName, position);
                    LOGGER.at(Level.INFO).log("📝 Tracking well at " + position + " for player " + playerName);
                    
                    // Schedule removal after 10 seconds (player likely closed UI)
                    org.hytaledevlib.lib.WorldHelper.waitTicks(world, 200, () -> {
                        openWells.remove(playerName);
                        LOGGER.at(Level.INFO).log("⏱️ Stopped tracking well for " + playerName + " (timeout)");
                    });
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("❌ ERROR processing well bench: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
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
                    
                    // TEST: Cancel all transactions for player "Se7enity"
                    // Get all players and find the one interacting with this container
                    for (com.hypixel.hytale.server.core.entity.Entity entity : org.hytaledevlib.lib.EntityHelper.getEntities(world)) {
                        if (org.hytaledevlib.lib.EntityHelper.isPlayer(entity)) {
                            String name = org.hytaledevlib.lib.EntityHelper.getName(entity);
                            if ("Se7enity".equals(name)) {
                                // Cancel the transaction - Se7enity cannot add or remove items from containers
                                transaction.setCancelled(true);
                                LOGGER.at(Level.INFO).log("❌ TRANSACTION CANCELLED - Player Se7enity cannot use containers!");
                                org.hytaledevlib.lib.PlayerHelper.sendMessage(entity, "You are not allowed to use containers!");
                                break;
                            }
                        }
                    }
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
        
        // Test onGameModeChange - Log when players change game modes
        org.hytaledevlib.lib.EcsEventHelper.onGameModeChange(world, (entity, gameMode) -> {
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(entity);
            LOGGER.at(Level.INFO).log("[EcsEventTest] 🎮 " + playerName + " changed to " + gameMode + " mode");
        });
        
        // Test onHotbarSwitch - Log when players switch hotbar slots
        org.hytaledevlib.lib.EcsEventHelper.onHotbarSwitch(world, (entity, previousSlot, newSlot) -> {
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(entity);
            LOGGER.at(Level.INFO).log("[EcsEventTest] 🎯 " + playerName + " switched from slot " + previousSlot + " to slot " + newSlot);
        });
        
        // Test onMoonPhaseChange - Log moon phase changes
        org.hytaledevlib.lib.EcsEventHelper.onMoonPhaseChange(world, (moonPhase) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] 🌙 Moon phase changed to: " + moonPhase);
        });
        
        // Test onChunkSave - Log chunk saves
        org.hytaledevlib.lib.EcsEventHelper.onChunkSave(world, (chunkIndex) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] 💾 Chunk saved: " + chunkIndex);
        });
        
        // Test onChunkUnload - Log chunk unloads
        org.hytaledevlib.lib.EcsEventHelper.onChunkUnload(world, (chunkIndex) -> {
            LOGGER.at(Level.INFO).log("[EcsEventTest] 📤 Chunk unloaded: " + chunkIndex);
        });
        
        LOGGER.at(Level.INFO).log("ECS EventHelper tests registered!");
        LOGGER.at(Level.INFO).log("  ✓ Block breaking (filters out Empty blocks)");
        LOGGER.at(Level.INFO).log("  ✓ Block placing");
        LOGGER.at(Level.INFO).log("  ✓ Block damage (mining progress tracking)");
        LOGGER.at(Level.INFO).log("  ✓ Zone discovery (map exploration)");
        LOGGER.at(Level.INFO).log("  ✓ Block interaction (existing container registration)");
        LOGGER.at(Level.INFO).log("  ✓ Game mode changes");
        LOGGER.at(Level.INFO).log("  ✓ Hotbar slot switching");
        LOGGER.at(Level.INFO).log("  ✓ Moon phase changes");
        LOGGER.at(Level.INFO).log("  ✓ Chunk save/unload");
    }
    
    /**
     * Register ParticleHelper test - spawns particles at Se7enity's feet when breaking blocks.
     */
    private void registerParticleHelperTest(World world) {
        LOGGER.at(Level.INFO).log("Registering ParticleHelper test...");
        
        // Spawn particles at Se7enity's feet when they break a block
        org.hytaledevlib.lib.EcsEventHelper.onBlockBreak(world, (position, blockTypeId, playerEntity) -> {
            if (playerEntity != null) {
                String playerName = org.hytaledevlib.lib.EntityHelper.getName(playerEntity);
                if ("Se7enity".equals(playerName)) {
                    // Spawn impact particles at player's feet (temporary effect)
                    org.hytaledevlib.lib.ParticleHelper.spawnParticleAtEntityFeet(
                        world, 
                        "Impact_Fire", 
                        playerEntity
                    );
                    
                    // Spawn sparkle dust at the broken block position
                    org.hytaledevlib.lib.ParticleHelper.spawnParticleAtBlock(
                        world,
                        "Dust_Sparkles",
                        position
                    );
                    
                    // Spawn a large explosion particle above the block
                    com.hypixel.hytale.math.vector.Vector3i abovePos = new com.hypixel.hytale.math.vector.Vector3i(
                        position.getX(), position.getY() + 1, position.getZ()
                    );
                    com.hypixel.hytale.math.vector.Vector3d explosionPos = new com.hypixel.hytale.math.vector.Vector3d(
                        abovePos.getX() + 0.5, 
                        abovePos.getY() + 0.5, 
                        abovePos.getZ() + 0.5
                    );
                    org.hytaledevlib.lib.ParticleHelper.spawnParticle(
                        world,
                        "Explosion_Medium",
                        explosionPos,
                        2.0f // Double size
                    );
                    
                    // Play 3D impact sound at the same position (sounds like an explosion)
                    org.hytaledevlib.lib.SoundHelper.playSound3D(
                        world,
                        "SFX_Golem_Earth_Slam_Impact",
                        explosionPos,
                        1.0f, // Normal volume
                        1.0f  // Normal pitch
                    );
                    
                    LOGGER.at(Level.INFO).log("✨ Spawned particles and sound for Se7enity breaking " + blockTypeId);
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("ParticleHelper test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Sparkle particles spawn at Se7enity's feet when breaking blocks");
        LOGGER.at(Level.INFO).log("  ✓ Particles at broken block position");
        LOGGER.at(Level.INFO).log("  ✓ Large particles (2x scale) above broken block");
        
        // Log all available particle system IDs
        logAvailableParticleSystems();
        
        // Log all available sound event IDs
        logAvailableSoundEvents();
    }
    
    /**
     * Log all available particle system IDs from Hytale's asset registry.
     */
    private void logAvailableParticleSystems() {
        try {
            LOGGER.at(Level.INFO).log("=== Available Particle Systems ===");
            
            // Get the ParticleSystem asset map - getAssetMap() returns the internal Map
            var defaultAssetMap = com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem.getAssetMap();
            
            if (defaultAssetMap != null) {
                // getAssetMap() returns an unmodifiable Map<K, T>
                java.util.Map<String, com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem> assetMap = 
                    defaultAssetMap.getAssetMap();
                
                int totalCount = assetMap.size();
                LOGGER.at(Level.INFO).log("Found " + totalCount + " particle systems:");
                
                // Write all particle IDs to a file
                java.util.List<String> particleIds = new java.util.ArrayList<>(assetMap.keySet());
                java.util.Collections.sort(particleIds); // Sort alphabetically
                
                try {
                    java.nio.file.Path outputPath = java.nio.file.Paths.get("ParticleList.md");
                    java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(outputPath);
                    
                    writer.write("# Hytale Particle Systems\n\n");
                    writer.write("Total particle systems: " + totalCount + "\n\n");
                    writer.write("## Available Particle IDs\n\n");
                    
                    for (String particleId : particleIds) {
                        writer.write("- `" + particleId + "`\n");
                    }
                    
                    writer.close();
                    LOGGER.at(Level.INFO).log("✓ Exported " + totalCount + " particle systems to ParticleList.md");
                } catch (java.io.IOException e) {
                    LOGGER.at(Level.WARNING).log("Failed to write ParticleList.md: " + e.getMessage());
                }
                
                // Log first 50 to console
                int count = 0;
                for (String particleId : particleIds) {
                    LOGGER.at(Level.INFO).log("  - " + particleId);
                    count++;
                    if (count >= 50) {
                        LOGGER.at(Level.INFO).log("  ... and " + (totalCount - 50) + " more (see ParticleList.md)");
                        break;
                    }
                }
            } else {
                LOGGER.at(Level.WARNING).log("ParticleSystem asset map is null!");
            }
            
            LOGGER.at(Level.INFO).log("=================================");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to list particle systems: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Log all available sound event IDs from Hytale's asset registry.
     */
    private void logAvailableSoundEvents() {
        try {
            LOGGER.at(Level.INFO).log("=== Available Sound Events ===");
            
            // Get the SoundEvent asset map
            var defaultAssetMap = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap();
            
            if (defaultAssetMap != null) {
                // getAssetMap() returns an unmodifiable Map<K, T>
                java.util.Map<String, com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent> assetMap = 
                    defaultAssetMap.getAssetMap();
                
                int totalCount = assetMap.size();
                LOGGER.at(Level.INFO).log("Found " + totalCount + " sound events:");
                
                // Write all sound event IDs to a file
                java.util.List<String> soundIds = new java.util.ArrayList<>(assetMap.keySet());
                java.util.Collections.sort(soundIds); // Sort alphabetically
                
                try {
                    java.nio.file.Path outputPath = java.nio.file.Paths.get("SoundList.md");
                    java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(outputPath);
                    
                    writer.write("# Hytale Sound Events\n\n");
                    writer.write("Total sound events: " + totalCount + "\n\n");
                    writer.write("## Available Sound Event IDs\n\n");
                    
                    for (String soundId : soundIds) {
                        writer.write("- `" + soundId + "`\n");
                    }
                    
                    writer.close();
                    LOGGER.at(Level.INFO).log("✓ Exported " + totalCount + " sound events to SoundList.md");
                } catch (java.io.IOException e) {
                    LOGGER.at(Level.WARNING).log("Failed to write SoundList.md: " + e.getMessage());
                }
                
                // Log first 50 to console
                int count = 0;
                for (String soundId : soundIds) {
                    LOGGER.at(Level.INFO).log("  - " + soundId);
                    count++;
                    if (count >= 50) {
                        LOGGER.at(Level.INFO).log("  ... and " + (totalCount - 50) + " more (see SoundList.md)");
                        break;
                    }
                }
            } else {
                LOGGER.at(Level.WARNING).log("SoundEvent asset map is null!");
            }
            
            LOGGER.at(Level.INFO).log("=================================");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to list sound events: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Register mob loot test - adds chance-based drops to Fen_Stalker.
     */
    private void registerMobLootTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering Mob Loot test...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Add chance-based drops to Fen_Stalker (all with drop chances)
        org.hytaledevlib.lib.LootHelper.registerMobLoot(world, "Fen_Stalker", (position) -> {
            return java.util.Arrays.asList(
                org.hytaledevlib.lib.LootHelper.ItemDrop.withChance("Weapon_Longsword_Adamantite", 1, 0.25f)
            );
        });
        
        LOGGER.at(Level.INFO).log("✅ Mob loot test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Fen_Stalker chance-based drops:");
        LOGGER.at(Level.INFO).log("    - 75%: 1x Weapon_Longsword_Adamantite");
        LOGGER.at(Level.INFO).log("    - 50%: 3x Ingredient_Diamond");
        LOGGER.at(Level.INFO).log("    - 25%: 5x Ingredient_Gold (with scatter)");
        LOGGER.at(Level.INFO).log("    - 10%: 1x Weapon_Sword_Iron (rare)");
        LOGGER.at(Level.INFO).log("  ✓ Kill multiple Fen_Stalkers to see different drop combinations!");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
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
    
    /**
     * Register EquipmentHelper test - tracks player armor and equipment changes.
     */
    private void registerEquipmentHelperTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering EquipmentHelper test...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Register equipment tracking when players join
        org.hytaledevlib.lib.EventHelper.onPlayerJoinWorld(this, joinedWorld -> {
            // Get the player who just joined
            for (com.hypixel.hytale.server.core.entity.Entity entity : joinedWorld.getPlayers()) {
                if (entity instanceof com.hypixel.hytale.server.core.entity.LivingEntity livingEntity) {
                    LOGGER.at(Level.INFO).log("Registering equipment tracking for player: " + org.hytaledevlib.lib.EntityHelper.getName(entity));
                    
                    org.hytaledevlib.lib.EquipmentHelper.onEquipmentChange(joinedWorld, livingEntity, (change, container) -> {
                        String slotType = change.getSlotType().toString();
                        String action = change.isEquipping() ? "EQUIPPED" : "UNEQUIPPED";
                        
                        LOGGER.at(Level.INFO).log("EQUIPMENT CHANGE DETECTED");
                        LOGGER.at(Level.INFO).log("Slot Type: " + slotType);
                        LOGGER.at(Level.INFO).log("Slot Index: " + change.getSlotIndex());
                        LOGGER.at(Level.INFO).log("Action: " + action);
                        
                        // Show armor slot name if it's armor
                        if (change.getSlotType() == org.hytaledevlib.lib.EquipmentHelper.EquipmentSlotType.ARMOR) {
                            com.hypixel.hytale.protocol.ItemArmorSlot armorSlot = change.getArmorSlot();
                            if (armorSlot != null) {
                                LOGGER.at(Level.INFO).log("Armor Slot: " + armorSlot.name() + " (Head/Chest/Hands/Legs)");
                            }
                        }
                        
                        // Show old item
                        if (change.getOldItemId() != null) {
                            LOGGER.at(Level.INFO).log("Old Item: " + change.getOldItemId());
                            LOGGER.at(Level.INFO).log("Old Quantity: " + change.getOldQuantity());
                        } else {
                            LOGGER.at(Level.INFO).log("Old Item: (empty)");
                        }
                        
                        // Show new item
                        if (change.getNewItemId() != null) {
                            LOGGER.at(Level.INFO).log("New Item: " + change.getNewItemId());
                            LOGGER.at(Level.INFO).log("New Quantity: " + change.getNewQuantity());
                        } else {
                            LOGGER.at(Level.INFO).log("New Item: (empty)");
                        }
                        
                        LOGGER.at(Level.INFO).log("---");
                    });
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("✅ EquipmentHelper test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Will track equipment for players when they join");
        LOGGER.at(Level.INFO).log("  ✓ Monitoring:");
        LOGGER.at(Level.INFO).log("    - Armor slots (Head, Chest, Hands, Legs)");
        LOGGER.at(Level.INFO).log("    - Utility slots (offhand/tools)");
        LOGGER.at(Level.INFO).log("    - Tool slots");
        LOGGER.at(Level.INFO).log("    - Hotbar and storage");
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("📝 To test:");
        LOGGER.at(Level.INFO).log("  1. Equip or unequip armor pieces");
        LOGGER.at(Level.INFO).log("  2. Change items in your offhand/utility slots");
        LOGGER.at(Level.INFO).log("  3. Switch tools");
        LOGGER.at(Level.INFO).log("  4. Watch for EQUIPMENT CHANGE logs");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Register DeathHelper test to track entity deaths.
     */
    private void registerDeathHelperTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering DeathHelper test...");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
        
        org.hytaledevlib.lib.DeathHelper.onEntityDeath(world, (death) -> {
            // Handle mob loot drops first
            org.hytaledevlib.lib.LootHelper.handleMobDeath(death, world);
            
            com.hypixel.hytale.math.vector.Vector3d pos = death.getPosition();
            
            LOGGER.at(Level.INFO).log("═══════════════════════════════════════");
            LOGGER.at(Level.INFO).log("💀 ENTITY DEATH DETECTED");
            LOGGER.at(Level.INFO).log("═══════════════════════════════════════");
            
            // Log who died (using helper methods)
            String entityName = death.getEntityName();
            String entityType = death.isPlayer() ? "Player" : "NPC";
            LOGGER.at(Level.INFO).log("☠️  Entity Died: " + entityName + " (" + entityType + ")");
            
            // Log death position
            if (pos != null) {
                LOGGER.at(Level.INFO).log("📍 Death Position: X=" + String.format("%.2f", pos.x) + 
                    " Y=" + String.format("%.2f", pos.y) + 
                    " Z=" + String.format("%.2f", pos.z));
            }
            
            // Log what killed the entity (using helper methods)
            if (death.wasKilledByEntity()) {
                LOGGER.at(Level.INFO).log("⚔️  Killed by: ENTITY");
                String killerName = death.getKillerName();
                String killerType = death.isKillerPlayer() ? "Player" : "NPC";
                LOGGER.at(Level.INFO).log("   Killer: " + killerName + " (" + killerType + ")");
            } else if (death.wasKilledByProjectile()) {
                LOGGER.at(Level.INFO).log("🏹 Killed by: PROJECTILE");
                String shooterName = death.getKillerName();
                String shooterType = death.isKillerPlayer() ? "Player" : "NPC";
                LOGGER.at(Level.INFO).log("   Shooter: " + shooterName + " (" + shooterType + ")");
            } else if (death.wasKilledByEnvironment()) {
                LOGGER.at(Level.INFO).log("🌍 Killed by: ENVIRONMENT");
                String envType = death.getEnvironmentType();
                if (envType != null) {
                    LOGGER.at(Level.INFO).log("   Type: " + envType);
                }
            } else {
                LOGGER.at(Level.INFO).log("❓ Killed by: UNKNOWN/OTHER");
            }
            
            // Log damage info if available
            com.hypixel.hytale.server.core.modules.entity.damage.Damage damageInfo = death.getDamageInfo();
            if (damageInfo != null) {
                LOGGER.at(Level.INFO).log("💥 Damage Amount: " + damageInfo.getAmount());
                com.hypixel.hytale.server.core.modules.entity.damage.DamageCause cause = damageInfo.getCause();
                if (cause != null) {
                    LOGGER.at(Level.INFO).log("💢 Damage Cause: " + cause.getId());
                }
            }
            
            LOGGER.at(Level.INFO).log("═══════════════════════════════════════");
        });
        
        LOGGER.at(Level.INFO).log("✅ DeathHelper test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Tracking all entity deaths");
        LOGGER.at(Level.INFO).log("  ✓ Logs death position");
        LOGGER.at(Level.INFO).log("  ✓ Logs killer information");
        LOGGER.at(Level.INFO).log("  ✓ Logs damage source and cause");
        LOGGER.at(Level.INFO).log("");
        LOGGER.at(Level.INFO).log("📝 To test:");
        LOGGER.at(Level.INFO).log("  1. Kill an NPC or mob");
        LOGGER.at(Level.INFO).log("  2. Die from fall damage");
        LOGGER.at(Level.INFO).log("  3. Die from drowning");
        LOGGER.at(Level.INFO).log("  4. Watch for ENTITY DEATH logs");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    private void registerStatsHelperTest(World world) {
        StatsHelperTest.register(this, world);
    }
    
    /**
     * Test game mode switching for player Se7enity.
     * Switches to CREATIVE after 100 ticks, then back to ADVENTURE after another 100 ticks.
     */
    private void testGameModeSwitching(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Starting GameMode switching test for Se7enity");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Find player Se7enity
        com.hypixel.hytale.server.core.entity.Entity targetPlayer = null;
        for (com.hypixel.hytale.server.core.entity.Entity entity : org.hytaledevlib.lib.EntityHelper.getEntities(world)) {
            if (org.hytaledevlib.lib.EntityHelper.isPlayer(entity)) {
                String name = org.hytaledevlib.lib.EntityHelper.getName(entity);
                if ("Se7enity".equals(name)) {
                    targetPlayer = entity;
                    break;
                }
            }
        }
        
        if (targetPlayer == null) {
            LOGGER.at(Level.WARNING).log("Player Se7enity not found! Test cancelled.");
            return;
        }
        
        final com.hypixel.hytale.server.core.entity.Entity player = targetPlayer;
        
        // Get current game mode
        GameMode currentMode = org.hytaledevlib.lib.PlayerHelper.getGameMode(player);
        LOGGER.at(Level.INFO).log("Se7enity's current game mode: " + (currentMode != null ? currentMode.name() : "UNKNOWN"));
        
        // Get game mode instances using valueOf
        GameMode creativeMode;
        GameMode adventureMode;
        try {
            // Use correct capitalization: Creative and Adventure (not CREATIVE/ADVENTURE)
            creativeMode = GameMode.valueOf("Creative");
            adventureMode = GameMode.valueOf("Adventure");
            LOGGER.at(Level.INFO).log("Successfully created GameMode instances");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to get GameMode instances: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Switch to CREATIVE after 100 ticks
        LOGGER.at(Level.INFO).log("⏱️ Will switch Se7enity to CREATIVE in 100 ticks (5 seconds)...");
        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
            boolean success = org.hytaledevlib.lib.PlayerHelper.setGameMode(world, player, creativeMode);
            if (success) {
                LOGGER.at(Level.INFO).log("✅ Se7enity switched to CREATIVE mode!");
                org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "You are now in CREATIVE mode!");
                
                // Show MAJOR title for creative mode (2 seconds)
                org.hytaledevlib.lib.TitleHelper.showMajorTitle(
                    player,
                    "Creative Mode",
                    "Build freely!",
                    2.0f
                );
                LOGGER.at(Level.INFO).log("📺 Showed MAJOR title for Creative mode");
                
                // Switch back to ADVENTURE after another 100 ticks
                LOGGER.at(Level.INFO).log("⏱️ Will switch Se7enity back to ADVENTURE in 100 ticks (5 seconds)...");
                org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
                    boolean success2 = org.hytaledevlib.lib.PlayerHelper.setGameMode(world, player, adventureMode);
                    if (success2) {
                        LOGGER.at(Level.INFO).log("✅ Se7enity switched back to ADVENTURE mode!");
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "You are now in ADVENTURE mode!");
                        
                        // Show MINOR title for adventure mode (2 seconds)
                        org.hytaledevlib.lib.TitleHelper.showMinorTitle(
                            player,
                            "Adventure Mode",
                            "Explore the world",
                            2.0f
                        );
                        LOGGER.at(Level.INFO).log("📺 Showed MINOR title for Adventure mode");
                        
                        // Play 2D sound test - only this player hears it
                        org.hytaledevlib.lib.SoundHelper.playSound2DToPlayer(
                            world,
                            "SFX_Axe_Special_Impact",
                            player,
                            1.0f, // Normal volume
                            1.0f  // Normal pitch
                        );
                        LOGGER.at(Level.INFO).log("🔊 Played 2D sound to Se7enity");
                        
                        // Wait another 100 ticks then show boss title
                        LOGGER.at(Level.INFO).log("⏱️ Will show boss title in 100 ticks (5 seconds)...");
                        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
                            org.hytaledevlib.lib.TitleHelper.showBossTitle(
                                player,
                                "Ancient Dragon",
                                "Prepare for battle!"
                            );
                            LOGGER.at(Level.INFO).log("📺 Showed BOSS title (4 seconds)");
                            
                            // Wait another 100 ticks then show title with icon
                            LOGGER.at(Level.INFO).log("⏱️ Will show title with icon in 100 ticks (5 seconds)...");
                            org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
                                org.hytaledevlib.lib.TitleHelper.showTitleWithIcon(
                                    player,
                                    true,  // Major title
                                    "Achievement Unlocked!",
                                    "HytaleDevLib Master",
                                    "D:\\Documents\\Windsurf Projects\\Hytale Mods\\GroundItems\\Hytale-Example-Project\\HTDL.png",
                                    2.0f
                                );
                                LOGGER.at(Level.INFO).log("📺 Showed title with HTDL.png icon (2 seconds)");
                            });
                        });
                    } else {
                        LOGGER.at(Level.WARNING).log("❌ Failed to switch Se7enity back to ADVENTURE mode");
                    }
                });
            } else {
                LOGGER.at(Level.WARNING).log("❌ Failed to switch Se7enity to CREATIVE mode");
            }
        });
        
        LOGGER.at(Level.INFO).log("GameMode switching test scheduled!");
        LOGGER.at(Level.INFO).log("========================================");
    }
    
    /**
     * Test teleporting all item entities to the player and setting game mode to adventure.
     */
    private void testItemEntityTeleport(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Starting Item Entity Teleport Test");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Find player Se7enity
        com.hypixel.hytale.server.core.entity.Entity targetPlayer = null;
        for (com.hypixel.hytale.server.core.entity.Entity entity : org.hytaledevlib.lib.EntityHelper.getEntities(world)) {
            if (org.hytaledevlib.lib.EntityHelper.isPlayer(entity)) {
                String name = org.hytaledevlib.lib.EntityHelper.getName(entity);
                if ("Se7enity".equals(name)) {
                    targetPlayer = entity;
                    break;
                }
            }
        }
        
        if (targetPlayer == null) {
            LOGGER.at(Level.WARNING).log("Player Se7enity not found! Test cancelled.");
            return;
        }
        
        final com.hypixel.hytale.server.core.entity.Entity player = targetPlayer;
        
        // Wait 5 seconds before running the test
        LOGGER.at(Level.INFO).log("⏱️ Will teleport items and set adventure mode in 100 ticks (5 seconds)...");
        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
            // Get player position
            com.hypixel.hytale.math.vector.Vector3d playerPos = org.hytaledevlib.lib.EntityHelper.getPosition(player);
            if (playerPos == null) {
                LOGGER.at(Level.WARNING).log("❌ Could not get player position!");
                return;
            }
            
            // Count items before teleport
            int itemCount = org.hytaledevlib.lib.ItemHelper.countItemEntities(world);
            LOGGER.at(Level.INFO).log("📦 Found " + itemCount + " dropped items in the world");
            
            // Teleport all items to player
            int teleported = org.hytaledevlib.lib.ItemHelper.teleportAllItemEntities(world, playerPos);
            LOGGER.at(Level.INFO).log("✨ Teleported " + teleported + " items to player position!");
            
            // Set game mode to Adventure
            GameMode adventureMode;
            try {
                adventureMode = GameMode.valueOf("Adventure");
                boolean success = org.hytaledevlib.lib.PlayerHelper.setGameMode(world, player, adventureMode);
                if (success) {
                    LOGGER.at(Level.INFO).log("✅ Set Se7enity to ADVENTURE mode!");
                    org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "All items teleported to you! Adventure mode activated!");
                    
                    // Show title
                    org.hytaledevlib.lib.TitleHelper.showMajorTitle(
                        player,
                        "Item Magnet!",
                        "All items teleported to you",
                        3.0f
                    );
                    
                    // COMMENTED OUT: Old bucket transformation test - now using well bench instead
                    /*
                    // Give empty bucket
                    org.hytaledevlib.lib.InventoryHelper.giveItem(player, "Container_Bucket", 1);
                    LOGGER.at(Level.INFO).log("🪣 Gave empty bucket to player");
                    org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "You received an empty bucket! Hold it in your hand.");
                    
                    // After 100 ticks, change the bucket state in active hand to filled with water
                    LOGGER.at(Level.INFO).log("⏱️ Will fill bucket in active hand with water in 100 ticks...");
                    org.hytaledevlib.lib.WorldHelper.waitTicks(world, 100, () -> {
                        // Change the bucket's state in active hand to filled with water
                        boolean filled = org.hytaledevlib.lib.InventoryHelper.changeItemStateInActiveHand(
                            player,
                            "Filled_Water"
                        );
                        
                        if (filled) {
                            LOGGER.at(Level.INFO).log("💧 Successfully filled bucket in active hand with water!");
                            org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "Your bucket is now filled with water!");
                            
                            // Show minor title
                            org.hytaledevlib.lib.TitleHelper.showMinorTitle(
                                player,
                                "Bucket Filled!",
                                "Water bucket ready",
                                2.0f
                            );
                        } else {
                            LOGGER.at(Level.WARNING).log("❌ Failed to fill bucket - make sure you're holding it!");
                            org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "Could not fill bucket! Make sure you're holding it in your hand.");
                        }
                    });
                    */
                } else {
                    LOGGER.at(Level.WARNING).log("❌ Failed to set adventure mode");
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("❌ Error setting game mode: " + e.getMessage());
            }
        });
        
        LOGGER.at(Level.INFO).log("Item entity teleport test scheduled!");
        LOGGER.at(Level.INFO).log("========================================");
    }
    
    /**
     * Register fluid detection test - logs water blocks near the player every 50 ticks.
     */
    private void registerFluidDetectionTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering Fluid Detection test...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Wait 50 ticks before starting the recurring check
        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 50, () -> {
            LOGGER.at(Level.INFO).log("🌊 Starting fluid detection checks...");
            scheduleFluidCheck(world);
        });
        
        LOGGER.at(Level.INFO).log("✅ Fluid detection test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Will start checking in 50 ticks (2.5 seconds)");
        LOGGER.at(Level.INFO).log("  ✓ Then checks every 50 ticks for water blocks");
        LOGGER.at(Level.INFO).log("  ✓ Checks 5-block radius around each player");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Schedule a recurring fluid check every 50 ticks.
     */
    private void scheduleFluidCheck(World world) {
        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 50, () -> {
            // Find all players
            for (com.hypixel.hytale.server.core.entity.Entity entity : org.hytaledevlib.lib.EntityHelper.getEntities(world)) {
                if (org.hytaledevlib.lib.EntityHelper.isPlayer(entity)) {
                    String playerName = org.hytaledevlib.lib.EntityHelper.getName(entity);
                    com.hypixel.hytale.math.vector.Vector3d playerPos = org.hytaledevlib.lib.EntityHelper.getPosition(entity);
                    
                    if (playerPos == null) {
                        continue;
                    }
                    
                    // Check for water in a 5-block radius around the player
                    int radius = 5;
                    java.util.List<org.hytaledevlib.lib.BlockHelper.FluidPosition> waterBlocks = new java.util.ArrayList<>();
                    
                    int playerX = (int) playerPos.getX();
                    int playerY = (int) playerPos.getY();
                    int playerZ = (int) playerPos.getZ();
                    
                    // Debug: Check a few specific positions
                    LOGGER.at(Level.INFO).log("[FluidDebug] Checking positions around player at (" + playerX + ", " + playerY + ", " + playerZ + ")");
                    
                    for (int x = playerX - radius; x <= playerX + radius; x++) {
                        for (int y = playerY - radius; y <= playerY + radius; y++) {
                            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                                int fluidId = org.hytaledevlib.lib.BlockHelper.getFluidId(world, x, y, z);
                                
                                // Debug: Log first few fluid checks
                                if (waterBlocks.size() < 3 && fluidId != 0) {
                                    String fluidName = org.hytaledevlib.lib.BlockHelper.getFluidName(world, x, y, z);
                                    LOGGER.at(Level.INFO).log("[FluidDebug] Found fluid at (" + x + ", " + y + ", " + z + ") - ID: " + fluidId + ", Name: " + fluidName);
                                }
                                
                                if (org.hytaledevlib.lib.BlockHelper.isWater(world, x, y, z)) {
                                    byte fluidLevel = org.hytaledevlib.lib.BlockHelper.getFluidLevel(world, x, y, z);
                                    waterBlocks.add(new org.hytaledevlib.lib.BlockHelper.FluidPosition(x, y, z, fluidId, fluidLevel));
                                }
                            }
                        }
                    }
                    
                    LOGGER.at(Level.INFO).log("[FluidDebug] Scan complete. Found " + waterBlocks.size() + " water blocks");
                    
                    // Log water blocks found
                    if (!waterBlocks.isEmpty()) {
                        LOGGER.at(Level.INFO).log("💧 [" + playerName + "] Found " + waterBlocks.size() + " water blocks within " + radius + " blocks:");
                        
                        // Log first 5 water blocks to avoid spam
                        int count = 0;
                        for (org.hytaledevlib.lib.BlockHelper.FluidPosition fluid : waterBlocks) {
                            if (count >= 5) {
                                LOGGER.at(Level.INFO).log("  ... and " + (waterBlocks.size() - 5) + " more");
                                break;
                            }
                            String fluidName = org.hytaledevlib.lib.BlockHelper.getFluidName(world, fluid.x, fluid.y, fluid.z);
                            
                            // Get the Fluid object to see its asset ID
                            com.hypixel.hytale.server.core.asset.type.fluid.Fluid fluidObj = org.hytaledevlib.lib.BlockHelper.getFluid(world, fluid.x, fluid.y, fluid.z);
                            String assetId = fluidObj != null ? fluidObj.getId() : "unknown";
                            
                            LOGGER.at(Level.INFO).log("  - " + fluidName + " (Asset: " + assetId + ", ID: " + fluid.fluidId + ") at (" + fluid.x + ", " + fluid.y + ", " + fluid.z + ") level=" + fluid.fluidLevel);
                            count++;
                        }
                    } else {
                        LOGGER.at(Level.INFO).log("[" + playerName + "] No water blocks nearby (checked 5-block radius)");
                    }
                }
            }
            
            // Schedule the next check (recursive)
            scheduleFluidCheck(world);
        });
    }
    
    /**
     * Register water placement test - places a 3x3 water area beneath each player after 200 ticks.
     */
    private void registerWaterPlacementTest(World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering Water Placement test...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Wait 200 ticks before placing water
        org.hytaledevlib.lib.WorldHelper.waitTicks(world, 200, () -> {
            LOGGER.at(Level.INFO).log("💧 Placing 3x3 water beneath players...");
            
            // Find all players
            for (com.hypixel.hytale.server.core.entity.Entity entity : org.hytaledevlib.lib.EntityHelper.getEntities(world)) {
                if (org.hytaledevlib.lib.EntityHelper.isPlayer(entity)) {
                    String playerName = org.hytaledevlib.lib.EntityHelper.getName(entity);
                    com.hypixel.hytale.math.vector.Vector3d playerPos = org.hytaledevlib.lib.EntityHelper.getPosition(entity);
                    
                    if (playerPos == null) {
                        continue;
                    }
                    
                    // Place water one block beneath the player in a 3x3 area centered on player X/Z.
                    int centerX = (int) playerPos.getX();
                    int waterY = (int) playerPos.getY() - 1;
                    int centerZ = (int) playerPos.getZ();

                    int placedCount = 0;
                    int failedCount = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int waterX = centerX + dx;
                            int waterZ = centerZ + dz;
                            boolean placed = org.hytaledevlib.lib.BlockHelper.placeWater(world, waterX, waterY, waterZ);
                            if (placed) {
                                placedCount++;
                            } else {
                                failedCount++;
                            }
                        }
                    }

                    if (placedCount > 0) {
                        LOGGER.at(Level.INFO).log("✅ [" + playerName + "] Placed " + placedCount + "/9 water blocks centered at (" + centerX + ", " + waterY + ", " + centerZ + ")");
                        if (failedCount > 0) {
                            LOGGER.at(Level.WARNING).log("⚠ [" + playerName + "] Failed to place " + failedCount + "/9 water blocks in 3x3 area");
                        }
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(entity, "Placed " + placedCount + "/9 water blocks beneath you.");
                    } else {
                        LOGGER.at(Level.WARNING).log("❌ [" + playerName + "] Failed to place any water in 3x3 area centered at (" + centerX + ", " + waterY + ", " + centerZ + ")");
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(entity, "Failed to place water in 3x3 area.");
                    }
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("✅ Water placement test registered!");
        LOGGER.at(Level.INFO).log("  ✓ Will place a 3x3 water area beneath players in 200 ticks (10 seconds)");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Register item interaction examples using the new ItemHelper system.
     */
    private void registerItemInteractions() {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering Item Interactions...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Example: Custom water bucket that places water using our working placeWater method
        org.hytaledevlib.lib.ItemHelper.onItemRightClick("hytale:water_bucket", 
            (player, item, targetBlock, targetEntity) -> {
                if (targetBlock == null) {
                    org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "No block in range!");
                    return false; // Don't cancel - let default behavior happen
                }
                
                // Place water at the block position (above the clicked block)
                com.hypixel.hytale.server.core.universe.world.World world = player.getWorld();
                int x = targetBlock.getX();
                int y = targetBlock.getY() + 1; // Place above the clicked block
                int z = targetBlock.getZ();
                
                boolean placed = org.hytaledevlib.lib.BlockHelper.placeWater(world, x, y, z);
                
                if (placed) {
                    org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "Custom water placed!");
                    LOGGER.at(Level.INFO).log("Custom water bucket placed water at (" + x + ", " + y + ", " + z + ")");
                    return true; // Cancel default bucket behavior
                } else {
                    org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "Failed to place water!");
                    return false; // Let default behavior happen
                }
            }
        );
        
        // Example: Cobalt longsword that sends chat message on right-click
        org.hytaledevlib.lib.ItemHelper.onItemRightClick("Weapon_Longsword_Cobalt", 
            (player, item, targetBlock, targetEntity) -> {
                // Send a chat message to the player
                org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                    "You right-clicked with your cobalt longsword!");
                
                LOGGER.at(Level.INFO).log("Cobalt longsword right-click by " + 
                    org.hytaledevlib.lib.EntityHelper.getName(player));
                
                return true; // Cancel default behavior
            }
        );
        
        // Example: Left-click with any pickaxe to break blocks instantly (for testing)
        org.hytaledevlib.lib.ItemHelper.onItemLeftClick("hytale:wooden_pickaxe", 
            (player, item, targetBlock, targetEntity) -> {
                if (targetBlock != null) {
                    com.hypixel.hytale.server.core.universe.world.World world = player.getWorld();
                    int x = targetBlock.getX();
                    int y = targetBlock.getY();
                    int z = targetBlock.getZ();
                    
                    // Break the block
                    boolean broken = org.hytaledevlib.lib.BlockHelper.setBlock(world, x, y, z, 0);
                    
                    if (broken) {
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                            "Instant break at (" + x + ", " + y + ", " + z + ")");
                        LOGGER.at(Level.INFO).log("Instant break: " + org.hytaledevlib.lib.EntityHelper.getName(player) + 
                            " broke block at (" + x + ", " + y + ", " + z + ")");
                        return true; // Cancel default behavior
                    }
                }
                return false; // Let default behavior happen
            }
        );
        
        LOGGER.at(Level.INFO).log("✅ Item interactions registered!");
        LOGGER.at(Level.INFO).log("  ✓ Water bucket: Custom water placement");
        LOGGER.at(Level.INFO).log("  ✓ Cobalt longsword: Chat message on right-click");
        LOGGER.at(Level.INFO).log("  ✓ Wooden pickaxe: Instant block break");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Register example quests using QuestHelper
     */
    private void registerExampleQuests() {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Registering Example Quests...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Register a simple gathering quest
        org.hytaledevlib.lib.QuestHelper.Quest gatheringQuest = new org.hytaledevlib.lib.QuestHelper.QuestBuilder("gather_wood")
            .name("Gather Wood")
            .description("Collect wood to help build the village")
            .addCollectObjective("wood", "Wood_Oak_Trunk", 10)
            .addItemReward("Rock_Gem_Diamond", 1)
            .addCurrencyReward(50)
            .repeatable(false)
            .build();
        
        org.hytaledevlib.lib.QuestHelper.registerQuest(gatheringQuest);
        
        // Register a hunting quest
        org.hytaledevlib.lib.QuestHelper.Quest huntingQuest = new org.hytaledevlib.lib.QuestHelper.QuestBuilder("hunt_animals")
            .name("Hunt Animals")
            .description("Hunt animals for food")
            .addKillObjective("kill_cows", "Cow", 5)
            .addKillObjective("kill_chickens", "Chicken", 3)
            .addItemReward("Ingredient_Meat_Raw", 8)
            .addCurrencyReward(100)
            .repeatable(true)
            .build();
        
        org.hytaledevlib.lib.QuestHelper.registerQuest(huntingQuest);
        
        // Register a quest chain (requires first quest)
        org.hytaledevlib.lib.QuestHelper.Quest advancedQuest = new org.hytaledevlib.lib.QuestHelper.QuestBuilder("advanced_gathering")
            .name("Advanced Gathering")
            .description("Now gather rare materials")
            .addCollectObjective("diamonds", "Rock_Gem_Diamond", 5)
            .addItemReward("Weapon_Longsword_Cobalt", 1)
            .addCurrencyReward(500)
            .prerequisite("gather_wood")
            .repeatable(false)
            .build();
        
        org.hytaledevlib.lib.QuestHelper.registerQuest(advancedQuest);
        
        // Register completion callbacks
        org.hytaledevlib.lib.QuestHelper.onQuestComplete("gather_wood", (player, questId) -> {
            org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "§a§lQuest Complete! You've finished: Gather Wood");
            LOGGER.at(Level.INFO).log(org.hytaledevlib.lib.EntityHelper.getName(player) + " completed quest: " + questId);
        });
        
        org.hytaledevlib.lib.QuestHelper.onQuestComplete("hunt_animals", (player, questId) -> {
            org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "§a§lQuest Complete! You've finished: Hunt Animals");
        });
        
        org.hytaledevlib.lib.QuestHelper.onQuestComplete("advanced_gathering", (player, questId) -> {
            org.hytaledevlib.lib.PlayerHelper.sendMessage(player, "§a§lQuest Complete! You've finished: Advanced Gathering");
        });
        
        LOGGER.at(Level.INFO).log("✅ Example quests registered!");
        LOGGER.at(Level.INFO).log("  ✓ Gather Wood - Collect 10 oak logs");
        LOGGER.at(Level.INFO).log("  ✓ Hunt Animals - Kill cows and chickens");
        LOGGER.at(Level.INFO).log("  ✓ Advanced Gathering - Quest chain requiring Gather Wood");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Setup automatic quest tracking by hooking into game events
     */
    private void setupQuestTracking(com.hypixel.hytale.server.core.universe.world.World world) {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Setting up Quest Tracking...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Track item pickups for COLLECT objectives
        org.hytaledevlib.lib.EventHelper.onItemPickup(this, (itemId, quantity, playerEntity) -> {
            if (!(playerEntity instanceof com.hypixel.hytale.server.core.entity.entities.Player)) return;
            
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                (com.hypixel.hytale.server.core.entity.entities.Player) playerEntity;
            
            LOGGER.at(Level.FINE).log("[Quest Tracking] Item pickup: " + itemId + " x" + quantity);
            
            // Check all active quests for this player
            for (org.hytaledevlib.lib.QuestHelper.QuestProgress progress : 
                 org.hytaledevlib.lib.QuestHelper.getActiveQuests(player)) {
                
                org.hytaledevlib.lib.QuestHelper.Quest quest = 
                    org.hytaledevlib.lib.QuestHelper.getQuest(progress.getQuestId());
                
                if (quest == null) continue;
                
                // Check each objective
                for (org.hytaledevlib.lib.QuestHelper.Objective objective : quest.getObjectives()) {
                    if (objective.getType() == org.hytaledevlib.lib.QuestHelper.ObjectiveType.COLLECT 
                        && objective.getTarget().equals(itemId)) {
                        
                        LOGGER.at(Level.INFO).log("[Quest Tracking] COLLECT objective matched via ITEM PICKUP");
                        
                        // Update progress
                        org.hytaledevlib.lib.QuestHelper.updateObjectiveProgress(
                            player, quest.getId(), objective.getId(), quantity
                        );
                        
                        int currentProgress = progress.getProgress(objective.getId());
                        int required = objective.getRequiredAmount();
                        
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                            "§e[Quest] " + quest.getName() + ": " + 
                            Math.min(currentProgress, required) + "/" + required + " " + itemId);
                        
                        LOGGER.at(Level.INFO).log("[Quest] " + org.hytaledevlib.lib.EntityHelper.getName(player) + 
                            " progress on " + quest.getName() + ": " + currentProgress + "/" + required);
                    }
                }
            }
        });
        
        // Track entity kills for KILL objectives
        org.hytaledevlib.lib.DeathHelper.onEntityDeath(world, (death) -> {
            // Check if killed by a player
            if (!death.isKillerPlayer()) return;
            
            // Get killer entity reference
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> killerRef = death.getKiller();
            if (killerRef == null || !killerRef.isValid()) return;
            
            // Get Player component from the killer
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                death.getStore().getComponent(killerRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            
            if (player == null) return;
            
            // Get the entity that died
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> deadRef = death.getEntityRef();
            String entityType = death.getEntityName(); // Use the helper method from DeathHelper
            
            // Check all active quests for this player
            for (org.hytaledevlib.lib.QuestHelper.QuestProgress progress : 
                 org.hytaledevlib.lib.QuestHelper.getActiveQuests(player)) {
                
                org.hytaledevlib.lib.QuestHelper.Quest quest = 
                    org.hytaledevlib.lib.QuestHelper.getQuest(progress.getQuestId());
                
                if (quest == null) continue;
                
                // Check each objective
                for (org.hytaledevlib.lib.QuestHelper.Objective objective : quest.getObjectives()) {
                    if (objective.getType() == org.hytaledevlib.lib.QuestHelper.ObjectiveType.KILL 
                        && objective.getTarget().equals(entityType)) {
                        
                        // Update progress
                        org.hytaledevlib.lib.QuestHelper.updateObjectiveProgress(
                            player, quest.getId(), objective.getId(), 1
                        );
                        
                        int currentProgress = progress.getProgress(objective.getId());
                        int required = objective.getRequiredAmount();
                        
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                            "§e[Quest] " + quest.getName() + ": " + 
                            Math.min(currentProgress, required) + "/" + required + " " + entityType + " killed");
                    }
                }
            }
        });
        
        // Track block breaking for BREAK_BLOCK objectives
        org.hytaledevlib.lib.EcsEventHelper.onBlockBreak(world, (position, blockTypeId, playerEntity) -> {
            if (!(playerEntity instanceof com.hypixel.hytale.server.core.entity.entities.Player)) return;
            
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                (com.hypixel.hytale.server.core.entity.entities.Player) playerEntity;
            
            // Check all active quests for this player
            for (org.hytaledevlib.lib.QuestHelper.QuestProgress progress : 
                 org.hytaledevlib.lib.QuestHelper.getActiveQuests(player)) {
                
                org.hytaledevlib.lib.QuestHelper.Quest quest = 
                    org.hytaledevlib.lib.QuestHelper.getQuest(progress.getQuestId());
                
                if (quest == null) continue;
                
                // Check each objective
                for (org.hytaledevlib.lib.QuestHelper.Objective objective : quest.getObjectives()) {
                    if (objective.getType() == org.hytaledevlib.lib.QuestHelper.ObjectiveType.BREAK_BLOCK 
                        && objective.getTarget().equals(blockTypeId)) {
                        
                        // Update progress
                        org.hytaledevlib.lib.QuestHelper.updateObjectiveProgress(
                            player, quest.getId(), objective.getId(), 1
                        );
                        
                        int currentProgress = progress.getProgress(objective.getId());
                        int required = objective.getRequiredAmount();
                        
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                            "§e[Quest] " + quest.getName() + ": " + 
                            Math.min(currentProgress, required) + "/" + required + " " + blockTypeId + " broken");
                    }
                }
            }
        });
        
        // Track zone visits for VISIT objectives
        org.hytaledevlib.lib.EcsEventHelper.onZoneDiscovery(world, (zoneName, playerEntity) -> {
            if (!(playerEntity instanceof com.hypixel.hytale.server.core.entity.entities.Player)) return;
            
            com.hypixel.hytale.server.core.entity.entities.Player player = 
                (com.hypixel.hytale.server.core.entity.entities.Player) playerEntity;
            
            // Check all active quests for this player
            for (org.hytaledevlib.lib.QuestHelper.QuestProgress progress : 
                 org.hytaledevlib.lib.QuestHelper.getActiveQuests(player)) {
                
                org.hytaledevlib.lib.QuestHelper.Quest quest = 
                    org.hytaledevlib.lib.QuestHelper.getQuest(progress.getQuestId());
                
                if (quest == null) continue;
                
                // Check each objective
                for (org.hytaledevlib.lib.QuestHelper.Objective objective : quest.getObjectives()) {
                    if (objective.getType() == org.hytaledevlib.lib.QuestHelper.ObjectiveType.VISIT 
                        && objective.getTarget().equals(zoneName)) {
                        
                        // Update progress
                        org.hytaledevlib.lib.QuestHelper.updateObjectiveProgress(
                            player, quest.getId(), objective.getId(), 1
                        );
                        
                        org.hytaledevlib.lib.PlayerHelper.sendMessage(player, 
                            "§e[Quest] " + quest.getName() + ": Visited " + zoneName);
                    }
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("✅ Quest tracking configured!");
        LOGGER.at(Level.INFO).log("  ✓ Item pickups -> COLLECT objectives");
        LOGGER.at(Level.INFO).log("  ✓ Entity kills -> KILL objectives");
        LOGGER.at(Level.INFO).log("  ✓ Block breaking -> BREAK_BLOCK objectives");
        LOGGER.at(Level.INFO).log("  ✓ Zone visits -> VISIT objectives");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Load persisted quest and economy data from disk
     */
    private void loadPersistedData() {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Loading persisted data...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Load quest data
        boolean questsLoaded = org.hytaledevlib.lib.QuestHelper.loadQuestData();
        if (questsLoaded) {
            LOGGER.at(Level.INFO).log("✅ Quest progress loaded from disk");
        } else {
            LOGGER.at(Level.INFO).log("ℹ️ No quest data found (fresh start)");
        }
        
        // Load economy data
        boolean economyLoaded = org.hytaledevlib.lib.EconomyHelper.loadEconomyData();
        if (economyLoaded) {
            LOGGER.at(Level.INFO).log("✅ Economy data loaded from disk");
        } else {
            LOGGER.at(Level.INFO).log("ℹ️ No economy data found (fresh start)");
        }
        
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Save all persisted data to disk
     */
    private void savePersistedData() {
        LOGGER.at(Level.INFO).log("💾 Saving quest and economy data...");
        
        boolean questsSaved = org.hytaledevlib.lib.QuestHelper.saveQuestData();
        boolean economySaved = org.hytaledevlib.lib.EconomyHelper.saveEconomyData();
        
        if (questsSaved && economySaved) {
            LOGGER.at(Level.INFO).log("✅ All data saved successfully");
        } else {
            if (!questsSaved) {
                LOGGER.at(Level.WARNING).log("⚠️ Failed to save quest data");
            }
            if (!economySaved) {
                LOGGER.at(Level.WARNING).log("⚠️ Failed to save economy data");
            }
        }
    }
    
    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Shutting down HytaleDevLib Test Plugin...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Save all data before shutdown
        savePersistedData();
        
        // Shutdown DataHelper executor
        org.hytaledevlib.lib.DataHelper.shutdown();
        
        LOGGER.at(Level.INFO).log("✅ Plugin shutdown complete");
    }
    
    /**
     * Setup economy system using EconomyHelper
     */
    private void setupEconomySystem() {
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("Setting up Economy System...");
        LOGGER.at(Level.INFO).log("========================================");
        
        // Register custom currencies
        org.hytaledevlib.lib.EconomyHelper.registerCurrency("coins", "Coin", "$", 100.0);
        org.hytaledevlib.lib.EconomyHelper.registerCurrency("gems", "Gem", "💎", 0.0);
        
        org.hytaledevlib.lib.EconomyHelper.setDefaultCurrency("coins");
        
        // Register transaction callback for logging
        org.hytaledevlib.lib.EconomyHelper.onTransaction((player, transaction) -> {
            String playerName = org.hytaledevlib.lib.EntityHelper.getName(player);
            LOGGER.at(Level.INFO).log("[Economy] " + playerName + " - " + 
                transaction.getType() + ": " + 
                org.hytaledevlib.lib.EconomyHelper.formatBalance(transaction.getCurrencyId(), transaction.getAmount()) + 
                " (" + transaction.getReason() + ")");
        });
        
        LOGGER.at(Level.INFO).log("✅ Economy system configured!");
        LOGGER.at(Level.INFO).log("  ✓ Default currency: Coins ($) - Starting balance: $100.00");
        LOGGER.at(Level.INFO).log("  ✓ Secondary currency: Gems (💎) - Starting balance: 0");
        LOGGER.at(Level.INFO).log("  ✓ Transaction logging enabled");
        LOGGER.at(Level.INFO).log("========================================");
        LOGGER.at(Level.INFO).log("");
    }
    
    /**
     * Start continuous well checking system that runs every tick
     */
    private void startWellCheckingSystem(World world) {
        LOGGER.at(Level.INFO).log("🔄 Starting continuous well checking system...");
        
        // Use WorldHelper to run a check every tick
        org.hytaledevlib.lib.WorldHelper.onTick(world, (tickCount) -> {
            if (!openWells.isEmpty()) {
                // Check all tracked wells
                for (java.util.Map.Entry<String, com.hypixel.hytale.math.vector.Vector3i> entry : openWells.entrySet()) {
                    String playerName = entry.getKey();
                    com.hypixel.hytale.math.vector.Vector3i wellPos = entry.getValue();
                    
                    try {
                        // Get the well state
                        com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState wellState = 
                            (com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState) 
                            org.hytaledevlib.lib.BlockStateHelper.ensureState(world, wellPos.x, wellPos.y, wellPos.z);
                        
                        if (wellState != null) {
                            com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer combinedContainer = wellState.getItemContainer();
                            if (combinedContainer != null) {
                                com.hypixel.hytale.server.core.inventory.container.ItemContainer outputContainer = combinedContainer.getContainer(2);
                                if (outputContainer != null) {
                                    // Check for well buckets and convert them
                                    for (short i = 0; i < outputContainer.getCapacity(); i++) {
                                        com.hypixel.hytale.server.core.inventory.ItemStack stack = outputContainer.getItemStack(i);
                                        if (stack != null && "Container_Bucket_Filled_Water_Well".equals(stack.getItemId())) {
                                            // Found a well bucket - convert it!
                                            LOGGER.at(Level.INFO).log("🔄 Auto-converting bucket in well at " + wellPos + " for " + playerName);
                                            
                                            // Temporarily disable filter
                                            outputContainer.setGlobalFilter(com.hypixel.hytale.server.core.inventory.container.filter.FilterType.ALLOW_ALL);
                                            
                                            // Create filled vanilla bucket
                                            com.hypixel.hytale.server.core.inventory.ItemStack emptyBucket = 
                                                new com.hypixel.hytale.server.core.inventory.ItemStack("Container_Bucket", stack.getQuantity());
                                            com.hypixel.hytale.server.core.inventory.ItemStack filledBucket = emptyBucket.withState("Filled_Water");
                                            
                                            // Set in slot
                                            outputContainer.setItemStackForSlot(i, filledBucket);
                                            
                                            // Restore filter
                                            outputContainer.setGlobalFilter(com.hypixel.hytale.server.core.inventory.container.filter.FilterType.ALLOW_OUTPUT_ONLY);
                                            
                                            // Mark for save
                                            org.hytaledevlib.lib.BlockStateHelper.markNeedsSave(wellState);
                                            
                                            LOGGER.at(Level.INFO).log("  ✓ Converted! Slot now has: " + filledBucket.getItemId());
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Silent fail for continuous checks
                    }
                }
            }
        });
        
        LOGGER.at(Level.INFO).log("✓ Well checking system started!");
    }
}
