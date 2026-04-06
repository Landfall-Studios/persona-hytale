package world.landfall.statecraft;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;


public class StatecraftCommand extends AbstractWorldCommand {
    private enum Commands {
        password,
        checkDisplayNames,
        updateDisplayNames
    }
    private final OptionalArg<Commands> commandArg;
    private final OptionalArg<String> passwordArg;
    private World world;

    public StatecraftCommand() {
        super("statecraft", "Command for interacting with Statecraft.");
        this.setPermissionGroup(GameMode.Creative);
        commandArg = this.withOptionalArg("command", "Which command to run", ArgTypes.forEnum("commands", Commands.class));
        passwordArg = this.withOptionalArg("password", "The password to be set", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull World world, @NonNull Store<EntityStore> store) {
        if (!commandContext.provided(commandArg))
            help(commandContext);
        this.world = world;
        switch(commandContext.get(commandArg)) {
            case password -> {
                if (!commandContext.provided(passwordArg))
                    passwordHelp(commandContext);
                setPassword(commandContext);
            }
            case checkDisplayNames -> {

            }
            case updateDisplayNames -> {

            }
        }
    }
    private static int help(CommandContext context) {
        context.sendMessage(Message.raw("§6=== Statecraft Commands ==="));
        context.sendMessage(Message.raw("§e/statecraft password <password>§f - Set your UUID password for web panel access"));
        context.sendMessage(Message.raw("§e/statecraft checkdisplaynames§f - Check your tracked display names"));
        context.sendMessage(Message.raw("§e/statecraft updatedisplaynames§f - Update your display name tracking"));
        context.sendMessage(Message.raw("§7Your UUID password allows you to log into the web panel and select from your characters."));
        return 1;
    }

    private static int passwordHelp(CommandContext context) {
        context.sendMessage(Message.raw("§6=== Password Command ==="));
        context.sendMessage(Message.raw("§e/statecraft password <password>§f - Set your UUID password"));
        context.sendMessage(Message.raw("§7This password is tied to your UUID and allows web panel access."));
        context.sendMessage(Message.raw("§7You can then select from any of your characters in the web panel."));
        context.sendMessage(Message.raw("§7Password must be at least 6 characters long."));
        return 1;
    }
    private int setPassword(CommandContext context) {
        return 1;
    }
}