package cn.cctstudio.velocity.pickblockfix.packetevents;

import cn.cctstudio.velocity.pickblockfix.config.PickBlockFixConfig;
import cn.cctstudio.velocity.pickblockfix.packet.PickPacketListener;
import com.github.retrooper.packetevents.PacketEvents;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Wraps PacketEvents bootstrap so the rest of the plugin can treat it as an optional subsystem.
 */
public final class PacketEventsManager {

    private final ProxyServer proxyServer;
    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Path dataDirectory;
    private final PickPacketListener packetListener;

    private volatile boolean initialized;

    public PacketEventsManager(
            ProxyServer proxyServer,
            PluginContainer pluginContainer,
            Logger logger,
            Path dataDirectory,
            PickPacketListener packetListener
    ) {
        this.proxyServer = proxyServer;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.packetListener = packetListener;
    }

    public void initialize(PickBlockFixConfig config) {
        try {
            if (PacketEvents.getAPI() == null) {
                PacketEvents.setAPI(VelocityPacketEventsBuilder.build(proxyServer, pluginContainer, logger, dataDirectory));
            }

            PacketEvents.getAPI().getSettings()
                    .checkForUpdates(false)
                    .debug(config.debug())
                    .kickOnPacketException(false);

            PacketEvents.getAPI().load();
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
            PacketEvents.getAPI().init();
            initialized = PacketEvents.getAPI().isInitialized();
        } catch (Throwable throwable) {
            initialized = false;
            logger.error("Failed to initialize PacketEvents. Diagnostics will remain available, but packet fixes will be disabled.", throwable);
        }
    }

    public void applyRuntimeSettings(PickBlockFixConfig config) {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getSettings().debug(config.debug());
        }
    }

    public void shutdown() {
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized()) {
                PacketEvents.getAPI().terminate();
            }
        } catch (Throwable throwable) {
            logger.warn("Failed to shut down PacketEvents cleanly.", throwable);
        } finally {
            initialized = false;
            VelocityPacketEventsBuilder.clearBuildCache();
        }
    }

    public boolean isInitialized() {
        return initialized && PacketEvents.getAPI() != null && PacketEvents.getAPI().isInitialized();
    }
}
