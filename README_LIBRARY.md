# HytaleDevLib - Developer Utility Library for Hytale Modding

A utility library that simplifies common Hytale modding tasks by providing tested helpers, utilities, and workarounds for the Hytale API.

## Helper Classes Overview

### 1. EventHelper
Simplified event registration for common game events. Handles the boilerplate of registering global event listeners and parsing transaction data.

**What it does:**
- Registers event listeners without manual EventRegistry calls
- Parses `LivingEntityInventoryChangeEvent` transactions to extract item details
- Provides callbacks for item drops, pickups, and player joins

**When to use:**
- Detecting when players drop or pick up items
- Tracking player join events
- Avoiding manual transaction string parsing

---

### 2. WorldHelper
Convenient utilities for world operations and thread-safe execution.

**What it does:**
- Executes tasks on the world's main thread
- Provides player management (get players, count)
- Broadcasts messages to all players
- Queries world state (paused, tick count)
- Simplifies logging with proper HytaleLogger API usage

**When to use:**
- Need thread-safe world operations
- Broadcasting server messages
- Logging debug/warning/error messages
- Checking world state or player counts

---

### 3. ComponentHelper
Type-safe ECS component operations using ComponentType for proper API usage.

**What it does:**
- Get/put/add/remove components with proper type safety
- Simplified display name management
- Extract item details (ID, quantity) from ItemComponent
- Handles null checks and exceptions gracefully

**When to use:**
- Adding/removing components from entities
- Setting display names on entities
- Reading item data from ground items
- Any ECS component manipulation

---

### 4. EntityHelper
Fundamental entity operations and spatial queries.

**What it does:**
- Find players by name or UUID
- Get entity positions and calculate distances
- Teleport entities to locations
- Find players within radius of a position or entity
- Check entity existence and type

**When to use:**
- Finding specific players in the world
- Proximity detection (players near a location)
- Teleportation mechanics
- Distance-based game logic
- Entity validation and queries

---

### 5. BlockHelper
Block manipulation and world editing utilities.

**What it does:**
- Get and set blocks at specific positions
- Fill or replace blocks in rectangular regions
- Find blocks of specific types within a radius
- Count blocks in regions
- Check if positions contain air blocks

**When to use:**
- Building/terrain modification mods
- Custom world generation
- Area protection or region management
- Block-based game mechanics
- Mining or construction features

## Discovered API Information

### Working Events
- ✅ `AddPlayerToWorldEvent` - Player joins world
- ✅ `LivingEntityInventoryChangeEvent` - Inventory changes (drops, pickups)
- ✅ `BreakBlockEvent` - Block breaking (via global listener)
- ❌ `DropItemEvent` - Does NOT fire for player inventory drops
- ❌ `InteractivelyPickupItemEvent` - Needs testing

### Component Paths
```java
// Items
com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent

// Players
com.hypixel.hytale.server.core.entity.entities.Player
com.hypixel.hytale.server.core.universe.PlayerRef
```

### Known Issues
1. **ECS System Registration**: `PlayerRef.getComponentType()` returns null during plugin setup
2. **EntityEventSystem**: Cannot register with component queries
3. **Direct Entity Access**: No working pattern for querying entity store yet

## Usage Guide

### EventHelper Methods

#### `onItemDrop(plugin, callback)`
Detects when a player drops an item from their inventory.

```java
EventHelper.onItemDrop(this, (itemId, quantity) -> {
    getLogger().at(Level.INFO).log("Item dropped: " + itemId + " x" + quantity);
    // Example: Track dropped items, spawn custom effects, etc.
});
```

#### `onItemPickup(plugin, callback)`
Detects when a player picks up an item.

```java
EventHelper.onItemPickup(this, (itemId, quantity) -> {
    getLogger().at(Level.INFO).log("Player picked up: " + itemId + " x" + quantity);
    // Example: Award achievements, track collection progress
});
```

#### `onPlayerJoin(plugin, callback)`
Fires when a player joins the world.

```java
EventHelper.onPlayerJoin(this, player -> {
    getLogger().at(Level.INFO).log("Player joined: " + player.getName());
    // Example: Send welcome message, initialize player data
});
```

---

### WorldHelper Methods

#### `executeOnWorldThread(world, task)`
Executes a task on the world's main thread (thread-safe).

```java
WorldHelper.executeOnWorldThread(world, () -> {
    // Any world operations here are thread-safe
    // Example: Spawn entities, modify blocks, etc.
});
```

#### `broadcastMessage(world, message)`
Sends a message to all players in the world.

```java
WorldHelper.broadcastMessage(world, Message.raw("Server restart in 5 minutes!"));
```

#### `getPlayers(world)` / `getPlayerCount(world)`
Get all players or just the count.

```java
List<Entity> players = WorldHelper.getPlayers(world);
int count = WorldHelper.getPlayerCount(world);
```

#### `isPaused(world)` / `getCurrentTick(world)`
Query world state.

```java
if (!WorldHelper.isPaused(world)) {
    long tick = WorldHelper.getCurrentTick(world);
    // Do something based on tick count
}
```

#### `log(world, message)` / `logWarning(world, message)` / `logError(world, message)`
Simplified logging using HytaleLogger.

```java
WorldHelper.log(world, "Debug: Processing items");
WorldHelper.logWarning(world, "Warning: High entity count");
WorldHelper.logError(world, "Error: Failed to load data");
```

#### `onTick(world, callback)`
Register a callback that fires every single tick.

```java
WorldHelper.onTick(world, currentTick -> {
    // Runs every tick (20 times per second at 20 TPS)
    // Example: Update HUD, check conditions, etc.
});
```

#### `onTickInterval(world, interval, callback)`
Register a callback that fires every N ticks. More efficient than `onTick` for periodic tasks.

```java
// Run every second (20 ticks at 20 TPS)
WorldHelper.onTickInterval(world, 20, currentTick -> {
    WorldHelper.log(world, "One second passed, tick: " + currentTick);
});

// Run every 5 seconds (100 ticks)
WorldHelper.onTickInterval(world, 100, currentTick -> {
    // Example: Auto-save, cleanup, periodic checks
    int itemCount = 0; // Count items, etc.
});
```

#### `stopTickTracking(world)`
Stop all tick tracking for a world. Call this in your plugin's shutdown method.

```java
@Override
public void onDisable() {
    WorldHelper.stopTickTracking(world);
}
```

#### `waitTicks(world, ticks, callback)`
Wait for a specified number of ticks before executing a callback. Perfect for delayed actions.

```java
// Player joins -> wait 3 seconds (60 ticks) -> send welcome message
EventHelper.onPlayerJoinWorld(plugin, world -> {
    WorldHelper.waitTicks(world, 60, () -> {
        WorldHelper.broadcastMessage(world, Message.raw("Welcome to the server!"));
    });
});

// Wait 5 seconds (100 ticks) before spawning an entity
WorldHelper.waitTicks(world, 100, () -> {
    // Spawn entity, trigger event, etc.
    WorldHelper.log(world, "Delayed action executed!");
});

// Chain multiple delays
WorldHelper.waitTicks(world, 20, () -> {
    WorldHelper.log(world, "After 1 second");
    
    WorldHelper.waitTicks(world, 20, () -> {
        WorldHelper.log(world, "After 2 seconds total");
    });
});
```

**Note:** The tick tracker uses a background timer that polls `world.getTick()` every 50ms and executes callbacks on the world's main thread for thread safety. The `waitTicks` method creates a self-canceling timer for one-time execution.

---

### EntityHelper Methods

#### `getPlayerByName(world, name)`
Find a player by their display name (case-insensitive).

```java
Entity player = EntityHelper.getPlayerByName(world, "Se7enity");
if (player != null) {
    WorldHelper.log(world, "Found player: " + EntityHelper.getName(player));
}
```

#### `getPlayerByUUID(world, uuid)`
Find a player by their UUID.

```java
UUID playerUuid = UUID.fromString("c3257f18-4326-4089-9231-60120125d5d7");
Entity player = EntityHelper.getPlayerByUUID(world, playerUuid);
```

#### `getPosition(entity)` / `teleport(entity, position)`
Get or set entity positions.

```java
// Get position
Vector3d pos = EntityHelper.getPosition(player);
WorldHelper.log(world, "Player at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());

// Teleport to position
EntityHelper.teleport(player, new Vector3d(100, 64, 100));

// Teleport to coordinates
EntityHelper.teleport(player, 100, 64, 100);
```

#### `getDistance(entity1, entity2)` / `getDistance(entity, position)`
Calculate distances between entities or positions.

```java
// Distance between two players
double distance = EntityHelper.getDistance(player1, player2);
WorldHelper.log(world, "Distance: " + distance + " blocks");

// Distance to a location
Vector3d spawn = new Vector3d(0, 64, 0);
double distanceToSpawn = EntityHelper.getDistance(player, spawn);
```

#### `getPlayersInRadius(world, center, radius)` / `getPlayersInRadius(entity, radius)`
Find all players within a radius.

```java
// Players within 50 blocks of a position
Vector3d center = new Vector3d(100, 64, 100);
List<Entity> nearbyPlayers = EntityHelper.getPlayersInRadius(world, center, 50.0);

// Players within 10 blocks of an entity
List<Entity> closeBy = EntityHelper.getPlayersInRadius(targetEntity, 10.0);

// Example: Broadcast to nearby players
for (Entity nearby : nearbyPlayers) {
    // Send message to each nearby player
}
```

#### `isWithinDistance(entity, position, distance)`
Check if an entity is within a certain distance.

```java
Vector3d checkpoint = new Vector3d(200, 64, 200);
if (EntityHelper.isWithinDistance(player, checkpoint, 5.0)) {
    WorldHelper.broadcastMessage(world, Message.raw("Player reached checkpoint!"));
}

// Check distance between two entities
if (EntityHelper.isWithinDistance(player, boss, 20.0)) {
    // Player is within boss aggro range
}
```

#### `isPlayer(entity)` / `exists(entity)` / `getName(entity)` / `getEntityType(entity)`
Entity validation and information.

```java
// Check if entity is a player
if (EntityHelper.isPlayer(entity)) {
    WorldHelper.log(world, "This is a player!");
}

// Check if entity still exists
if (EntityHelper.exists(entity)) {
    // Entity is valid and not removed
}

// Get entity name (retrieves username from PlayerRef for players)
String name = EntityHelper.getName(entity);
WorldHelper.log(world, "Player name: " + name);

// Get entity type (returns readable names for NPCs)
String type = EntityHelper.getEntityType(entity);
WorldHelper.log(world, "Entity type: " + type);
// Returns: "Minnow", "Deer_Doe", "Skeleton_Fighter", "Fox", etc. for NPCs
// Returns: "Player", "ItemEntity", etc. for other entity types
```

**Note:** `getName()` uses the ECS system to retrieve player usernames from the `PlayerRef` component. For NPCs and other entities, it returns the entity class name as a fallback.

**Note:** `getEntityType()` extracts the `roleName` field from NPC role objects to return readable entity type names like "Minnow", "Deer_Doe", "Trork_Warrior", etc. For NPCs without initialized roles, it returns "NPCEntity_Uninitialized". For non-NPC entities, it returns the class name (e.g., "Player", "ItemEntity").

#### `getPlayerHome(player)` / `getPlayerRespawnPosition(player)` / `teleportPlayerHome(player)`
Get and use player home/respawn locations.

```java
// Get player's home/respawn position (bed or world spawn)
Vector3d homePos = EntityHelper.getPlayerHome(player);
if (homePos != null) {
    WorldHelper.log(world, "Player's home is at: " + homePos);
    double distance = EntityHelper.getDistance(player, homePos);
    WorldHelper.log(world, "Distance from home: " + distance + " blocks");
}

// Get full Transform (position + rotation)
Transform respawnTransform = EntityHelper.getPlayerRespawnPosition(player);
if (respawnTransform != null) {
    Vector3d pos = respawnTransform.getPosition();
    Vector3f rotation = respawnTransform.getRotation();
}

// Teleport player to their home/respawn location
if (EntityHelper.teleportPlayerHome(player)) {
    WorldHelper.log(world, "Teleported player home!");
}
```

**Note:** These methods use `Player.getRespawnPosition()` which returns the player's bed spawn point if they have one, or the world spawn point otherwise. This is the same logic used by Hytale's respawn system.

#### `getAllEntities(world)` / `getClosestEntity(...)`
Entity iteration and proximity searches.

```java
// Get all loaded entities in the world
List<Entity> allEntities = EntityHelper.getAllEntities(world);
WorldHelper.log(world, "Total entities: " + allEntities.size());

// Iterate through all entities
for (Entity entity : allEntities) {
    String type = EntityHelper.getEntityType(entity);
    WorldHelper.log(world, "Found entity of type: " + type);
}

// Find closest entity to a position
Vector3d searchPos = new Vector3d(100, 64, 100);
Entity closest = EntityHelper.getClosestEntity(world, searchPos);
if (closest != null) {
    WorldHelper.log(world, "Closest entity: " + EntityHelper.getEntityType(closest));
}

// Find closest entity to another entity within range
Entity nearbyEntity = EntityHelper.getClosestEntity(player, 50.0);
if (nearbyEntity != null) {
    double distance = EntityHelper.getDistance(player, nearbyEntity);
    WorldHelper.log(world, "Found entity " + distance + " blocks away");
}

// Find closest entity excluding a specific entity (e.g., exclude the player)
Entity closestNonPlayer = EntityHelper.getClosestEntity(world, playerPos, player);
if (closestNonPlayer != null) {
    String type = EntityHelper.getEntityType(closestNonPlayer);
    WorldHelper.log(world, "Closest non-player entity: " + type);
}
```

**Note:** `getAllEntities()` uses reflection to access the `EntityStore`'s internal `entitiesByUuid` map, which contains **all loaded entities** in the world including players, NPCs, items, and all other entity types. If reflection fails (e.g., due to security restrictions), it falls back to returning only players.

**Note:** `getClosestEntity()` has an overload that accepts an `excludeEntity` parameter to exclude a specific entity from the search (useful for finding the closest entity to a player without returning the player itself).

#### Filtering Entities by Type
You can filter entities by their type to work with specific entity types:

```java
// Get all entities
List<Entity> allEntities = EntityHelper.getAllEntities(world);

// Filter for only cows
List<Entity> cows = new ArrayList<>();
for (Entity entity : allEntities) {
    if ("Cow".equals(EntityHelper.getEntityType(entity))) {
        cows.add(entity);
    }
}
WorldHelper.log(world, "Found " + cows.size() + " cows");

// Teleport all cows to a specific location
Vector3d barnLocation = new Vector3d(100, 64, 100);
for (Entity cow : cows) {
    EntityHelper.teleport(cow, barnLocation);
}

// Find all hostile mobs
List<Entity> hostileMobs = new ArrayList<>();
for (Entity entity : allEntities) {
    String type = EntityHelper.getEntityType(entity);
    if (type.contains("Skeleton") || type.contains("Trork") || type.contains("Zombie")) {
        hostileMobs.add(entity);
    }
}
WorldHelper.log(world, "Found " + hostileMobs.size() + " hostile mobs");

// Count entities by type
Map<String, Integer> entityCounts = new HashMap<>();
for (Entity entity : allEntities) {
    String type = EntityHelper.getEntityType(entity);
    entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
}
// Log the counts
for (Map.Entry<String, Integer> entry : entityCounts.entrySet()) {
    WorldHelper.log(world, entry.getKey() + ": " + entry.getValue());
}
```

#### Spawning Entities
Entity spawning in Hytale requires manual ECS setup. Here's how to spawn entities:

```java
// Spawning entities requires access to the EntityStore and proper component setup
world.execute(() -> {
    // 1. Get the EntityStore
    EntityStore entityStore = world.getEntityStore();
    Store<EntityStore> store = entityStore.getStore();
    
    // 2. Create a new entity holder
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
    
    // 3. Get the model asset for the entity you want to spawn
    // Available entity models: Cow, Deer_Doe, Minnow, Fox, Rabbit, Pig, Sheep, etc.
    // Full list: https://hytalemodding.dev/en/docs/server/entities
    ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Cow");
    Model model = Model.createScaledModel(modelAsset, 1.0f);
    
    // 4. Define spawn position
    Vector3d spawnPos = new Vector3d(100, 64, 100);
    
    // 5. Add required components to the entity
    holder.addComponent(TransformComponent.getComponentType(), 
        new TransformComponent(spawnPos, new Vector3f(0, 0, 0)));
    holder.addComponent(PersistentModel.getComponentType(), 
        new PersistentModel(model.toReference()));
    holder.addComponent(ModelComponent.getComponentType(), 
        new ModelComponent(model));
    holder.addComponent(BoundingBox.getComponentType(), 
        new BoundingBox(model.getBoundingBox()));
    holder.addComponent(NetworkId.getComponentType(), 
        new NetworkId(store.getExternalData().takeNextNetworkId()));
    holder.addComponent(Interactions.getComponentType(), 
        new Interactions());
    
    // 6. Add the entity to the world
    entityStore.addEntity(holder, AddReason.SPAWN);
    
    WorldHelper.log(world, "Spawned entity at " + spawnPos);
});
```

**Available Entity Models:**
- Animals: `Cow`, `Pig`, `Sheep`, `Rabbit`, `Fox`, `Deer_Doe`, `Chicken`, `Horse`
- Fish: `Minnow`, `Salmon`, `Pike`, `Catfish`, `Bluegill`
- Hostile: `Skeleton_Fighter`, `Trork_Warrior`, `Goblin`, `Zombie`
- And many more! See the [full entity list](https://hytalemodding.dev/en/docs/server/entities)

**Note:** Entity spawning is a complex process that requires proper component initialization. The above example shows the basic structure, but you may need to add additional components depending on the entity type. For more details, see the [Hytale Modding Documentation](https://hytalemodding.dev/en/docs/guides/plugin/spawning-entities).

---

### ComponentHelper Methods

#### Setup: Get Component Accessor
First, get the component accessor from the world:

```java
ComponentAccessor<EntityStore> accessor = world.getEntityStore().getStore().getAccessor();
```

#### `getComponent(accessor, entityRef, componentType)`
Get a component from an entity.

```java
ComponentType<EntityStore, ItemComponent> itemType = ItemComponent.getComponentType();
ItemComponent item = ComponentHelper.getComponent(accessor, entityRef, itemType);

if (item != null) {
    String itemId = item.getItemStack().getItemId();
}
```

#### `putComponent(accessor, entityRef, componentType, component)`
Add or update a component on an entity.

```java
ComponentType<EntityStore, DisplayNameComponent> nameType = DisplayNameComponent.getComponentType();
DisplayNameComponent nameComp = new DisplayNameComponent(Message.raw("Custom Label"));
ComponentHelper.putComponent(accessor, entityRef, nameType, nameComp);
```

#### `addComponent(accessor, entityRef, componentType, component)`
Add a new component (fails if already exists).

```java
ComponentHelper.addComponent(accessor, entityRef, componentType, component);
```

#### `removeComponent(accessor, entityRef, componentType)`
Remove a component from an entity.

```java
ComponentHelper.removeComponent(accessor, entityRef, nameType);
```

#### `hasComponent(accessor, entityRef, componentType)`
Check if an entity has a component.

```java
if (ComponentHelper.hasComponent(accessor, entityRef, itemType)) {
    // Entity is an item
}
```

#### `setDisplayName(accessor, entityRef, displayName)`
Simplified method to set display name (creates component automatically).

```java
ComponentHelper.setDisplayName(accessor, entityRef, "Diamond Sword +5");
```

#### `getItemId(accessor, entityRef)` / `getItemQuantity(accessor, entityRef)`
Convenience methods for item entities.

```java
String itemId = ComponentHelper.getItemId(accessor, entityRef);
int quantity = ComponentHelper.getItemQuantity(accessor, entityRef);

if (itemId != null) {
    getLogger().at(Level.INFO).log("Found item: " + itemId + " x" + quantity);
}
```

---

### BlockHelper Methods

#### `getBlock(world, position)` / `getBlock(world, x, y, z)`
Get the block ID at a specific position.

```java
// Get block at a position
Vector3d pos = new Vector3d(100, 64, 100);
int blockId = BlockHelper.getBlock(world, pos);
WorldHelper.log(world, "Block ID at position: " + blockId);

// Get block at specific coordinates
int block = BlockHelper.getBlock(world, 100, 64, 100);

// Check if a position is air
if (BlockHelper.isAir(world, pos)) {
    WorldHelper.log(world, "Position is empty!");
}
```

#### `setBlock(world, position, blockId)` / `setBlock(world, x, y, z, blockId)`
Set a block at a specific position.

```java
// Get the current block ID first
Vector3d pos = new Vector3d(100, 64, 100);
int currentBlockId = BlockHelper.getBlock(world, pos);
WorldHelper.log(world, "Current block ID: " + currentBlockId);

// Set a block using a known block ID
boolean success = BlockHelper.setBlock(world, pos, currentBlockId);
if (success) {
    WorldHelper.log(world, "Block placed successfully!");
}

// Example: Copy a block from one location to another
int sourceBlock = BlockHelper.getBlock(world, 100, 64, 100);
BlockHelper.setBlock(world, 200, 64, 200, sourceBlock);
```

**⚠️ IMPORTANT - Block IDs:**
- Block IDs are **internal numeric identifiers** (e.g., 684, 1032, 105)
- Block ID `1` may create an invisible/invalid block in some builds
- **Best practice:** Use `getBlock()` to read existing block IDs from the world, then use those IDs
- Common block IDs vary by Hytale version and aren't documented yet
- To find valid block IDs: scan your world with `getBlocksInRegion()` and use the IDs you find

#### `fillRegion(world, pos1, pos2, blockId)`
Fill a rectangular region with a specific block type.

```java
// Create a stone platform
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);
int blocksSet = BlockHelper.fillRegion(world, corner1, corner2, 1); // Fill with stone
WorldHelper.log(world, "Created platform with " + blocksSet + " blocks");

// Build a wall
Vector3d wallStart = new Vector3d(100, 64, 100);
Vector3d wallEnd = new Vector3d(100, 70, 110);
BlockHelper.fillRegion(world, wallStart, wallEnd, 1);
```

#### `replaceBlocksInRegion(world, pos1, pos2, oldBlockId, newBlockId)`
Replace all blocks of one type with another in a region.

```java
// Replace all dirt with grass in an area
Vector3d corner1 = new Vector3d(90, 60, 90);
Vector3d corner2 = new Vector3d(110, 70, 110);
int replaced = BlockHelper.replaceBlocksInRegion(world, corner1, corner2, 3, 2); // dirt -> grass
WorldHelper.log(world, "Replaced " + replaced + " blocks");

// Clear water from an area (replace with air)
BlockHelper.replaceBlocksInRegion(world, corner1, corner2, 8, 0); // water -> air
```

#### `findNearbyBlocks(world, center, radius, blockId)`
Find all positions of a specific block type within a radius.

```java
// Find all diamond ore within 50 blocks
Vector3d playerPos = EntityHelper.getPosition(player);
List<Vector3i> diamondOres = BlockHelper.findNearbyBlocks(world, playerPos, 50, 56); // 56 = diamond ore (example)
WorldHelper.log(world, "Found " + diamondOres.size() + " diamond ore blocks nearby");

// Highlight found blocks to player
for (Vector3i orePos : diamondOres) {
    WorldHelper.log(world, "Diamond ore at: " + orePos.getX() + ", " + orePos.getY() + ", " + orePos.getZ());
}
```

#### `getBlocksInRegion(world, pos1, pos2)`
Get all block positions and IDs within a rectangular region.

```java
// Scan a region and catalog all blocks
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(105, 69, 105);
List<BlockHelper.BlockPosition> blocks = BlockHelper.getBlocksInRegion(world, corner1, corner2);

// Count different block types
Map<Integer, Integer> blockCounts = new HashMap<>();
for (BlockHelper.BlockPosition block : blocks) {
    blockCounts.put(block.blockId, blockCounts.getOrDefault(block.blockId, 0) + 1);
}

// Log the results
for (Map.Entry<Integer, Integer> entry : blockCounts.entrySet()) {
    WorldHelper.log(world, "Block ID " + entry.getKey() + ": " + entry.getValue() + " blocks");
}
```

#### `countBlocksInRegion(world, pos1, pos2, blockId)`
Count how many blocks of a specific type exist in a region.

```java
// Count stone blocks in a mining area
Vector3d corner1 = new Vector3d(100, 50, 100);
Vector3d corner2 = new Vector3d(120, 64, 120);
int stoneCount = BlockHelper.countBlocksInRegion(world, corner1, corner2, 1);
WorldHelper.log(world, "Mining area contains " + stoneCount + " stone blocks");

// Check if area is mostly cleared (count air blocks)
int airCount = BlockHelper.countBlocksInRegion(world, corner1, corner2, 0);
int totalBlocks = (21 * 15 * 21); // volume of region
double clearPercentage = (airCount * 100.0) / totalBlocks;
WorldHelper.log(world, "Area is " + clearPercentage + "% cleared");
```

#### Practical Examples

**Example 1: Create a simple house foundation**
```java
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);

// Create stone floor
BlockHelper.fillRegion(world, corner1, corner2, 1);

// Create walls (4 separate fills)
BlockHelper.fillRegion(world, new Vector3d(100, 65, 100), new Vector3d(100, 70, 110), 1); // West wall
BlockHelper.fillRegion(world, new Vector3d(110, 65, 100), new Vector3d(110, 70, 110), 1); // East wall
BlockHelper.fillRegion(world, new Vector3d(100, 65, 100), new Vector3d(110, 70, 100), 1); // North wall
BlockHelper.fillRegion(world, new Vector3d(100, 65, 110), new Vector3d(110, 70, 110), 1); // South wall
```

**Example 2: Ore detector command**
```java
EventHelper.onPlayerJoinWorld(plugin, world -> {
    // Register a command to find nearby ores
    world.getEventRegistry().registerListener(PlayerCommandEvent.class, event -> {
        if (event.getCommand().equals("/findores")) {
            Entity player = event.getPlayer();
            Vector3d pos = EntityHelper.getPosition(player);
            
            // Search for different ore types
            int diamondCount = BlockHelper.findNearbyBlocks(world, pos, 50, 56).size();
            int goldCount = BlockHelper.findNearbyBlocks(world, pos, 50, 14).size();
            int ironCount = BlockHelper.findNearbyBlocks(world, pos, 50, 15).size();
            
            WorldHelper.log(world, "Ores within 50 blocks:");
            WorldHelper.log(world, "Diamond: " + diamondCount);
            WorldHelper.log(world, "Gold: " + goldCount);
            WorldHelper.log(world, "Iron: " + ironCount);
        }
    });
});
```

**Example 3: Protected region system**
```java
// Check if player is trying to break blocks in a protected area
world.getEventRegistry().registerListener(BreakBlockEvent.class, event -> {
    Vector3d blockPos = event.getBlockPosition();
    Vector3d protectedCorner1 = new Vector3d(0, 0, 0);
    Vector3d protectedCorner2 = new Vector3d(100, 100, 100);
    
    // Check if block is in protected region
    if (isInRegion(blockPos, protectedCorner1, protectedCorner2)) {
        event.setCancelled(true);
        WorldHelper.log(world, "Cannot break blocks in spawn protection!");
    }
});

// Helper method to check if position is in region
private boolean isInRegion(Vector3d pos, Vector3d corner1, Vector3d corner2) {
    double minX = Math.min(corner1.getX(), corner2.getX());
    double maxX = Math.max(corner1.getX(), corner2.getX());
    double minY = Math.min(corner1.getY(), corner2.getY());
    double maxY = Math.max(corner1.getY(), corner2.getY());
    double minZ = Math.min(corner1.getZ(), corner2.getZ());
    double maxZ = Math.max(corner1.getZ(), corner2.getZ());
    
    return pos.getX() >= minX && pos.getX() <= maxX &&
           pos.getY() >= minY && pos.getY() <= maxY &&
           pos.getZ() >= minZ && pos.getZ() <= maxZ;
}
```

**⚠️ IMPORTANT - Chunk Requirements:**

BlockHelper uses Hytale's **32x32 block chunks** and can only access chunks present in the ChunkStore. Chunks that aren't in the store will return 0 (air) for reads and fail for writes.

**Checking if a chunk is loaded:**
```java
ChunkStore chunkStore = world.getChunkStore();
// Hytale chunks are 32x32 blocks (use ChunkUtil for correct indexing)
long chunkPos = ChunkUtil.indexChunkFromBlock(x, z);
LongSet chunkIndexes = chunkStore.getChunkIndexes();
boolean isLoaded = chunkIndexes.contains(chunkPos);
WorldHelper.log(world, "Chunk loaded: " + isLoaded);
```

Most chunks where players are active will be in the ChunkStore. If you encounter issues, verify the chunk is loaded using the code above.

## Installation

Add as a dependency in your `build.gradle`:
```gradle
dependencies {
    implementation files("path/to/HytaleDevLib.jar")
}
```

## Contributing

Found a working pattern or API discovery? Contributions welcome!

## License

MIT License - Free to use in your Hytale mods
