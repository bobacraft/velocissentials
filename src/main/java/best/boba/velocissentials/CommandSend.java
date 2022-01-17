package best.boba.velocissentials;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandSend {
    private final Config config;
    public CommandSend(Config config) {
        this.config = config;
    }

    public void register() {
        LiteralCommandNode<CommandSource> rootNode = LiteralArgumentBuilder
                .<CommandSource>literal("send")
                .requires(sender -> sender.hasPermission("velocissentials.send"))
                .build();

        LiteralCommandNode<CommandSource> serverNode = LiteralArgumentBuilder
                .<CommandSource>literal("server")
                .build();

        LiteralCommandNode<CommandSource> playerNode = LiteralArgumentBuilder
                .<CommandSource>literal("player")
                .then(RequiredArgumentBuilder
                        .<CommandSource, String>argument("player", StringArgumentType.string())
                        .suggests(((context, builder) -> {
                            this.config.server().getAllPlayers().forEach(p -> builder.suggest(p.getUsername()));
                            return builder.buildFuture();
                        }))
                        .then(RequiredArgumentBuilder
                                .<CommandSource, String>argument("to", StringArgumentType.string())
                                .suggests(((context, builder) -> suggestAllServers(builder)))
                                .executes(context -> {
                                    CommandSource sender = context.getSource();
                                    String playerName = context.getArgument("player", String.class);
                                    Optional<Player> optionalPlayer = this.config.server().getPlayer(playerName);
                                    if (optionalPlayer.isEmpty()) {
                                        sender.sendMessage(Component.text(
                                                "Player named " + playerName + " could not be found"
                                        ).color(NamedTextColor.RED));
                                        return 0;
                                    }
                                    Player player = optionalPlayer.get();

                                    String serverName = context.getArgument("to", String.class);
                                    Optional<RegisteredServer> optionalTargetServer = this.config.server().getServer(serverName);
                                    if (optionalTargetServer.isEmpty()) {
                                        sender.sendMessage(Component.text(
                                                "Server named " + serverName + " could not be found"
                                        ).color(NamedTextColor.RED));
                                        return 0;
                                    }

                                    List<Player> playerList = new ArrayList<>();
                                    playerList.add(player);
                                    sendTo(sender, playerList, optionalTargetServer.get());
                                    return 1;
                                })
                        )
                )
                .build();

        LiteralCommandNode<CommandSource> allCommand = LiteralArgumentBuilder
                .<CommandSource>literal("all")
                .then(RequiredArgumentBuilder
                        .<CommandSource, String>argument("to", StringArgumentType.string())
                        .suggests(((context, builder) -> suggestAllServers(builder)))
                        .executes(context -> {
                            CommandSource sender = context.getSource();
                            String toString = context.getArgument("to", String.class);
                            Optional<RegisteredServer> optionalToServer = this.config.server().getServer(toString);

                            if (optionalToServer.isEmpty()) {
                                sender.sendMessage(Component.text(
                                        "Server named " + toString + " does not exist"
                                ).color(NamedTextColor.RED));
                                return 0;
                            }

                            RegisteredServer toServer = optionalToServer.get();
                            sendTo(sender, this.config.server().getAllPlayers(), toServer);
                            return 1;
                        }
                )).build();

        ArgumentCommandNode<CommandSource, String> fromArg = RequiredArgumentBuilder
                .<CommandSource, String>argument("from", StringArgumentType.string())
                .suggests(((context, builder) -> suggestAllServers(builder)))
                .build();

        ArgumentCommandNode<CommandSource, String> toArg = RequiredArgumentBuilder
                .<CommandSource, String>argument("to", StringArgumentType.string())
                .suggests(((context, builder) -> suggestAllServers(builder)))
                .executes(context -> {
                    CommandSource sender = context.getSource();
                    String fromString = context.getArgument("from", String.class);
                    String toString = context.getArgument("to", String.class);
                    Optional<RegisteredServer> optionalFromServer = this.config.server().getServer(fromString);
                    Optional<RegisteredServer> optionalToServer = this.config.server().getServer(toString);

                    if (optionalFromServer.isEmpty()) {
                        sender.sendMessage(Component.text(
                                "Server named " + fromString + " does not exist"
                        ).color(NamedTextColor.RED));
                        return 0;
                    }
                    if (optionalToServer.isEmpty()) {
                        sender.sendMessage(Component.text(
                                "Server named " + toString + " does not exist"
                        ).color(NamedTextColor.RED));
                        return 0;
                    }

                    RegisteredServer fromServer = optionalFromServer.get();
                    RegisteredServer toServer = optionalToServer.get();
                    sendTo(sender, fromServer.getPlayersConnected(), toServer);
                    return 1;
                }).build();

        rootNode.addChild(serverNode);
        rootNode.addChild(playerNode);
        serverNode.addChild(allCommand);
        serverNode.addChild(fromArg);
        fromArg.addChild(toArg);

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        CommandMeta commandMeta = this.config.server().getCommandManager().metaBuilder(brigadierCommand).build();
        this.config.server().getCommandManager().register(commandMeta, brigadierCommand);
    }

    public static void sendTo(CommandSource sender, Collection<Player> fromPlayers, RegisteredServer toServer) {
        int totalCount = fromPlayers.size();
        List<CompletableFuture<ConnectionRequestBuilder.Result>> results = new ArrayList<>();
        for (Player player : fromPlayers) {
            ConnectionRequestBuilder request = player.createConnectionRequest(toServer);
            results.add(request.connect());
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger alreadyConnected = new AtomicInteger();
        AtomicInteger kicked = new AtomicInteger();
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger internalErrors = new AtomicInteger();
        int connectFailed = 0;
        results.stream().parallel().forEach(f -> {
            try {
                switch (f.get().getStatus()) {
                    case SUCCESS -> success.getAndIncrement();
                    case ALREADY_CONNECTED -> alreadyConnected.getAndIncrement();
                    case SERVER_DISCONNECTED -> kicked.getAndIncrement();
                    case CONNECTION_CANCELLED -> cancelled.getAndIncrement();
                    case CONNECTION_IN_PROGRESS -> sender.sendMessage(Component.text(
                            "This should not happen, but a player's connection is in progress!"
                    ).color(NamedTextColor.DARK_RED));
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                internalErrors.getAndIncrement();
            }
        });

        sender.sendMessage(Component.text(String.format("Sent %d players to %s",
                totalCount,
                toServer.getServerInfo().getName())));
        sender.sendMessage(Component.text("  Success: " + success).color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Already connected: " + alreadyConnected).color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Kicked by server: " + kicked).color(NamedTextColor.RED));
        sender.sendMessage(Component.text("  Cancelled by a plugin: " + success).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Internal server errors: " + internalErrors).color(NamedTextColor.DARK_RED));
    }

    private CompletableFuture<Suggestions> suggestAllServers(SuggestionsBuilder builder) {
        for (RegisteredServer s : this.config.server().getAllServers()) {
            builder.suggest(s.getServerInfo().getName());
        }
        return builder.buildFuture();
    }
}
