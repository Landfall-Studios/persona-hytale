package world.landfall.statecraft.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.api.StatecraftCharacter;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;

import java.util.ArrayList;
import java.util.List;

public class CharacterOperations {
    public static boolean createCharacter(Ref<EntityStore> player, String name) {
        var playerRef = player.getStore().getComponent(player, PlayerRef.getComponentType());
        var result = StatecraftMod.api.recordCharacter(playerRef.getUuid(), name);
        if (result.isSuccess()) {
            var character = result.getOrThrow();
            switchCharacters(player, character.getCharacterId());
        }
        return result.isSuccess();
    }
    public static boolean switchCharacters(Ref<EntityStore> player, long characterId) {
        var entityStore = player.getStore();
        var world = entityStore.getExternalData().getWorld();
        var chunkStore = world.getChunkStore().getStore();
        var playerRef = entityStore.getComponent(player, PlayerRef.getComponentType());
        var playerComponent = entityStore.getComponent(player, Player.getComponentType());
        var playerTransform = entityStore.getComponent(player, TransformComponent.getComponentType());
        var inventory = InventoryComponent.getCombined(entityStore, player, InventoryComponent.Storage.getComponentType());
        var stats = entityStore.getComponent(player, EntityStatMap.getComponentType());
//        var playerInventory = entityStore.getComponent(player, InventoryComponent.getComponentTypeById(InventoryComponent.STORAGE_SECTION_ID));
        var characterComponent = entityStore.getComponent(player, StatecraftMod.CHARACTER_COMPONENT);
        if (characterComponent == null) {

            return false;
        }
        var character = characterComponent.character;
        if (character.getCharacterId() == characterId) return false;
        var characterTable = chunkStore.getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE);
        var newCharacterData = characterTable.TABLE.getOrDefault(characterId, StatecraftCharacterTableResource.LocalCharacterData.create());

        world.execute(() -> {
            characterTable.TABLE.put(character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                inventory, stats, new PlayerSkin(), playerTransform.getPosition()
            ));
            entityStore.addComponent(player, Teleport.getComponentType(), new Teleport(newCharacterData.position, new Vector3f()));
            var combined = ((CombinedItemContainer)newCharacterData.inventory.clone());

            entityStore.putComponent(player, InventoryComponent.Storage.getComponentType(), new InventoryComponent.Storage(combined.getContainer(0)));
            entityStore.putComponent(player, EntityStatMap.getComponentType(), newCharacterData.stats.clone());
        });
        return true;
    }
    public static List<StatecraftCharacter> getCharacters(Ref<EntityStore> player) {
        return StatecraftMod.api.getCharactersByPlayer(player.getStore().getComponent(player, PlayerRef.getComponentType()).getUuid()).getOrElse(List.of());
    }
}
