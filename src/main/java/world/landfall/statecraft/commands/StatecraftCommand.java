package world.landfall.statecraft.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import world.landfall.statecraft.StatecraftMod;
import world.landfall.statecraft.util.CharacterOperations;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;


public class StatecraftCommand extends AbstractPlayerCommand {

    private enum Commands {
        password,
        list
    }
    private final RequiredArg<Commands> commandArg;
    private final OptionalArg<String> passwordArg;

    public StatecraftCommand() {
        super("statecraft", "Command for interacting with Statecraft.");
        this.setPermissionGroup(GameMode.Creative);
        commandArg = this.withRequiredArg("command", "Which command to run", ArgTypes.forEnum("commands", Commands.class));
        passwordArg = this.withOptionalArg("password", "The password to be set", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNull CommandContext commandContext, @NonNull Store<EntityStore> store, @NonNull Ref<EntityStore> player, @NonNull PlayerRef playerRef, @NonNull World world) {

        if (!commandContext.provided(commandArg))
            help(commandContext);
        switch(commandContext.get(commandArg)) {
            case password -> {
                if (!commandContext.provided(passwordArg))
                    passwordHelp(commandContext);
                setPassword(commandContext);
            }
            case list -> {
                if (!player.isValid()) {
                    commandContext.sendMessage(Message.raw("Cannot list characters; not running command as a player!"));
                    return;
                }
                var characterList = CharacterOperations.getCharacters(player);
                if (characterList.isEmpty()) {
                    commandContext.sendMessage(Message.raw("You haven't made a character yet!").color(Color.RED));
                }
                commandContext.sendMessage(Message.raw("Your characters:"));
                characterList.forEach(c -> {
                    commandContext.sendMessage(Message.raw(c.getDisplayName()+":"));
                    commandContext.sendMessage(Message.raw("  ID: "+c.getCharacterId()));
                    commandContext.sendMessage(Message.raw("  Created: "+c.getFirstUsed()));
                    commandContext.sendMessage(Message.raw("  Last Seen: "+c.getLastSeen()));
                    commandContext.sendMessage(Message.empty());
                });

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
//        StatecraftMod.api.setPassword()
        var player = context.senderAsPlayerRef();
        if (player == null || !player.isValid()) return 1;
        var playerRef = player.getStore().getComponent(player, PlayerRef.getComponentType());
        var password = context.get(passwordArg);
        if (password == null) return 1;
        if (password.length() < 6) {
            context.sendMessage(Message.raw("Password must be at least 6 characters").color(Color.RED));
            return 1;
        }

        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        var salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            hash = Base64.getEncoder().encodeToString(hashedPassword);
        } catch (java.security.NoSuchAlgorithmException e) {
            context.sendMessage(Message.raw("Internal server error: could not make password hash").color(Color.RED));
            return 1;
        }
        var result = StatecraftMod.api.setPassword(playerRef.getUuid(), hash, salt);
        if (result.isSuccess()) {
            context.sendMessage(Message.raw("Password set successfully. You can now log in at the Statecraft web panel."));
            return 0;
        } else {
            context.sendMessage(Message.raw("Could not set the password on the server."));
        }
        return 0;
    }
}