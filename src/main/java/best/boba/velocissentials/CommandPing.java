package best.boba.velocissentials;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class CommandPing {
    private final Config config;
    public CommandPing(Config config) {
        this.config = config;
    }
    public void register() {
        LiteralCommandNode<CommandSource> rootNode = LiteralArgumentBuilder
                .<CommandSource>literal("ping")
                .requires(sender -> sender.hasPermission("velocissentials.ping"))
                .executes(context -> {
                    CommandSource sender = context.getSource();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text(
                                "Provide a player to ping"
                        ).color(NamedTextColor.RED));
                        return 0;
                    }

                    sender.sendMessage(Component.text(
                            String.format("Your ping is %dms", player.getPing())
                    ));
                    return 1;
                })
                .build();

        ArgumentCommandNode<CommandSource, String> pingOther = RequiredArgumentBuilder
                .<CommandSource, String>argument("player", StringArgumentType.string())
                .requires(sender -> sender.hasPermission("velocissentials.ping.others"))
                .suggests(Utils.suggestOnlinePlayers(this.config.server()))
                .executes(context -> {
                    CommandSource sender = context.getSource();
                    String username = context.getArgument("player", String.class);
                    Optional<Player> optionalPlayer = this.config.server().getPlayer(username);
                    if (optionalPlayer.isEmpty()) {
                        sender.sendMessage(Component.text(
                                String.format("Player %s is not online", username)
                        ).color(NamedTextColor.RED));
                        return 0;
                    }

                    Player player = optionalPlayer.get();
                    sender.sendMessage(Component.text(
                            String.format("%s's ping is %dms", player.getUsername(), player.getPing())
                    ));
                    return 1;
                })
                .build();

        rootNode.addChild(pingOther);

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        CommandMeta commandMeta = this.config.server().getCommandManager().metaBuilder(brigadierCommand).build();
        this.config.server().getCommandManager().register(commandMeta, brigadierCommand);
    }
}
