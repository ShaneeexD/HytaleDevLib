# HytaleDevLib - Developer Utility Library for Hytale Modding

A utility library that simplifies common Hytale modding tasks by providing tested helpers, utilities, and workarounds for the Hytale API.

## Helper Classes Overview

### 1. EventHelper
Simplified event registration for common game events. Handles the boilerplate of registering global event listeners and parsing transaction data.

**What it does:**
- Registers event listeners without manual EventRegistry calls
- Parses `LivingEntityInventoryChangeEvent` transactions to extract item details
- Provides callbacks for item drops, pickups, player joins, chat, and disconnects

**When to use:**
- Detecting when players drop or pick up items
- Tracking player join/disconnect events
- Monitoring chat messages
- Avoiding manual transaction string parsing

---

### 1.5. EcsEventHelper
Simplified ECS event registration for block-related events. Automatically handles the complex ECS system registration required for events like block breaking and placing.

**What it does:**
- Creates and registers ECS systems automatically
- Provides simple callbacks for block breaking and placing
- Filters out false positives (e.g., "Empty" blocks during placement)
- Extracts block type and item information from events

**When to use:**
- Detecting when players break or place blocks
- Building protection systems or region management
- Tracking block modifications
- Creating custom building mechanics

**Important:** ECS events must be registered after you have a World instance, typically in the `AddPlayerToWorldEvent` callback.

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

#### `onPlayerJoinWorld(plugin, callback)`
Fires when a player joins the world.

```java
EventHelper.onPlayerJoinWorld(this, world -> {
    getLogger().at(Level.INFO).log("Player joined world: " + world.getName());
    // Example: Send welcome message, initialize player data
});
```

#### `onPlayerChat(plugin, callback)`
Detects when a player sends a chat message.

```java
EventHelper.onPlayerChat(this, (username, message) -> {
    getLogger().at(Level.INFO).log(username + " said: " + message);
    // Example: Chat filtering, command detection, logging
});
```

#### `onPlayerDisconnect(plugin, callback)`
Fires when a player disconnects from the server.

```java
EventHelper.onPlayerDisconnect(this, (username) -> {
    getLogger().at(Level.INFO).log(username + " left the server");
    // Example: Save player data, broadcast leave message
});
```

---

### EcsEventHelper Methods

EcsEventHelper provides simplified access to ECS-based events that require system registration. These methods automatically create and register the necessary ECS systems for you.

**Important:** ECS events must be registered after you have a World instance. Register them in the `AddPlayerToWorldEvent` callback.

#### `onBlockBreak(world, callback)`
Detects when a player breaks a block.

```java
// Register in AddPlayerToWorldEvent callback
this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
    World world = event.getWorld();
    
    EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
        getLogger().at(Level.INFO).log("Block broken: " + blockTypeId + " at " + position);
        // Example: Track mining, prevent breaking in protected areas, drop custom items
    });
});
```

**Features:**
- Automatically filters out "Empty" blocks (prevents false positives during block placement)
- Provides block type ID (e.g., "Soil_Dirt", "Rock_Stone")
- Provides exact block position as Vector3i

#### `onBlockPlace(world, callback)`
Detects when a player places a block.

```java
// Register in AddPlayerToWorldEvent callback
this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
    World world = event.getWorld();
    
    EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
        getLogger().at(Level.INFO).log("Block placed: " + itemId + " at " + position);
        // Example: Track building, prevent placing in protected areas, custom placement logic
    });
});
```

**Features:**
- Provides the item ID being placed from the player's hand
- Provides exact block position as Vector3i
- Fires for all block placements

#### Complete ECS Event Example

```java
@Override
protected void setup() {
    // Register simple global events
    EventHelper.onPlayerChat(this, (username, message) -> {
        getLogger().at(Level.INFO).log("[Chat] " + username + ": " + message);
    });
    
    EventHelper.onItemDrop(this, (itemId, quantity) -> {
        getLogger().at(Level.INFO).log("[Drop] " + quantity + "x " + itemId);
    });
    
    // Register ECS events when world is available
    this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, (event) -> {
        World world = event.getWorld();
        
        // Now we can register ECS events
        EcsEventHelper.onBlockBreak(world, (position, blockTypeId) -> {
            getLogger().at(Level.INFO).log("[Break] " + blockTypeId + " at " + position);
            
            // Example: Protected region system
            if (isInProtectedRegion(position)) {
                // Note: You can't cancel the event from here, but you can
                // restore the block or take other actions
                BlockHelper.setBlockByName(world, position, blockTypeId);
                WorldHelper.broadcastMessage(world, Message.raw("Cannot break blocks in protected area!"));
            }
        });
        
        EcsEventHelper.onBlockPlace(world, (position, itemId) -> {
            getLogger().at(Level.INFO).log("[Place] " + itemId + " at " + position);
            
            // Example: Build limit system
            if (isAboveBuildLimit(position)) {
                // Remove the placed block
                BlockHelper.setBlock(world, position, 0); // 0 = air
                WorldHelper.broadcastMessage(world, Message.raw("Cannot build above Y=100!"));
            }
        });
    });
}

private boolean isInProtectedRegion(Vector3i position) {
    // Check if position is in a protected area
    return position.getX() >= 0 && position.getX() <= 100 &&
           position.getZ() >= 0 && position.getZ() <= 100;
}

private boolean isAboveBuildLimit(Vector3i position) {
    return position.getY() > 100;
}
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

#### Time and Day System

Get and manipulate the in-game time and day cycle.

**Getting Time Information:**
```java
// Get current game date/time
LocalDateTime gameDateTime = WorldHelper.getGameDateTime(world);
WorldHelper.log(world, "Current time: " + gameDateTime);

// Get specific time components
int year = WorldHelper.getYear(world);              // Current year
int dayOfYear = WorldHelper.getDayOfYear(world);    // Day of year (1-365)
int hour = WorldHelper.getCurrentHour(world);       // Hour (0-23)

// Get day progress (0.0 = midnight, 0.5 = noon, 1.0 = next midnight)
float dayProgress = WorldHelper.getDayProgress(world);
WorldHelper.log(world, "Day is " + (dayProgress * 100) + "% complete");
```

**Day/Night Detection:**
```java
// Check if it's day or night
if (WorldHelper.isDaytime(world)) {
    WorldHelper.log(world, "It's daytime!");
}

if (WorldHelper.isNighttime(world)) {
    WorldHelper.log(world, "It's nighttime!");
}

// Get sunlight factor (0.0 = night, 1.0 = full daylight)
double sunlight = WorldHelper.getSunlightFactor(world);
WorldHelper.log(world, "Sunlight: " + (sunlight * 100) + "%");
```

**Moon Phase:**
```java
// Get current moon phase (0-7 by default)
int moonPhase = WorldHelper.getMoonPhase(world);
WorldHelper.log(world, "Moon phase: " + moonPhase);
```

**Setting Time:**
```java
// Set time of day (0.0-1.0)
WorldHelper.setDayTime(world, 0.0);   // Midnight
WorldHelper.setDayTime(world, 0.25);  // Sunrise
WorldHelper.setDayTime(world, 0.5);   // Noon
WorldHelper.setDayTime(world, 0.75);  // Sunset

// Set specific game time
Instant newTime = Instant.parse("2024-06-15T12:00:00Z");
WorldHelper.setGameTime(world, newTime);
```

**Practical Examples:**
```java
// Spawn hostile mobs only at night
if (WorldHelper.isNighttime(world)) {
    EntityHelper.spawnNPC(world, "Skeleton_Fighter", x, y, z);
}

// Change behavior based on time of day
int hour = WorldHelper.getCurrentHour(world);
if (hour >= 6 && hour < 18) {
    // Daytime behavior (6 AM - 6 PM)
    WorldHelper.log(world, "NPCs are active");
} else {
    // Nighttime behavior
    WorldHelper.log(world, "NPCs are sleeping");
}

// Moon phase events
int moonPhase = WorldHelper.getMoonPhase(world);
if (moonPhase == 0) {
    WorldHelper.log(world, "Full moon! Werewolves appear!");
}
```

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

#### `spawnNPC(world, roleName, position)` / `spawnNPC(world, roleName, x, y, z)`
Spawn an NPC entity by role name (recommended method).

```java
// Spawn a cow at a position
Vector3d spawnPos = new Vector3d(100, 64, 100);
Entity cow = EntityHelper.spawnNPC(world, "Cow", spawnPos);
if (cow != null) {
    WorldHelper.log(world, "Successfully spawned a Cow!");
}

// Spawn using coordinates
Entity deer = EntityHelper.spawnNPC(world, "Deer_Doe", 105, 64, 100);

// Spawn with rotation (yaw in radians)
Entity chicken = EntityHelper.spawnNPC(world, "Chicken", 110, 64, 100, (float) Math.PI);
```

**Available NPC Role Names:**
- **Animals:** `Cow`, `Pig`, `Sheep`, `Rabbit`, `Fox`, `Deer_Doe`, `Chicken`, `Horse`
- **Fish:** `Minnow`, `Salmon`, `Pike`, `Catfish`, `Bluegill`
- **Hostile:** `Skeleton_Fighter`, `Trork_Warrior`, `Goblin`, `Zombie`
- And many more! Any NPC role name in Hytale works, find a comprehensive list here: https://hytalemodding.dev/en/docs/server/entities

**How It Works:**
EntityHelper uses Hytale's internal `NPCPlugin.spawnEntity()` method which properly:
1. Looks up the role index by name
2. Creates all required ECS components (Transform, HeadRotation, DisplayName, UUID, Model, etc.)
3. Adds the entity to the EntityStore with proper initialization
4. Returns the spawned NPCEntity

**Example: Spawn multiple entities around player**
```java
Vector3d playerPos = EntityHelper.getPosition(player);

// Spawn a cow in front
Entity cow = EntityHelper.spawnNPC(world, "Cow", 
    playerPos.getX() + 5, playerPos.getY(), playerPos.getZ());

// Spawn a deer to the right
Entity deer = EntityHelper.spawnNPC(world, "Deer_Doe",
    playerPos.getX(), playerPos.getY(), playerPos.getZ() + 5);

// Spawn a chicken behind with rotation
Entity chicken = EntityHelper.spawnNPC(world, "Chicken",
    playerPos.getX() - 5, playerPos.getY(), playerPos.getZ(), (float) Math.PI);
```

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

BlockHelper now supports **name-based block operations** similar to Minecraft modding, making it much easier to work with blocks without memorizing numeric IDs.

#### Name-Based Block Setting (Recommended)

```java
// Set a block by name - much easier than using numeric IDs!
BlockHelper.setBlockByName(world, x, y, z, "Rock_Stone");
BlockHelper.setBlockByName(world, position, "Soil_Grass");

// Get block ID from name
int stoneId = BlockHelper.getBlockId("Rock_Stone"); // Returns 1032

// Fill a region with a named block
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);
int blocksSet = BlockHelper.fillRegionByName(world, corner1, corner2, "Rock_Stone");

// Replace blocks by name
int replaced = BlockHelper.replaceBlocksInRegionByName(
    world, corner1, corner2, 
    "Soil_Dirt",   // old block
    "Soil_Grass"   // new block
);

// Find blocks by name
List<Vector3i> diamonds = BlockHelper.findNearbyBlocksByName(world, playerPos, 50, "Ore_Copper_Stone");
```

See `lib/BlockIds.java` for the complete list of 3,951 block names.

#### `getBlockName(blockId)` / `getBlockName(world, position)` / `getBlockName(world, x, y, z)`
Get the human-readable block name from a block ID or position.

```java
// Get block name from numeric ID
int blockId = 684;
String blockName = BlockHelper.getBlockName(blockId);
WorldHelper.log(world, "Block: " + blockName); // e.g., "Rock_Stone"

// Get block name at a position
Vector3d pos = new Vector3d(100, 64, 100);
String name = BlockHelper.getBlockName(world, pos);
WorldHelper.log(world, "Block at position: " + name);

// Get block name at specific coordinates
String blockName = BlockHelper.getBlockName(world, 100, 64, 100);
```

**Note:** Block names are the internal asset IDs (e.g., "Rock_Stone", "Soil_Grass"). This is similar to how `EntityHelper.getEntityType()` returns entity role names.

#### `getBlock(world, position)` / `getBlock(world, x, y, z)`
Get the numeric block ID at a specific position.

```java
// Get block at a position
Vector3d pos = new Vector3d(100, 64, 100);
int blockId = BlockHelper.getBlock(world, pos);
WorldHelper.log(world, "Block ID: " + blockId);

// Get block at specific coordinates
int block = BlockHelper.getBlock(world, 100, 64, 100);

// Combine with getBlockName for readable output
int blockId = BlockHelper.getBlock(world, pos);
String blockName = BlockHelper.getBlockName(blockId);
WorldHelper.log(world, "Block: " + blockId + " (" + blockName + ")");
```

// Check if a position is air
if (BlockHelper.isAir(world, pos)) {
    WorldHelper.log(world, "Position is empty!");
}

#### `setBlockByName(world, position, blockName)` / `setBlockByName(world, x, y, z, blockName)`
Set a block by name at a specific position (recommended method).

```java
// Set a block by name - easy and readable!
boolean success = BlockHelper.setBlockByName(world, 100, 64, 100, "Rock_Stone");
if (success) {
    WorldHelper.log(world, "Block placed successfully!");
}

// Using Vector3d position
Vector3d pos = new Vector3d(100, 64, 100);
BlockHelper.setBlockByName(world, pos, "Soil_Grass");

// Example: Build a cobblestone path
for (int x = 100; x <= 110; x++) {
    BlockHelper.setBlockByName(world, x, 64, 100, "Rock_Stone_Cobble");
}
```

#### `setBlock(world, position, blockId)` / `setBlock(world, x, y, z, blockId)`
Set a block using numeric ID (use `setBlockByName` instead when possible).

```java
// Get block ID from name first
int stoneId = BlockHelper.getBlockId("Rock_Stone");

// Set using numeric ID
boolean success = BlockHelper.setBlock(world, 100, 64, 100, stoneId);

// Example: Copy a block from one location to another
int sourceBlock = BlockHelper.getBlock(world, 100, 64, 100);
BlockHelper.setBlock(world, 200, 64, 200, sourceBlock);
```

**💡 TIP:** Use name-based methods (`setBlockByName`, `fillRegionByName`, etc.) for better code readability. Numeric IDs are still useful for copying blocks or advanced operations.

#### `fillRegionByName(world, pos1, pos2, blockName)`
Fill a rectangular region with a named block type (recommended).

```java
// Create a stone platform
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);
int blocksSet = BlockHelper.fillRegionByName(world, corner1, corner2, "Rock_Stone");
WorldHelper.log(world, "Created platform with " + blocksSet + " blocks");

// Build a grass field
BlockHelper.fillRegionByName(world, corner1, corner2, "Soil_Grass");
```

#### `fillRegion(world, pos1, pos2, blockId)`
Fill a rectangular region with a block ID (use `fillRegionByName` when possible).

```java
// Using numeric ID
int stoneId = BlockHelper.getBlockId("Rock_Stone");
int blocksSet = BlockHelper.fillRegion(world, corner1, corner2, stoneId);
```

#### `replaceBlocksInRegionByName(world, pos1, pos2, oldBlockName, newBlockName)`
Replace all blocks of one type with another in a region by name (recommended).

```java
// Replace all dirt with grass in an area
Vector3d corner1 = new Vector3d(90, 60, 90);
Vector3d corner2 = new Vector3d(110, 70, 110);
int replaced = BlockHelper.replaceBlocksInRegionByName(
    world, corner1, corner2, 
    "Soil_Dirt",   // old block
    "Soil_Grass"   // new block
);
WorldHelper.log(world, "Replaced " + replaced + " dirt blocks with grass");

// Convert stone to cobblestone
BlockHelper.replaceBlocksInRegionByName(world, corner1, corner2, "Rock_Stone", "Rock_Stone_Cobble");
```

#### `replaceBlocksInRegion(world, pos1, pos2, oldBlockId, newBlockId)`
Replace blocks using numeric IDs (use `replaceBlocksInRegionByName` when possible).

```java
// Using numeric IDs
int dirtId = BlockHelper.getBlockId("Soil_Dirt");
int grassId = BlockHelper.getBlockId("Soil_Grass");
int replaced = BlockHelper.replaceBlocksInRegion(world, corner1, corner2, dirtId, grassId);
```

#### `findNearbyBlocksByName(world, center, radius, blockName)`
Find all positions of a specific block type by name within a radius (recommended).

```java
// Find all copper ore within 50 blocks
Vector3d playerPos = EntityHelper.getPosition(player);
List<Vector3i> copperOres = BlockHelper.findNearbyBlocksByName(world, playerPos, 50, "Ore_Copper_Stone");
WorldHelper.log(world, "Found " + copperOres.size() + " copper ore blocks nearby");

// Find all diamond ore
List<Vector3i> diamonds = BlockHelper.findNearbyBlocksByName(world, playerPos, 50, "Ore_Diamond");
for (Vector3i orePos : diamonds) {
    WorldHelper.log(world, "Diamond at: " + orePos.getX() + ", " + orePos.getY() + ", " + orePos.getZ());
}
```

#### `findNearbyBlocks(world, center, radius, blockId)`
Find blocks using numeric ID (use `findNearbyBlocksByName` when possible).

```java
// Using numeric ID
int diamondId = BlockHelper.getBlockId("Ore_Diamond");
List<Vector3i> diamonds = BlockHelper.findNearbyBlocks(world, playerPos, 50, diamondId);
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

// Create stone floor using name-based method
BlockHelper.fillRegionByName(world, corner1, corner2, "Rock_Stone");

// Create cobblestone walls (4 separate fills)
BlockHelper.fillRegionByName(world, new Vector3d(100, 65, 100), new Vector3d(100, 70, 110), "Rock_Stone_Cobble"); // West wall
BlockHelper.fillRegionByName(world, new Vector3d(110, 65, 100), new Vector3d(110, 70, 110), "Rock_Stone_Cobble"); // East wall
BlockHelper.fillRegionByName(world, new Vector3d(100, 65, 100), new Vector3d(110, 70, 100), "Rock_Stone_Cobble"); // North wall
BlockHelper.fillRegionByName(world, new Vector3d(100, 65, 110), new Vector3d(110, 70, 110), "Rock_Stone_Cobble"); // South wall
```

**Example 2: Ore detector command**
```java
EventHelper.onPlayerJoinWorld(plugin, world -> {
    // Register a command to find nearby ores
    world.getEventRegistry().registerListener(PlayerCommandEvent.class, event -> {
        if (event.getCommand().equals("/findores")) {
            Entity player = event.getPlayer();
            Vector3d pos = EntityHelper.getPosition(player);
            
            // Search for different ore types using name-based methods
            int diamondCount = BlockHelper.findNearbyBlocksByName(world, pos, 50, "Ore_Diamond").size();
            int goldCount = BlockHelper.findNearbyBlocksByName(world, pos, 50, "Ore_Gold").size();
            int copperCount = BlockHelper.findNearbyBlocksByName(world, pos, 50, "Ore_Copper_Stone").size();
            
            WorldHelper.log(world, "Ores within 50 blocks:");
            WorldHelper.log(world, "Diamond: " + diamondCount);
            WorldHelper.log(world, "Gold: " + goldCount);
            WorldHelper.log(world, "Copper: " + copperCount);
        }
    });
});
```

**Example 3: Terrain transformation**
```java
// Convert a dirt area to grass
Vector3d corner1 = new Vector3d(90, 60, 90);
Vector3d corner2 = new Vector3d(110, 70, 110);
int replaced = BlockHelper.replaceBlocksInRegionByName(world, corner1, corner2, "Soil_Dirt", "Soil_Grass");
WorldHelper.log(world, "Transformed " + replaced + " dirt blocks to grass");

// Create a stone path through grass
for (int x = 95; x <= 105; x++) {
    BlockHelper.setBlockByName(world, x, 64, 100, "Rock_Stone_Cobble");
}
```

**Example 4: Protected region system**
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

---

### How Block Setting Works (Technical Details)

When you call any `BlockHelper.setBlock()` or `setBlockByName()` method, the following happens automatically:

1. **Block Data Update**: The block is set in the `BlockChunk` via `blockChunk.setBlock(localX, y, localZ, blockId, rotation, filler)`
   - `blockId`: The numeric block type ID
   - `rotation`: Block rotation (0-23, typically 0 for no rotation)
   - `filler`: Filler block data (typically 0)

2. **Section Invalidation**: The `BlockChunk` automatically invalidates the chunk section's cached packet, marking it as needing a rebuild.

3. **Client Notification**: A `ServerSetBlock` packet is sent to all players who have that chunk loaded via `WorldNotificationHandler.sendPacketIfChunkLoaded()`. This ensures the block change is immediately visible to players.

**Why This Matters:**
Without sending the `ServerSetBlock` packet, blocks would be set on the server but clients wouldn't see the change until they reload the chunk (e.g., by moving far away and coming back). The packet ensures:
- **Immediate visibility**: Players see the block change instantly
- **Collision updates**: The client updates collision for the new block
- **Proper rendering**: The block mesh is rebuilt on the client

**All BlockHelper methods handle this automatically** - you don't need to manually send any packets. This includes:
- `setBlock()` / `setBlockByName()`
- `fillRegion()` / `fillRegionByName()`
- `replaceBlocksInRegion()` / `replaceBlocksInRegionByName()`

#### Advanced Usage: Block Rotation

```java
// Set a block with specific rotation
BlockHelper.setBlock(world, x, y, z, blockId, rotation, filler);

// rotation values: 0-23 (different orientations)
// filler: typically 0, used for special block states
```

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
