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
import world.landfall.statecraft.util.CharacterOperations;

public class CharacterListUI extends InteractiveCustomUIPage<CharacterListUI.Data> {
    public static class Data {
        private String character;
        public static final BuilderCodec<Data> CODEC = BuilderCodec.<Data>builder(Data.class, Data::new)
                .append(new KeyedCodec<String>("Character", BuilderCodec.STRING),
                        (data, value) -> data.character = value,
                        data -> data.character).add()
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
            uiCommandBuilder.appendInline("#Root #Characters", """
                    $C = "Common.ui";
                   
                    Group #Border%s {
                        Background: #FFFFFF;
                        Anchor: (Width: 750, Height: 100, Bottom: 10, Top: 20);
                        Padding: (Full: 1);
                        Group #Character%s {
                            LayoutMode: Left;
                            Padding: (Full: 10);
                            Background: #1a2634;
                        }
                    }""".formatted(c.getCharacterId(), c.getCharacterId()));
            uiCommandBuilder.append("#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId(), "CharacterEntry.ui");
            uiCommandBuilder.set("#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId()+" #Text #NameText.Text", c.getDisplayName());
            uiCommandBuilder.set("#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId()+" #Text #IDText.Text", "ID: "+c.getCharacterId());
            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#Root #Characters #Border"+c.getCharacterId()+" #Character"+c.getCharacterId()+" #ButtonHolder #Button",
                    new EventData().append("Character", c.getCharacterId()+"")
            );
        });
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
            world.execute(() -> {
                CharacterOperations.switchCharacters(ref, characterId);
            });
            player.getPageManager().setPage(ref, store, Page.None);
        } catch (NumberFormatException e) {
            return;
        }
    }
}
