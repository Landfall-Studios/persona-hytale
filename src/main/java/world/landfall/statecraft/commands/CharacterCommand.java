package world.landfall.statecraft.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;
import world.landfall.statecraft.util.Util;

import java.awt.*;

public class CharacterCommand extends AbstractPlayerCommand {

    private OptionalArg<Boolean> SET_SKIN;

    public CharacterCommand() {
        super("character", "Opens the character menu");

        SET_SKIN = withOptionalArg("set-skin", "Uploads your current avatar as this character's skin.", ArgTypes.BOOLEAN);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> ref, @NonNull PlayerRef playerRef, @NonNull World world) {
        if (commandContext.provided(SET_SKIN)) {
            world.execute(() -> {
                var characterComponent = store.getComponent(ref, StatecraftMod.CHARACTER_COMPONENT);
                if (characterComponent == null) {
                    commandContext.sendMessage(Message.raw("Not in a character right now!").color(Color.RED));
                    return;
                }
                var character = characterComponent.character;
                var table = world.getChunkStore().getStore().getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE).TABLE;
                var characterData = table.get(character.getCharacterId());
                if (characterData == null) {
                    commandContext.sendMessage(Message.raw("Could not find character data").color(Color.RED));
                    return;
                }

                var currentModel = store.getComponent(ref, PlayerSkinComponent.getComponentType());
                var newModel = Util.UUID_TO_SKIN.getOrDefault(playerRef.getUuid(), currentModel.getPlayerSkin());
                store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(CosmeticsModule.get().createModel(newModel)));
                store.putComponent(ref, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(newModel));
                table.put(character.getCharacterId(), new StatecraftCharacterTableResource.LocalCharacterData(
                        characterData.inventory, characterData.stats, newModel, characterData.position
                ));



            });
        }
    }
}
