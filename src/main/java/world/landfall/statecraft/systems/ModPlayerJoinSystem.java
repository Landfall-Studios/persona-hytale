package world.landfall.statecraft.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.components.StatecraftComponent;

public class ModPlayerJoinSystem extends PlayerSystems.PlayerInitSystem {

    @Override
    public void onEntityAdd(@NonNull Holder<EntityStore> holder, @NonNull AddReason reason, @NonNull Store<EntityStore> store) {
        super.onEntityAdd(holder, reason, store);

        holder.ensureComponent(StatecraftMod.STATECRAFT_COMPONENT);
    }
}
