package org.hytaledevlib.tools;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tool to generate a markdown file listing all available blocks with their IDs.
 * This uses the game's native BlockType asset system to get accurate, up-to-date block information.
 */
public class BlockListGenerator {
    
    public static class BlockInfo implements Comparable<BlockInfo> {
        public final int id;
        public final String name;
        
        public BlockInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public int compareTo(BlockInfo other) {
            return this.name.compareTo(other.name);
        }
    }
    
    /**
     * Generate a formatted markdown list of all blocks.
     * Call this from TestPlugin when a world is available.
     * 
     * @param outputPath Path to write the markdown file
     * @return Number of blocks exported
     */
    public static int generateBlockList(String outputPath) {
        System.out.println("[BlockListGenerator] Starting block list generation...");
        try {
            System.out.println("[BlockListGenerator] Getting BlockType asset map...");
            BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
            
            if (assetMap == null) {
                System.err.println("[BlockListGenerator] ERROR: Asset map is null!");
                return -1;
            }
            
            List<BlockInfo> blocks = new ArrayList<>();
            
            // Get the next index to know how many blocks exist
            int maxIndex = assetMap.getNextIndex();
            System.out.println("[BlockListGenerator] Max block index: " + maxIndex);
            
            // Iterate through all block IDs and collect valid blocks
            for (int i = 0; i < maxIndex; i++) {
                BlockType blockType = assetMap.getAsset(i);
                if (blockType != null) {
                    String name = blockType.getId();
                    if (name != null && !name.isEmpty()) {
                        blocks.add(new BlockInfo(i, name));
                    }
                }
            }
            
            System.out.println("[BlockListGenerator] Found " + blocks.size() + " valid blocks");
            
            // Sort alphabetically by name
            Collections.sort(blocks);
            
            // Write to markdown file
            System.out.println("[BlockListGenerator] Writing to file: " + outputPath);
            java.io.File outputFile = new java.io.File(outputPath);
            System.out.println("[BlockListGenerator] Absolute path will be: " + outputFile.getAbsolutePath());
            
            // Create parent directories if they don't exist
            java.io.File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                System.out.println("[BlockListGenerator] Creating directory: " + parentDir.getAbsolutePath());
                parentDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write("# Complete Block List\n\n");
                writer.write("This is a complete list of all blocks available in Hytale, automatically generated from the game's asset system.\n\n");
                writer.write("**Total Blocks:** " + blocks.size() + "\n\n");
                writer.write("**Usage with BlockHelper:**\n");
                writer.write("```java\n");
                writer.write("// Set a block by name\n");
                writer.write("BlockHelper.setBlockByName(world, x, y, z, \"Rock_Stone\");\n\n");
                writer.write("// Get block ID from name\n");
                writer.write("int blockId = BlockHelper.getBlockId(\"Soil_Grass\");\n");
                writer.write("```\n\n");
                writer.write("---\n\n");
                writer.write("## Block List\n\n");
                writer.write("| Block Name | Block ID |\n");
                writer.write("|------------|----------|\n");
                
                for (BlockInfo block : blocks) {
                    writer.write(String.format("| `%s` | %d |\n", block.name, block.id));
                }
                
                writer.write("\n---\n\n");
                writer.write("*Generated automatically using BlockHelper's dynamic asset system*\n");
            }
            
            System.out.println("[BlockListGenerator] ✓ Successfully wrote " + blocks.size() + " blocks to " + outputPath);
            return blocks.size();
            
        } catch (IOException e) {
            System.err.println("[BlockListGenerator] ERROR writing file: " + e.getMessage());
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            System.err.println("[BlockListGenerator] ERROR: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
