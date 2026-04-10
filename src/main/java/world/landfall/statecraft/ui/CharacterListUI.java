package world.landfall.statecraft.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.util.CharacterOperations;
import world.landfall.statecraft.util.Util;

public class CharacterListUI extends InteractiveCustomUIPage<CharacterListUI.Data> {
    public static class Data {
        enum ButtonAction {
            EQUIP, MARK_DECEASED, CREATE, COLLECT, UPLOAD_SKIN
        }
        private String character;
        private ButtonAction action;
        public static final BuilderCodec<Data> CODEC = BuilderCodec.<Data>builder(Data.class, Data::new)
                .append(new KeyedCodec<String>("Character", BuilderCodec.STRING),
                        (data, value) -> data.character = value,
                        data -> data.character).add()
                .append(new KeyedCodec<String>("Action", BuilderCodec.STRING),
                        (data, value) -> data.action = ButtonAction.valueOf(value),
                        data -> data.action.name()).add()
                .build();
    }
    public CharacterListUI(@NonNull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@NonNull Ref<EntityStore> ref, @NonNull UICommandBuilder uiCommandBuilder, @NonNull UIEventBuilder evt, @NonNull Store<EntityStore> store) {
        uiCommandBuilder.append("CharacterList.ui");
        var characterList = CharacterOperations.getCharacters(ref);
        characterList.forEach(c -> {
            if (c.isDeceased()) {
                var table = Util.getCharacterTable();
                if (!table.containsKey(c.getCharacterId())) return;
            }
            uiCommandBuilder.appendInline("#Root #Characters", """
                    $C = "Common.ui";
                   
                    Group #Border%s {
                        Background: #FFFFFF;
                        Anchor: (Width: 750, Height: 125, Bottom: 10, Top: 20);
                        Padding: (Full: 1);
                        Group #Character%s {
                            LayoutMode: Left;
                            Padding: (Full: 10);
                            Background: #1a2634;
                        }
                    }""".formatted(c.getCharacterId(), c.getCharacterId()));
            if (c.isDeceased()) {
                uiCommandBuilder.append("#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId(), "CharacterEntryDeceased.ui");
                evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId() + " #ButtonHolder #Button",
                        new EventData().append("Character", c.getCharacterId() + "").append("Action", "COLLECT")
                );
            } else {
                uiCommandBuilder.append("#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId(), "CharacterEntry.ui");
                evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId() + " #ButtonHolder #Button",
                        new EventData().append("Character", c.getCharacterId() + "").append("Action", "EQUIP")
                );
                evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId() + " #ButtonHolder #KillButton",
                        new EventData().append("Character", c.getCharacterId() + "").append("Action", "MARK_DECEASED")
                );
                evt.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#Root #Characters #Border" + c.getCharacterId() + " #Character" + c.getCharacterId() + " #UploadButtonWrapper #UploadButton",
                        new EventData().append("Character", c.getCharacterId() + "").append("Action", "UPLOAD_SKIN")
                );
            }
            uiCommandBuilder.set("#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId()+" #Text #NameText.Text", c.getDisplayName());
            uiCommandBuilder.set("#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId()+" #Text #IDText.Text", "ID: "+c.getCharacterId());

        });
        uiCommandBuilder.append("#Root #Characters", "CharacterCreateButton.ui");
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#Root #Characters #CreateButtonGroup #CreateButtonWrapper #CreateButton",
                new EventData().append("Character", "-1").append("Action", "CREATE")
        );
    }

    @Override
    public void handleDataEvent(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store, CharacterListUI.@NonNull Data data) {
        super.handleDataEvent(ref, store, data);
        var world = store.getExternalData().getWorld();
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null || !ref.isValid())
            return;

        try {
            var characterId = Long.parseLong(data.character);
            var action = data.action;
            switch (action) {
                case EQUIP -> world.execute(() -> {
                    CharacterOperations.switchCharacters(ref, characterId);
                    player.getPageManager().setPage(ref, store, Page.None);
                });
                case MARK_DECEASED -> world.execute(() -> {
                    //TODO handle this button
                    CharacterOperations.markCharacterDeceased(ref, store, characterId);
                    player.getPageManager().setPage(ref, store, Page.None);
                });
                case CREATE -> world.execute(() -> player.getPageManager().openCustomPage(ref, store, new CharacterCreateMenuUI(playerRef)));
                case COLLECT -> world.execute(() -> {
                    var table = Util.getCharacterTable();
                    var character = table.get(characterId);
                    character.inventory.STORAGE.forEach((i, stack) -> player.giveItem(stack, ref, store));
                    character.inventory.UTILITY.forEach((i, stack) -> player.giveItem(stack, ref, store));
                    character.inventory.HOTBAR.forEach((i, stack) -> player.giveItem(stack, ref, store));
                    character.inventory.BACKPACK.forEach((i, stack) -> player.giveItem(stack, ref, store));
                    character.inventory.ARMOR.forEach((i, stack) -> player.giveItem(stack, ref, store));
                    table.remove(characterId);
                    player.getPageManager().setPage(ref, store, Page.None);
                });
                case UPLOAD_SKIN -> world.execute(() -> {
                    CharacterOperations.setModel(ref, store, characterId);
                    player.getPageManager().setPage(ref, store, Page.None);

                });
            }
        } catch (NumberFormatException e) {
            return;
        }
    }
}
