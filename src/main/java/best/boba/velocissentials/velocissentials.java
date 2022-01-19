package best.boba.velocissentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "velocissentials",
        name = "velocissentials",
        version = "1.0.1",
        url = "https://github.com/bobacraft/velocissentials",
        authors = {"bbaovanc"},
        description = "Some basic commands for the Velocity proxy")
public class velocissentials {
    private final ProxyServer server;
    private final Logger logger;
    private final Config config;

    @Inject
    public velocissentials(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.config = new Config(this.server, this.logger);
    }

    public void initalize() {
        new CommandFind(config).register();
        new CommandPing(config).register();
        new CommandSend(config).register();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        initalize();
    }
}
