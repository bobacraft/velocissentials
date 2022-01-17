package best.boba.velocissentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "velocissentials",
        name = "velocissentials",
        version = "1.0",
        url = "https://github.com/bobacraft/velocissentials",
        authors = {"bbaovanc"},
        description = "Some basic commands for the Velocity proxy")
public class velocissentials {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public velocissentials(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void initalize() {
        PluginManager pluginManager = server.getPluginManager();
    }
}
