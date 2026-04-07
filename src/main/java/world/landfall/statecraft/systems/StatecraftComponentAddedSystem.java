package world.landfall.statecraft.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.components.StatecraftComponent;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;
import world.landfall.statecraft.util.CharacterOperations;
import world.landfall.statecraft.util.UtilCodecs;

import java.time.Instant;

public class StatecraftComponentAddedSystem extends RefSystem<EntityStore> {
    @Override
    public void onEntityAdded(@NonNull Ref<EntityStore> ref, @NonNull AddReason addReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        var world = store.getExternalData().getWorld();
        world.execute(() -> {
            // Load that player's characters



            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null || !playerRef.isValid()) return;
            var characterList = CharacterOperations.getCharacters(playerRef.getUuid());
            var characterTable = world.getChunkStore().getStore().getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE).TABLE;
            var inventory = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
            var entityStatMap = store.getComponent(ref, EntityStatMap.getComponentType());
            var chunkStore = store.getExternalData().getWorld().getChunkStore().getStore();
            Instant latestTime = Instant.MIN;
            long id = -1;
            for (var x : characterList) {
                if (x.getLastSeen().isAfter(latestTime)) {
                    latestTime = x.getLastSeen();
                    id = x.getCharacterId();

                }
            }
            if (id == -1) return;
            CharacterOperations.switchCharacters(ref, id);
        });
    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        var world = store.getExternalData().getWorld();
        var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);
        var stats = store.getComponent(ref, EntityStatMap.getComponentType());
//        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
//        if (playerRef == null || !playerRef.isValid()) return;
//        var transform = playerRef.getTransform();
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        var position = transform.getPosition();
        var table = store.getExternalData().getWorld().getChunkStore().getStore().getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE).TABLE;

//        var inventory = store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory();
        world.execute(() -> {
            if (characterComponent == null) return;
            table.put(characterComponent.character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                    UtilCodecs.PlayerInventory.fromPlayer(ref, store), stats, new PlayerSkin(), position
            ));
            for (var x : table.entrySet())
                System.out.println("DEBUG -- "+x.getKey() + " " + x.getValue().position);
        });
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(StatecraftMod.STATECRAFT_COMPONENT);
    }
}
