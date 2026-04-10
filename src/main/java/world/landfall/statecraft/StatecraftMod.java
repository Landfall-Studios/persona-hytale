package world.landfall.statecraft;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import world.landfall.statecraft.api.ApiConnectionManager;
import world.landfall.statecraft.api.StatecraftApi;
import world.landfall.statecraft.api.impl.ApiConnectionManagerImpl;
import world.landfall.statecraft.api.impl.StatecraftApiImpl;
import world.landfall.statecraft.commands.CharacterCommand;
import world.landfall.statecraft.commands.StatecraftCommand;
import world.landfall.statecraft.components.CharacterComponent;
import world.landfall.statecraft.components.StatecraftComponent;
import world.landfall.statecraft.config.StatecraftConfig;
import world.landfall.statecraft.packet.ModPlayerInputHandler;
import world.landfall.statecraft.resources.StatecraftCharacterTableResource;
import world.landfall.statecraft.systems.ModPlayerJoinSystem;
import world.landfall.statecraft.systems.StatecraftComponentAddedSystem;
import world.landfall.statecraft.systems.StatecraftComponentUpdateSystem;

public class StatecraftMod extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String MODID = "statecraft";

    public static StatecraftMod instance;
    public static Config<StatecraftConfig> config;
    public static StatecraftApi api;
    public static ApiConnectionManager connectionManager;

    public static PacketFilter input;

    public static ComponentType<EntityStore, CharacterComponent> CHARACTER_COMPONENT;
    public static ComponentType<EntityStore, StatecraftComponent> STATECRAFT_COMPONENT;

    public static ResourceType<ChunkStore, StatecraftCharacterTableResource> STATECRAFT_PLAYER_TABLE_RESOURCE;

    public StatecraftMod(JavaPluginInit init) {
        super(init);
        instance = this;
        StatecraftConfig.setupConfig();
        config = withConfig(StatecraftConfig.CODEC);
        api = new StatecraftApiImpl();
        connectionManager = new ApiConnectionManagerImpl(api);

    }

    @Override
    protected void shutdown() {
        config.save();
        super.shutdown();

    }

    @Override
    protected void setup() {
        instance = this;

        config.load();

//        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));

        this.getCommandRegistry().registerCommand(new StatecraftCommand());
        this.getCommandRegistry().registerCommand(new CharacterCommand());

        CHARACTER_COMPONENT = this.getEntityStoreRegistry().registerComponent(CharacterComponent.class, "CharacterComponent", CharacterComponent.CODEC);
        STATECRAFT_COMPONENT = this.getEntityStoreRegistry().registerComponent(StatecraftComponent.class, StatecraftComponent::new);
        connectionManager.start(HytaleServer.get());

        STATECRAFT_PLAYER_TABLE_RESOURCE = this.getChunkStoreRegistry().registerResource(StatecraftCharacterTableResource.class, "StatecraftPlayerTable", StatecraftCharacterTableResource.CODEC);

        this.getEntityStoreRegistry().registerSystem(new ModPlayerJoinSystem());
        this.getEntityStoreRegistry().registerSystem(new StatecraftComponentUpdateSystem());
        this.getEntityStoreRegistry().registerSystem(new StatecraftComponentAddedSystem());

        var inputHandler = new ModPlayerInputHandler();
        input = PacketAdapters.registerInbound(inputHandler);
        input = PacketAdapters.registerOutbound(inputHandler);


        LOGGER.atInfo().log("Statecraft mod initialized");


        
    }
}
