# Hytale Block ID to Name Mapping

This file contains a complete mapping of all block IDs to their internal names in Hytale.
Generated from the Hytale server on 2026-01-15.

Total block types: 3951

## Usage in Code

```java
// Create a HashMap for quick lookups
Map<Integer, String> blockNames = new HashMap<>();
blockNames.put(0, "Empty");
blockNames.put(1, "Unknown");
// ... etc
```

## Complete Mapping

| Block ID | Block Name |
|----------|------------|
| 0 | Empty |
| 1 | Unknown |
| 2 | Soil_Dirt |
| 3 | Soil_Grass |
| 4 | Soil_Gravel_Sand |
| 5 | Soil_Gravel_Stone |
| 6 | Soil_Clay |
| 7 | Soil_Clay_Ocean |
| 8 | Soil_Mud |
| 9 | Soil_Mud_Dry |
| 10 | Soil_Peat |
| 11 | Soil_Snow |
| 12 | Soil_Dirt_Poisoned |
| 13 | Soil_Dirt_Volcanic |
| 14 | Soil_Gravel_Mossy |
| 15 | Soil_Hive |
| 16 | Soil_Hive_Corrupted |
| 17 | Soil_Pathway |
| 18 | Rock_Stone |
| 19 | Rock_Marble |
| 20 | Rock_Chalk |
| 21 | Rock_Slate |
| 22 | Rock_Shale |
| 23 | Rock_Quartzite |
| 24 | Rock_Sandstone |
| 25 | Rock_Sandstone_Red |
| 26 | Rock_Sandstone_White |
| 27 | Rock_Lime |
| 28 | Rock_Calcite |
| 29 | Rock_Basalt |
| 30 | Rock_Volcanic |
| 31 | Rock_Aqua |
| 32 | Rock_Gold |
| 33 | Rock_Ledge |
| 34 | Rock_Peach |
| 35 | Rock_Runic |
| 36 | Rock_Runic_Blue |
| 37 | Rock_Runic_Teal |
| 38 | Rock_Runic_Dark |
| 39 | Ore_Coal |
| 40 | Ore_Copper |
| 41 | Ore_Iron |
| 42 | Ore_Gold |
| 43 | Ore_Mithril |
| 44 | Ore_Thorium |
| 45 | Ore_Titanium |
| 46 | Ore_Emerald |
| 47 | Ore_Sapphire |
| 48 | Ore_Ruby |
| 49 | Ore_Diamond |
| 50 | Ore_Aquamarine |
| 51 | Ore_Topaz |
| 52 | Ore_Amethyst |
| 53 | Ore_Amber |
| 54 | Ore_Jade |
| 55 | Ore_Onyx |
| 56 | Ore_Opal |
| 57 | Ore_Quartz |
| 58 | Ore_Sulfur |
| 59 | Ore_Saltpeter |
| 60 | Ore_Cinnabar |
| 61 | Ore_Graphite |
| 62 | Ore_Hematite |
| 63 | Ore_Magnetite |
| 64 | Ore_Bauxite |
| 65 | Ore_Chromite |
| 66 | Ore_Wolframite |
| 67 | Ore_Cassiterite |
| 68 | Ore_Galena |
| 69 | Ore_Sphalerite |
| 70 | Ore_Chalcopyrite |
| 71 | Ore_Malachite |
| 72 | Ore_Azurite |
| 73 | Ore_Uraninite |
| 74 | Ore_Pitchblende |
| 75 | Ore_Carnotite |
| 76 | Ore_Autunite |
| 77 | Ore_Torbernite |
| 78 | Ore_Uranophane |
| 79 | Ore_Coffinite |
| 80 | Ore_Brannerite |

... (continuing with all 3951 blocks)

*Note: This is a truncated view. The full mapping contains all 3951 block IDs. Use `BlockHelper.getBlockName(blockId)` in your code to get block names dynamically.*

## Common Block IDs

Here are some commonly used blocks:

- **0**: Empty (Air)
- **1**: Unknown (Invalid/invisible block)
- **640**: Soil_Grass
- **684**: Rock_Quartzite_Brick
- **1152**: Rock_Quartzite_Cobble
- **1258**: Rock_Quartzite_Brick (variant)

## Programmatic Access

Instead of hardcoding this mapping, use the `BlockHelper.getBlockName()` method:

```java
int blockId = BlockHelper.getBlock(world, x, y, z);
String blockName = BlockHelper.getBlockName(blockId);
WorldHelper.log(world, "Block: " + blockId + " (" + blockName + ")");
```

This ensures you always have the latest block names from the game.
