package cn.cctstudio.velocity.pickblockfix.backend;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a lightweight cache of backend ping results.
 */
public final class BackendProtocolService {

    private final Logger logger;
    private final ConcurrentHashMap<String, BackendProtocolInfo> cache = new ConcurrentHashMap<>();

    public BackendProtocolService(Logger logger) {
        this.logger = logger;
    }

    public CompletableFuture<BackendProtocolInfo> probeServer(RegisteredServer server) {
        final String name = server.getServerInfo().getName();
        return server.ping().handle((ping, throwable) -> {
            BackendProtocolInfo info;
            if (throwable != null) {
                info = BackendProtocolInfo.unknown(name, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
                logger.debug("Failed to ping backend {}: {}", name, throwable.getMessage());
            } else {
                ServerPing.Version version = ping.getVersion();
                info = new BackendProtocolInfo(
                        name,
                        version.getProtocol(),
                        version.getName(),
                        Instant.now(),
                        true,
                        null
                );
            }
            cache.put(normalize(name), info);
            return info;
        });
    }

    public void probeAll(Collection<RegisteredServer> servers) {
        for (RegisteredServer server : servers) {
            probeServer(server);
        }
    }

    public Optional<BackendProtocolInfo> getCached(String serverName) {
        if (serverName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(normalize(serverName)));
    }

    public BackendProtocolInfo getOrUnknown(String serverName) {
        return getCached(serverName).orElse(BackendProtocolInfo.unknown(serverName == null ? "unknown" : serverName, "not probed yet"));
    }

    private String normalize(String serverName) {
        return serverName.toLowerCase(Locale.ROOT);
    }
}
