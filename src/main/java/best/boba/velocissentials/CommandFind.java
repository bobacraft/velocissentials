package best.boba.velocissentials;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class CommandFind {
    private final Config config;
    public CommandFind(Config config) {
        this.config = config;
    }

    public void register() {
        LiteralCommandNode<CommandSource> rootNode = LiteralArgumentBuilder
                .<CommandSource>literal("find")
                .then(RequiredArgumentBuilder
                        .<CommandSource, String>argument("player", StringArgumentType.string())
                        .suggests(((context, builder) -> {
                            this.config.server().getAllPlayers().forEach(p -> builder.suggest(p.getUsername()));
                            return builder.buildFuture();
                        }))
                        .executes(context -> {
                            CommandSource sender = context.getSource();
                            String username = context.getArgument("player", String.class);
                            Optional<Player> optionalPlayer = this.config.server().getPlayer(username);
                            if (optionalPlayer.isEmpty()) {
                                sender.sendMessage(Component.text(
                                        username + " is not online"
                                ).color(NamedTextColor.RED));
                                return 0;
                            }

                            Player player = optionalPlayer.get();
                            Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
                            if (optionalServerConnection.isEmpty()) {
                                sender.sendMessage(Component.text(
                                        player.getUsername() + " is not on a server"
                                ).color(NamedTextColor.RED));
                                return 0;
                            }

                            sender.sendMessage(Component.text(
                                    String.format("%s is connected to %s",
                                            player.getUsername(),
                                            optionalServerConnection.get().getServerInfo().getName())
                            ));
                            return 0;
                        })
                )
                .requires(sender -> sender.hasPermission("velocissentials.find"))
                .build();


        CommandManager commandManager = this.config.server().getCommandManager();
        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        CommandMeta commandMeta = commandManager.metaBuilder(brigadierCommand).build();
        commandManager.register(commandMeta, brigadierCommand);
    }
}
