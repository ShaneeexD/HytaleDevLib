package org.hytaledevlib.plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * ECS System to handle BreakBlockEvent - used to test if ECS systems work.
 */
public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BreakBlockEventSystem() {
        super(BreakBlockEvent.class);
        LOGGER.atInfo().log("BreakBlockEventSystem initialized!");
    }

    @Override
    public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk, 
                       @Nonnull final Store<EntityStore> store, 
                       @Nonnull final CommandBuffer<EntityStore> commandBuffer, 
                       @Nonnull final BreakBlockEvent event) {
        try {
            LOGGER.atInfo().log("=== BreakBlockEvent via ECS System ===");
            
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(ref, Player.getComponentType());
            
            if (player != null) {
                LOGGER.atInfo().log("Player broke block: " + player.getDisplayName());
            }
            
            LOGGER.atInfo().log("Block position: " + event.getTargetBlock());
        } catch (Exception e) {
            LOGGER.atWarning().log("Error in BreakBlockEventSystem: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
