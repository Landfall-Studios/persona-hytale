package world.landfall.statecraft.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.commands.server.KickCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.components.StatecraftComponent;
import world.landfall.statecraft.config.StatecraftConfig;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;
import world.landfall.statecraft.util.CharacterOperations;
import world.landfall.statecraft.util.Util;
import world.landfall.statecraft.util.UtilCodecs;

import java.time.Instant;

public class StatecraftComponentAddedSystem extends RefChangeSystem<EntityStore, StatecraftComponent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(StatecraftMod.STATECRAFT_COMPONENT);
    }

    @Override
    public @NonNull ComponentType<EntityStore, StatecraftComponent> componentType() {
        return StatecraftMod.STATECRAFT_COMPONENT;
    }

    @Override
    public void onComponentAdded(@NonNull Ref<EntityStore> ref, @NonNull StatecraftComponent statecraftComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        var world = store.getExternalData().getWorld();
        var table = Util.getCharacterTable();


        world.execute(() -> {
            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;
            LOGGER.atInfo().log("Added Statecraft Component to player %s", playerRef.getUsername());
            var currentModel = store.getComponent(ref, PlayerSkinComponent.getComponentType()).getPlayerSkin();

            Util.UUID_TO_SKIN.put(playerRef.getUuid(), currentModel.clone());
            // Load that player's characters
            var player = store.getComponent(ref, Player.getComponentType());
            if (!playerRef.isValid()) return;
            var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);

            if (characterComponent == null) {
                var requireCharacter = StatecraftConfig.REQUIRE_CHARACTER_ON_JOIN.get();
                if (requireCharacter) {
                    playerRef.getPacketHandler().disconnect(Message.raw("This server requires you to set up a character before joining!"));
                }
                return;
            }
            // Are we in a glitched state? Get that component outa here, it'll break shit
            if (!table.containsKey(characterComponent.character.getCharacterId())) {
                store.removeComponent(ref, StatecraftMod.CHARACTER_COMPONENT);

                return;
            }
            if (!ref.isValid()) return;


            var characterList = CharacterOperations.getCharacters(playerRef.getUuid());
            Instant latestTime = Instant.MIN;
            long id = -1;
            for (var x : characterList) {
                if (x.getLastSeen().isAfter(latestTime)) {
                    latestTime = x.getLastSeen();
                    id = x.getCharacterId();

                }
            }
            if (id != -1)
                CharacterOperations.switchCharactersNoTeleport(ref, id);
            world.execute(() -> CharacterOperations.refreshModel(ref, store));
        });
    }

    @Override
    public void onComponentSet(@NonNull Ref<EntityStore> ref, @Nullable StatecraftComponent statecraftComponent, @NonNull StatecraftComponent t1, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Override
    public void onComponentRemoved(@NonNull Ref<EntityStore> ref, @NonNull StatecraftComponent statecraftComponent, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        var table = Util.getCharacterTable();
        var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);
        if (characterComponent == null) return;
        var transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        var characterData = table.get(characterComponent.character.getCharacterId());
        if (characterData == null) return;
        table.put(characterComponent.character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                UtilCodecs.PlayerInventory.fromPlayer(ref, store),
                characterData.stats,
                characterData.playerSkin,
                transform.getPosition(),
                characterData.icon

        ));
    }
}
