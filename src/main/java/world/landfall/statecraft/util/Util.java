package world.landfall.statecraft.util;

import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Util {
    public static final HashMap<UUID, PlayerSkin> UUID_TO_SKIN = new HashMap<>();

    public static Map<Long, StatecraftCharacterTableResource.LocalCharacterData> getCharacterTable() {
        var universe = Universe.get();
        var world = universe.getWorld(World.DEFAULT);
        var chunkStore = world.getChunkStore().getStore();
        return chunkStore.getResource(StatecraftMod.STATECRAFT_PLAYER_TABLE_RESOURCE).TABLE;
    }
    public static void runDelayed(float seconds, World world, Runnable runnable) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(runnable), (long)(seconds * 1000), TimeUnit.MILLISECONDS);
    }
}
