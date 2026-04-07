package world.landfall.statecraft.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import world.landfall.statecraft.StatecraftMod;

public class ModPlayerJoinSystem extends PlayerSystems.PlayerInitSystem {

    @Override
    public void onEntityAdd(@NonNull Holder<EntityStore> holder, @NonNull AddReason reason, @NonNull Store<EntityStore> store) {
        super.onEntityAdd(holder, reason, store);

        holder.ensureComponent(StatecraftMod.STATECRAFT_COMPONENT);
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        var inventory = holder.getComponent(InventoryComponent.Storage.getComponentType());
        var entityStatMap = holder.getComponent(EntityStatMap.getComponentType());
        var chunkStore = store.getExternalData().getWorld().getChunkStore().getStore();
        //actually this only needs to be set when we need it for save/load
//        var playerTable = chunkStore.getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE).TABLE.putIfAbsent(playerRef.getUuid(),
//                new StatecraftPlayerTableResource.LocalCharacterData(inventory.getInventory().clone(), entityStatMap));



    }
}
