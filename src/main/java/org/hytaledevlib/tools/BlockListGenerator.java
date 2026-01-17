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
        try {
            BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
            List<BlockInfo> blocks = new ArrayList<>();
            
            // Get the next index to know how many blocks exist
            int maxIndex = assetMap.getNextIndex();
            
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
            
            // Sort alphabetically by name
            Collections.sort(blocks);
            
            // Write to markdown file
            try (FileWriter writer = new FileWriter(outputPath)) {
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
            
            return blocks.size();
            
        } catch (IOException e) {
            System.err.println("Error writing block list: " + e.getMessage());
            return -1;
        } catch (Exception e) {
            System.err.println("Error generating block list: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
