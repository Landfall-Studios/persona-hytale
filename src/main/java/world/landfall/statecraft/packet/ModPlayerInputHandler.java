package world.landfall.statecraft.packet;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.MovementStatesUpdate;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import world.landfall.statecraft.ui.CharacterListUI;

import java.util.UUID;

public class ModPlayerInputHandler implements PlayerPacketWatcher {

    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof MovementStatesUpdate movementStatesPacket)) return;
        System.out.println("Got here");
        var ref = playerRef.getReference();
        var store = ref.getStore();
        var world = store.getExternalData().getWorld();
        world.execute(() -> {
            var player = store.getComponent(ref, Player.getComponentType());

                if (player.getPageManager().getCustomPage() == null && movementStatesPacket.movementStates.walking)
                    player.getPageManager().openCustomPage(ref, store, new CharacterListUI(playerRef));
        });

    }
}
