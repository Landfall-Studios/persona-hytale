package world.landfall.statecraft.util;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.system.ModelSystems;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.api.StatecraftCharacter;
import world.landfall.statecraft.components.CharacterComponent;
import world.landfall.statecraft.config.StatecraftConfig;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CharacterOperations {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static StatecraftCharacter createCharacter(Ref<EntityStore> player, String name, StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon icon) {

        var playerRef = player.getStore().getComponent(player, PlayerRef.getComponentType());
        var world = player.getStore().getExternalData().getWorld();
        if (!world.getName().equals(World.DEFAULT)) {
            playerRef.sendMessage(Message.raw("Can't perform character actions outside of Orbis!").color(Color.RED));
            return null;
        }
//        AtomicBoolean keepGoing = new AtomicBoolean(true);
        var currentCharacters = StatecraftMod.api.getCharactersByPlayer(playerRef.getUuid());
        if (currentCharacters.isSuccess() && currentCharacters.getValue().get().stream().anyMatch(c -> c.getDisplayName().equals(name))) {
            playerRef.sendMessage(Message.raw("Could not create character: Character by this name already exists!").color(Color.RED));
            return null;
        }

        if (currentCharacters.isSuccess() && currentCharacters.getValue().get().size() >= StatecraftConfig.MAX_CHARACTERS_PER_PLAYER.get()) {
            playerRef.sendMessage(Message.raw("You have reached your character limit!").color(Color.RED));
            return null;
        }
        var nameValidationRegex = StatecraftConfig.NAME_VALIDATION_REGEX.get();
        if (!nameValidationRegex.isEmpty() && !name.matches(nameValidationRegex)) {
            playerRef.sendMessage(Message.raw("You must follow the naming rules!").color(Color.RED));
            playerRef.sendMessage(Message.raw("First name + last name").color(Color.RED));

        }

        var result = StatecraftMod.api.recordCharacter(playerRef.getUuid(), name);
        var store = player.getStore();
        var skin = store.getComponent(player, PlayerSkinComponent.getComponentType());
        var table = Util.getCharacterTable();
        if (result.isSuccess()) {
            var character = result.getOrThrow();
            table.put(character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                    new UtilCodecs.PlayerInventory(), new EntityStatMap(), skin.getPlayerSkin(), playerRef.getTransform().getPosition(), icon
            ));
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
        if (!world.getName().equals(World.DEFAULT)) {
            playerRef.sendMessage(Message.raw("Can't perform character actions outside of Orbis!").color(Color.RED));
            return false;
        };
        var playerTransform = entityStore.getComponent(player, TransformComponent.getComponentType());
        var playerPos = playerTransform.getPosition().clone();
        var characterAPIData = StatecraftMod.api.getCharacterById(characterId);
        if (characterAPIData.isFailure()) {
            playerRef.sendMessage(Message.raw("Could not access character in ScAPI!").color(Color.RED));
            return false;
        } else {
            var temp = characterAPIData.getValue().get().get();
            if (temp.isDeceased()) {
                playerRef.sendMessage(Message.raw("Character is deceased!").color(Color.RED));
                return false;
            }
            if (StatecraftConfig.ENABLE_NAME_SYSTEM.get())
                entityStore.putComponent(player, Nameplate.getComponentType(), new Nameplate(temp.getDisplayName()));

        }

        var stats = entityStore.getComponent(player, EntityStatMap.getComponentType());
//        var playerInventory = entityStore.getComponent(player, InventoryComponent.getComponentTypeById(InventoryComponent.STORAGE_SECTION_ID));
        var characterComponent = entityStore.getComponent(player, StatecraftMod.CHARACTER_COMPONENT);
        var characterTable = Util.getCharacterTable();
        var newCharacterData = characterTable.getOrDefault(characterId, new StatecraftCharacterTableResource.LocalCharacterData(
                UtilCodecs.PlayerInventory.fromPlayer(player, entityStore), stats, new PlayerSkin(), playerPos, StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon.ANGEL
        ));
        var characterList = getCharacters(player);
        if (characterComponent == null) {
            world.execute(() -> {
                var savedStats = stats.clone();
                characterTable.put(characterId, new StatecraftCharacterTableResource.LocalCharacterData(
                        UtilCodecs.PlayerInventory.fromPlayer(player, entityStore), savedStats, characterTable.get(characterId).playerSkin, playerPos, characterTable.get(characterId).icon
                ));
                characterList.forEach(c -> {if (c.getCharacterId() == characterId)
                    entityStore.putComponent(player, StatecraftMod.CHARACTER_COMPONENT, new CharacterComponent(c));});
                refreshModel(player, entityStore);
            });
            return false;
        }
        var oldCharacter = characterComponent.character;


        world.execute(() -> {
            var savedStats = stats.clone();
            characterTable.put(oldCharacter.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                UtilCodecs.PlayerInventory.fromPlayer(player, entityStore), savedStats, characterTable.get(oldCharacter.getCharacterId()).playerSkin, playerPos, characterTable.get(oldCharacter.getCharacterId()).icon
            ));
            if (oldCharacter.getCharacterId() == characterId) return;
//            playerRef.sendMessage(Message.raw("StatMap Data: "+stats.get(DefaultEntityStatTypes.getHealth())+" "+newCharacterData.stats.get(DefaultEntityStatTypes.getHealth())));
            entityStore.addComponent(player, Teleport.getComponentType(), new Teleport(newCharacterData.position, new Vector3f()));

            newCharacterData.inventory.applyToPlayer(player, entityStore);
            var newStats = newCharacterData.stats;
            var oldStats = entityStore.getComponent(player, EntityStatMap.getComponentType());

            for (int i = 0; i < newStats.size(); i++) {
                oldStats.setStatValue(i, newStats.get(i).get());
            }

            characterList.forEach(c -> {if (c.getCharacterId() == characterId)
                entityStore.putComponent(player, StatecraftMod.CHARACTER_COMPONENT, new CharacterComponent(c));});
            refreshModel(player, entityStore);
        });
        return true;
    }
    public static List<StatecraftCharacter> getCharacters(Ref<EntityStore> player) {
        return StatecraftMod.api.getCharactersByPlayer(player.getStore().getComponent(player, PlayerRef.getComponentType()).getUuid()).getOrElse(List.of());
    }
    public static List<StatecraftCharacter> getCharacters(UUID player) {
        return StatecraftMod.api.getCharactersByPlayer(player).getOrElse(List.of());
    }

    public static void markCharacterDeceased(Ref<EntityStore> player, ComponentAccessor<EntityStore> store, long characterId) {
        var world = store.getExternalData().getWorld();
        var table = Util.getCharacterTable();
        //TODO handle character doesn't exist
        var characterData = StatecraftMod.api.getCharacterById(characterId).getValue().get().get();

        var playerRef = store.getComponent(player, PlayerRef.getComponentType());
        if (!world.getName().equals(World.DEFAULT)) {
            LOGGER.atWarning().log("Player %s tried to mark a character deceased while not in DEFAULT.", playerRef.getUsername());
            playerRef.sendMessage(Message.raw("Can't perform character actions outside of Orbis!").color(Color.RED));
            return;
        };
        var result = StatecraftMod.api.getCharactersByPlayer(playerRef.getUuid());
        if (result.isFailure())  {
            playerRef.sendMessage(Message.raw("Server API timed out!"));
            LOGGER.atSevere().log("Statecraft API failed to resolve characters for player %s!", playerRef.getUsername());
            return;
        }
        var characters = result.getValue().get();
        StatecraftCharacter suitableReplacementCharacter = null;
        for (var character : characters) {
            if (!character.isDeceased() && character.getCharacterId() != characterId) {
                suitableReplacementCharacter = character;
                break;
            }
        }
        if (suitableReplacementCharacter == null) {
            LOGGER.atInfo().log("Player %s no longer has a character!", playerRef.getUsername());
            StatecraftMod.api.markDeceased(characterId, characterData.getDisplayName());
            store.removeComponent(player, StatecraftMod.CHARACTER_COMPONENT);
            if (StatecraftConfig.ENABLE_NAME_SYSTEM.get())
                store.removeComponent(player, Nameplate.getComponentType());
            return;
        }
        StatecraftMod.api.markDeceased(characterId, characterData.getDisplayName());
        switchCharacters(player, suitableReplacementCharacter.getCharacterId());
    }
    public static void setModel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, long characterId) {
        var world = ref.getStore().getExternalData().getWorld();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || playerRef.isValid()) return;
        LOGGER.atFine().log("Uploading model for player %s and character %d", playerRef.getUsername(), characterId);
        if (!world.getName().equals(World.DEFAULT)) {
            LOGGER.atWarning().log("Player %s tried to set a character model while not in DEFAULT.", playerRef.getUsername());
            playerRef.sendMessage(Message.raw("Can't perform character actions outside of Orbis!").color(Color.RED));
            return;
        };
        var chunkStore = world.getChunkStore().getStore();
        var table = Util.getCharacterTable();
        var characterData = table.get(characterId);
        if (characterData == null) {
            LOGGER.atSevere().log("Tried to set model for character %d, but that character isn't in the table!", characterId);
            return;
        }
        var currentModel = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        var newModel = Util.UUID_TO_SKIN.getOrDefault(playerRef.getUuid(), currentModel.getPlayerSkin()).clone();
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(CosmeticsModule.get().createModel(newModel)));
        store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(newModel));
        table.put(characterId, new StatecraftCharacterTableResource.LocalCharacterData(
                characterData.inventory, characterData.stats, newModel, characterData.position, characterData.icon
        ));
        refreshModel(ref, store);

    }
    public static void refreshModel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        var world = ref.getStore().getExternalData().getWorld();
        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid()) return;
        LOGGER.atFine().log("Refreshing model for player %s", playerRef.getUsername());
        var chunkStore = world.getChunkStore().getStore();
        var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);
        var table = Util.getCharacterTable();
        var oldSkin = Util.UUID_TO_SKIN.get(playerRef.getUuid());
        if (characterComponent == null) {
            LOGGER.atWarning().log("Can't refresh model for player %s because there is no character component!", playerRef.getUsername());
            var cosmetics = CosmeticsModule.get();
            Model newModel = cosmetics.createModel(oldSkin);
            store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
            store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(oldSkin));
            return;
        }
        var characterData = table.get(characterComponent.character.getCharacterId());
        var currentData = table.get(characterComponent.character.getCharacterId());
        var currentModel = store.getComponent(ref, PlayerSkinComponent.getComponentType());
        var alreadyHasModel = currentData.playerSkin.bodyCharacteristic != null && !currentData.playerSkin.bodyCharacteristic.isEmpty();
        if (alreadyHasModel)
            LOGGER.atFine().log("Character already has a model");
        else
            table.put(characterComponent.character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                currentData.inventory, currentData.stats, oldSkin, currentData.position, currentData.icon
            ));
        var cosmetics = CosmeticsModule.get();
        Model newModel = !alreadyHasModel ? cosmetics.createModel(oldSkin) : cosmetics.createModel(characterData.playerSkin);
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
        store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(characterData.playerSkin));
    }

}