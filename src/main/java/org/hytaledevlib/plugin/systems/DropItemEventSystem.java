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
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * ECS System to handle DropItemEvent - this is the correct way to handle events in Hytale.
 */
public class DropItemEventSystem extends EntityEventSystem<EntityStore, DropItemEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public DropItemEventSystem() {
        super(DropItemEvent.class);
        LOGGER.atInfo().log("DropItemEventSystem initialized!");
    }

    @Override
    public void handle(final int index, @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk, 
                       @Nonnull final Store<EntityStore> store, 
                       @Nonnull final CommandBuffer<EntityStore> commandBuffer, 
                       @Nonnull final DropItemEvent event) {
        try {
            LOGGER.atInfo().log("=== DropItemEvent via ECS System ===");
            LOGGER.atInfo().log("Event type: " + event.getClass().getName());
            
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            
            // Try to get player info if available
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                LOGGER.atInfo().log("Player dropping item: " + player.getDisplayName());
            }
            
            // Check if this is a Drop event with ItemStack
            if (event instanceof DropItemEvent.Drop dropEvent) {
                ItemStack itemStack = dropEvent.getItemStack();
                if (itemStack != null && !itemStack.isEmpty()) {
                    String itemId = itemStack.getItemId();
                    int quantity = itemStack.getQuantity();
                    LOGGER.atInfo().log("Item dropped: " + itemId + " x" + quantity);
                }
            } else if (event instanceof DropItemEvent.PlayerRequest) {
                LOGGER.atInfo().log("PlayerRequest to drop item detected");
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Error in DropItemEventSystem: " + e.getMessage());
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
