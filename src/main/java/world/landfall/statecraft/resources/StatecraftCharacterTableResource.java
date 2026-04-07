package world.landfall.statecraft.resources;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;
import world.landfall.statecraft.util.MapCodec;
import world.landfall.statecraft.util.UtilCodecs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatecraftCharacterTableResource implements Resource<ChunkStore> {

    public static class LocalCharacterData {
        public ItemContainer inventory;
        public EntityStatMap stats;
        public PlayerSkin playerSkin;
        public Vector3d position;
        public static final BuilderCodec<LocalCharacterData> CODEC = BuilderCodec.builder(
                LocalCharacterData.class, LocalCharacterData::create
        )
                .append(new KeyedCodec<ItemContainer>("Inventory", ItemContainer.CODEC),
                        (data, value) -> data.inventory = value,
                        data -> data.inventory).add()
                .append(new KeyedCodec<EntityStatMap>("Stats", EntityStatMap.CODEC),
                        (data, value) -> data.stats = value,
                        data -> data.stats).add()
                .append(new KeyedCodec<PlayerSkin>("PlayerSkin", UtilCodecs.PLAYER_SKIN_CODEC),
                        (data, value) -> data.playerSkin = value,
                        data -> data.playerSkin).add()
                .append(new KeyedCodec<Vector3d>("Transform", Vector3d.CODEC),
                        (data, value) -> data.position = value,
                        data -> data.position).add()
                .build();
        public LocalCharacterData(ItemContainer inventory, EntityStatMap stats, PlayerSkin playerSkin, Vector3d position) {
            this.inventory = inventory;
            this.stats = stats;
            this.playerSkin = playerSkin;
            this.position = position;
        }
        public static LocalCharacterData create() {
            return new LocalCharacterData(new SimpleItemContainer((short)0), new EntityStatMap(), new PlayerSkin(), Vector3d.ZERO);
        }
    }

    public Map<Long, LocalCharacterData> TABLE;

    public static final BuilderCodec<StatecraftCharacterTableResource> CODEC = BuilderCodec.builder(StatecraftCharacterTableResource.class, StatecraftCharacterTableResource::new)
            .append(new KeyedCodec<>("Table", new MapCodec<Long, LocalCharacterData>(
                    BuilderCodec.LONG,
                    LocalCharacterData.CODEC
            )), (data, value) -> data.TABLE = value, data -> data.TABLE).add()
            .build();

    public StatecraftCharacterTableResource() {
        TABLE = new HashMap<>();
    }
    private StatecraftCharacterTableResource(Map<Long, LocalCharacterData> TABLE) {
        this.TABLE = new HashMap<>(TABLE);
    }

    @Override
    public @Nullable Resource<ChunkStore> clone() {
        return new StatecraftCharacterTableResource(this.TABLE);
    }
}
