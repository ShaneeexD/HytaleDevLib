# Block Mapping Usage Guide

The BlockMapping system allows you to work with block names instead of numeric IDs, similar to Minecraft modding.

## Quick Start

```java
import org.grounditems.lib.BlockHelper;
import org.grounditems.lib.BlockMapping;

// Set a block by name (Minecraft-style)
BlockHelper.setBlockByName(world, 100, 64, 100, "Rock_Stone");

// Get block name at position
String blockName = BlockHelper.getBlockName(world, 100, 64, 100);
WorldHelper.log(world, "Block: " + blockName); // "Rock_Stone"

// Check if block exists
if (BlockMapping.hasBlock("Soil_Grass")) {
    WorldHelper.log(world, "Grass block exists!");
}
```

## Available Methods

### BlockMapping Class

```java
// Get block ID from name
int stoneId = BlockMapping.getBlockId("Rock_Stone");

// Get block name from ID
String blockName = BlockMapping.getBlockName(684);

// Check if block exists
boolean exists = BlockMapping.hasBlock("Soil_Grass");

// Get total block count
int count = BlockMapping.getBlockCount(); // 3951 blocks
```

### BlockHelper Name-Based Methods

```java
// Set block by name
BlockHelper.setBlockByName(world, x, y, z, "Rock_Stone");
BlockHelper.setBlockByName(world, position, "Soil_Grass");

// Get block ID from name
int blockId = BlockHelper.getBlockId("Rock_Stone");

// Fill region with named block
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);
int blocksSet = BlockHelper.fillRegionByName(world, corner1, corner2, "Rock_Stone");

// Replace blocks by name
int replaced = BlockHelper.replaceBlocksInRegionByName(
    world, corner1, corner2, 
    "Soil_Dirt",  // old block
    "Soil_Grass"  // new block
);

// Find blocks by name
List<Vector3i> ores = BlockHelper.findNearbyBlocksByName(
    world, playerPos, 50, "Ore_Diamond"
);
```

## Common Block Names

Here are some commonly used block names:

### Terrain
- `Empty` - Air (ID: 0)
- `Soil_Dirt` - Dirt
- `Soil_Grass` - Grass
- `Soil_Gravel_Sand` - Sand
- `Soil_Gravel_Stone` - Gravel
- `Soil_Clay` - Clay
- `Soil_Snow` - Snow

### Stone Types
- `Rock_Stone` - Stone
- `Rock_Marble` - Marble
- `Rock_Chalk` - Chalk
- `Rock_Slate` - Slate
- `Rock_Quartzite` - Quartzite
- `Rock_Sandstone` - Sandstone
- `Rock_Basalt` - Basalt
- `Rock_Volcanic` - Volcanic rock

### Ores
- `Ore_Coal` - Coal ore
- `Ore_Copper` - Copper ore
- `Ore_Iron` - Iron ore
- `Ore_Gold` - Gold ore
- `Ore_Diamond` - Diamond ore
- `Ore_Emerald` - Emerald ore
- `Ore_Ruby` - Ruby ore
- `Ore_Sapphire` - Sapphire ore

### Wood Types
- `Wood_Softwood` - Softwood
- `Wood_Hardwood` - Hardwood
- `Wood_Redwood` - Redwood
- `Wood_Blackwood` - Blackwood
- `Wood_Greenwood` - Greenwood

### Processed Materials
- `Rock_Stone_Cobble` - Cobblestone
- `Rock_Stone_Brick` - Stone bricks
- `Wood_Softwood_Planks` - Wooden planks

## Practical Examples

### Example 1: Build a stone platform
```java
Vector3d corner1 = new Vector3d(100, 64, 100);
Vector3d corner2 = new Vector3d(110, 64, 110);

// Fill with stone
BlockHelper.fillRegionByName(world, corner1, corner2, "Rock_Stone");
WorldHelper.log(world, "Created stone platform!");
```

### Example 2: Convert dirt to grass
```java
Vector3d corner1 = new Vector3d(90, 60, 90);
Vector3d corner2 = new Vector3d(110, 70, 110);

// Replace all dirt with grass
int replaced = BlockHelper.replaceBlocksInRegionByName(
    world, corner1, corner2, 
    "Soil_Dirt", 
    "Soil_Grass"
);
WorldHelper.log(world, "Converted " + replaced + " dirt blocks to grass!");
```

### Example 3: Find nearby diamonds
```java
Vector3d playerPos = EntityHelper.getPosition(player);

// Search for diamonds within 50 blocks
List<Vector3i> diamonds = BlockHelper.findNearbyBlocksByName(
    world, playerPos, 50, "Ore_Diamond"
);

if (diamonds.isEmpty()) {
    WorldHelper.log(world, "No diamonds found nearby!");
} else {
    WorldHelper.log(world, "Found " + diamonds.size() + " diamond ore blocks!");
    
    // Show closest diamond
    Vector3i closest = diamonds.get(0);
    WorldHelper.log(world, "Closest diamond at: " + 
        closest.getX() + ", " + closest.getY() + ", " + closest.getZ());
}
```

### Example 4: Create a command to set blocks by name
```java
world.getEventRegistry().registerListener(PlayerCommandEvent.class, event -> {
    String[] args = event.getCommand().split(" ");
    
    if (args[0].equals("/setblock") && args.length == 5) {
        try {
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);
            String blockName = args[4];
            
            boolean success = BlockHelper.setBlockByName(world, x, y, z, blockName);
            
            if (success) {
                WorldHelper.log(world, "Set block at " + x + "," + y + "," + z + " to " + blockName);
            } else {
                WorldHelper.log(world, "Failed to set block! Invalid name or chunk not loaded.");
            }
        } catch (NumberFormatException e) {
            WorldHelper.log(world, "Usage: /setblock <x> <y> <z> <blockName>");
        }
    }
});
```

## Benefits Over Numeric IDs

1. **Readable Code**: `"Rock_Stone"` is more readable than `684`
2. **Self-Documenting**: Block names describe what they are
3. **Easier Maintenance**: No need to look up block IDs
4. **Familiar**: Works like Minecraft modding
5. **Type-Safe**: Invalid block names return -1 or false

## Performance

The BlockMapping system uses HashMaps for O(1) lookup performance. The mapping is loaded once on first use and cached in memory, so there's minimal performance overhead compared to using numeric IDs directly.

## Total Blocks Available

The current Hytale build contains **3,951 unique block types**!
