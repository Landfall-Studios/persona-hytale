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
import world.landfall.statecraft.components.CharacterComponent;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;
import world.landfall.statecraft.util.CharacterOperations;

public class CharacterCreateMenuUI extends InteractiveCustomUIPage<CharacterCreateMenuUI.Data> {
    private StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon currentIcon = StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon.ANGEL;
    public static class Data {


        public enum ButtonAction {
            CREATE, SWAP_ICON
        }

        public String name;
        public ButtonAction buttonAction;

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .append(new KeyedCodec<>("@Name", BuilderCodec.STRING),
                        (data, value) -> data.name = value,
                        data -> data.name).add()
                .append(new KeyedCodec<>("ButtonAction", BuilderCodec.STRING),
                        (data, value) -> data.buttonAction = ButtonAction.valueOf(value),
                        data -> data.buttonAction.name()).add()
                .build();

        public Data() {
            name = "";
        }
    }
    public CharacterCreateMenuUI(@NonNull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@NonNull Ref<EntityStore> ref, @NonNull UICommandBuilder builder, @NonNull UIEventBuilder evt, @NonNull Store<EntityStore> store) {
        builder.append("CharacterCreateMenu.ui");
        evt.addEventBinding(
                CustomUIEventBindingType.Activating, "#Root #Panel #Right #Bottom #ButtonWrapper #Button",
                new EventData()
                        .append("@Name", "#Root #Panel #Right #Top #Input.Value")
                        .append("ButtonAction", "CREATE")
        );
        evt.addEventBinding(
                CustomUIEventBindingType.Activating, "#Root #Panel #Left #SwapIconButtonWrapper #SwapIconButton",
                new EventData()
                        .append("@Name", "#Root #Panel #Right #Top #Input.Value")
                        .append("ButtonAction", "SWAP_ICON")
        );
        refreshIcon();

    }

    @Override
    public void handleDataEvent(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store, CharacterCreateMenuUI.@NonNull Data data) {
        var player = store.getComponent(ref, Player.getComponentType());
        var world = store.getExternalData().getWorld();
        if (player == null) return;

        var name = data.name;
        var action = data.buttonAction;
        switch (action) {
            case CREATE -> {
                world.execute(() -> CharacterOperations.createCharacter(ref, name, this.currentIcon));
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case SWAP_ICON -> {
                var ordinal = this.currentIcon.ordinal();
                this.currentIcon = StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon.values()[(ordinal+1)% StatecraftCharacterTableResource.LocalCharacterData.CharacterIcon.values().length];
                refreshIcon();
            }
        }


    }
    private void refreshIcon() {
        var command = new UICommandBuilder();
        command.set("#Root #Panel #Left #Icon.Background", "#"+(this.currentIcon.ordinal()+"").repeat(6));
        sendUpdate(command);
    }

}
