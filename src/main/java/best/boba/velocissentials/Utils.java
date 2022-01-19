package best.boba.velocissentials;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

public class Utils {
    public static SuggestionProvider<CommandSource> suggestOnlinePlayers(ProxyServer server) {
        return (context, builder) -> {
            String query = builder.getRemainingLowerCase();
            server.getAllPlayers().forEach(player -> {
                String username = player.getUsername();
                if (username.toLowerCase().startsWith(query)) {
                    builder.suggest(username, () -> "player");
                }
            });
            return builder.buildFuture();
        };
    }
}
