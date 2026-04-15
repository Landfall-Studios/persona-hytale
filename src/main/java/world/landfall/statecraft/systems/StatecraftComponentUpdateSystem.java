package world.landfall.statecraft.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.PlayerSkinUpdate;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.config.StatecraftConfig;
import world.landfall.statecraft.util.CharacterOperations;
import world.landfall.statecraft.util.Util;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class StatecraftComponentUpdateSystem extends DelayedEntitySystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public StatecraftComponentUpdateSystem() {
        super(StatecraftConfig.SYNC_INTERVAL_SECONDS.get());
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        var ref = archetypeChunk.getReferenceTo(index);
        if (!ref.isValid()) return;
        var player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return;
        LOGGER.atFine().log("Refreshing data for player %s", player.getUsername());

        var world = store.getExternalData().getWorld();

        var api = StatecraftMod.api;
        var characters = api.getCharactersByPlayer(player.getUuid()).getOrElse(List.of());
        var table = Util.getCharacterTable();
        var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);
        if (characterComponent != null) {
            // Are we in a glitched state? Get that component outa here, it'll break shit
            if (!table.containsKey(characterComponent.character.getCharacterId())) {
                world.execute(() -> store.removeComponent(ref, StatecraftMod.CHARACTER_COMPONENT));
                return;
            }
            // Already is logged in with a character
            world.execute(() -> CharacterOperations.refreshModel(ref, store));


        } else {
            // Doesn't have a character (new player/character was deleted)
            player.sendMessage(Message.join(Message.raw("[STATECRAFT] ").bold(true).color(Color.CYAN), Message.raw("No character found! Run /character (or /char) to make one!")));
        }

    }
    

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(StatecraftMod.STATECRAFT_COMPONENT);
    }
}
