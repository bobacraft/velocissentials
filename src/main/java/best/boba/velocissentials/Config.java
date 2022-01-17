package best.boba.velocissentials;

import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;


public record Config(ProxyServer server, Logger logger) {
}
