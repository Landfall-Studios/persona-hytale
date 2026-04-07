package world.landfall.statecraft.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.api.StatecraftCharacter;
import world.landfall.statecraft.components.CharacterComponent;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CharacterOperations {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static StatecraftCharacter createCharacter(Ref<EntityStore> player, String name) {
        var playerRef = player.getStore().getComponent(player, PlayerRef.getComponentType());
        var result = StatecraftMod.api.recordCharacter(playerRef.getUuid(), name);
        if (result.isSuccess()) {
            var character = result.getOrThrow();
            switchCharacters(player, character.getCharacterId());
        } else {
            LOGGER.atSevere().log("Couldn't create character: %s", result.getErrorMessage());
        }
        return result.getValue().get();
    }
    public static boolean switchCharacters(Ref<EntityStore> player, long characterId) {
        var entityStore = player.getStore();
        var world = entityStore.getExternalData().getWorld();
        var chunkStore = world.getChunkStore().getStore();
        var playerRef = entityStore.getComponent(player, PlayerRef.getComponentType());
        var playerComponent = entityStore.getComponent(player, Player.getComponentType());
        var playerTransform = entityStore.getComponent(player, TransformComponent.getComponentType());
        var playerPos = playerTransform.getPosition().clone();

        var stats = entityStore.getComponent(player, EntityStatMap.getComponentType());
//        var playerInventory = entityStore.getComponent(player, InventoryComponent.getComponentTypeById(InventoryComponent.STORAGE_SECTION_ID));
        var characterComponent = entityStore.getComponent(player, StatecraftMod.CHARACTER_COMPONENT);
        var characterTable = chunkStore.getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE);
        var newCharacterData = characterTable.TABLE.getOrDefault(characterId, StatecraftCharacterTableResource.LocalCharacterData.create());
        var characterList = getCharacters(player);
        if (characterComponent == null) {
            world.execute(() -> {
                characterList.forEach(c -> {if (c.getCharacterId() == characterId)
                    entityStore.putComponent(player, StatecraftMod.CHARACTER_COMPONENT, new CharacterComponent(c));});
            });
            return false;
        }
        var oldCharacter = characterComponent.character;


        world.execute(() -> {
            characterTable.TABLE.put(oldCharacter.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                UtilCodecs.PlayerInventory.fromPlayer(player, entityStore), stats, new PlayerSkin(), playerPos
            ));
            if (oldCharacter.getCharacterId() == characterId) return;
            System.out.println("DEBUG -- Old position: "+playerTransform.getPosition()+" New position: "+newCharacterData.position);
            System.out.println("DEBUG -- Old ID: "+oldCharacter.getCharacterId()+" New ID: "+characterId);
            entityStore.addComponent(player, Teleport.getComponentType(), new Teleport(newCharacterData.position, new Vector3f()));

            newCharacterData.inventory.applyToPlayer(player, entityStore);

            entityStore.putComponent(player, EntityStatMap.getComponentType(), newCharacterData.stats.clone());

            characterList.forEach(c -> {if (c.getCharacterId() == characterId)
                entityStore.putComponent(player, StatecraftMod.CHARACTER_COMPONENT, new CharacterComponent(c));});
        });
        return true;
    }
    public static List<StatecraftCharacter> getCharacters(Ref<EntityStore> player) {
        return StatecraftMod.api.getCharactersByPlayer(player.getStore().getComponent(player, PlayerRef.getComponentType()).getUuid()).getOrElse(List.of());
    }
    public static List<StatecraftCharacter> getCharacters(UUID player) {
        return StatecraftMod.api.getCharactersByPlayer(player).getOrElse(List.of());
    }
}
