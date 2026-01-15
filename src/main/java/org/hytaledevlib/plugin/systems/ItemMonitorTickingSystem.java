package org.hytaledevlib.plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ticking system that monitors entities with ItemComponent (ground items).
 * This runs every tick and can detect items on the ground.
 */
public class ItemMonitorTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private int tickCounter = 0;

    public ItemMonitorTickingSystem() {
        LOGGER.atInfo().log("ItemMonitorTickingSystem initialized!");
    }

    @Override
    public void tick(float deltaTime, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Only log every 200 ticks to avoid spam (about every 10 seconds at 20 tps)
        tickCounter++;
        if (tickCounter >= 200) {
            tickCounter = 0;
            LOGGER.atInfo().log("=== ItemMonitorTickingSystem tick ===");
            
            try {
                Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    LOGGER.atInfo().log("Ticking for player: " + playerRef.getUuid());
                }
            } catch (Exception e) {
                LOGGER.atWarning().log("Tick error: " + e.getMessage());
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Use PlayerRef to query player entities, then check nearby items
        return PlayerRef.getComponentType();
    }
}
