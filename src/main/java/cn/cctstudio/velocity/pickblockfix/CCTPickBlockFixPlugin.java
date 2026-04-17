package cn.cctstudio.velocity.pickblockfix;

import cn.cctstudio.velocity.pickblockfix.backend.BackendConnectionLocator;
import cn.cctstudio.velocity.pickblockfix.backend.BackendInboundPacketInjector;
import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolInfo;
import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolService;
import cn.cctstudio.velocity.pickblockfix.command.CctPickCommand;
import cn.cctstudio.velocity.pickblockfix.config.ConfigManager;
import cn.cctstudio.velocity.pickblockfix.config.PickBlockFixConfig;
import cn.cctstudio.velocity.pickblockfix.debug.DebugLogger;
import cn.cctstudio.velocity.pickblockfix.packet.FallbackExecutor;
import cn.cctstudio.velocity.pickblockfix.packet.PickPacketListener;
import cn.cctstudio.velocity.pickblockfix.packet.PickPacketRecognizer;
import cn.cctstudio.velocity.pickblockfix.packet.PickPacketRewriter;
import cn.cctstudio.velocity.pickblockfix.packetevents.PacketEventsManager;
import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.state.PlayerStateCache;
import cn.cctstudio.velocity.pickblockfix.util.VersionUtil;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Main Velocity entry point.
 */
public final class CCTPickBlockFixPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final PluginContainer pluginContainer;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private PlayerStateCache playerStateCache;
    private BackendProtocolService backendProtocolService;
    private DebugLogger debugLogger;
    private PacketEventsManager packetEventsManager;
    private ScheduledTask backendProbeTask;

    @Inject
    public CCTPickBlockFixPlugin(
            ProxyServer proxyServer,
            Logger logger,
            PluginContainer pluginContainer,
            @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(dataDirectory, logger);
        this.configManager.load();

        this.playerStateCache = new PlayerStateCache();
        this.backendProtocolService = new BackendProtocolService(logger);
        this.debugLogger = new DebugLogger(logger, () -> configManager.current());

        BackendConnectionLocator backendConnectionLocator = new BackendConnectionLocator();
        BackendInboundPacketInjector backendInboundPacketInjector =
                new BackendInboundPacketInjector(backendConnectionLocator);
        PickPacketRecognizer recognizer = new PickPacketRecognizer();
        PickPacketRewriter rewriter = new PickPacketRewriter(
                () -> configManager.current(),
                debugLogger,
                recognizer,
                new FallbackExecutor()
        );
        PickPacketListener packetListener = new PickPacketListener(playerStateCache, rewriter, debugLogger);
        this.packetEventsManager = new PacketEventsManager(proxyServer, pluginContainer, logger, dataDirectory, packetListener);
        this.packetEventsManager.initialize(configManager.current());

        CommandMeta meta = proxyServer.getCommandManager()
                .metaBuilder("cctpick")
                .plugin(this)
                .build();
        proxyServer.getCommandManager().register(meta, new CctPickCommand(this));

        backendProtocolService.probeAll(proxyServer.getAllServers());
        this.backendProbeTask = proxyServer.getScheduler()
                .buildTask(this, () -> backendProtocolService.probeAll(proxyServer.getAllServers()))
                .delay(Duration.ofSeconds(10))
                .repeat(Duration.ofMinutes(5))
                .schedule();

        logger.info("CCTPickBlockFix initialized. PacketEvents ready={}", packetEventsManager.isInitialized());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (playerStateCache == null || backendProtocolService == null) {
            return;
        }

        Player player = event.getPlayer();
        PlayerState state = playerStateCache.getOrCreate(player);
        state.setPlayerName(player.getUsername());
        state.setClientVersion(VersionUtil.toClientVersion(player.getProtocolVersion()));

        String backendName = event.getServer().getServerInfo().getName();
        BackendProtocolInfo currentInfo = backendProtocolService.getOrUnknown(backendName);
        state.resetForServerSwitch(backendName, currentInfo);
        backendProtocolService.probeServer(event.getServer()).thenAccept(state::setBackendProtocolInfo);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (playerStateCache != null) {
            playerStateCache.remove(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (backendProbeTask != null) {
            backendProbeTask.cancel();
        }
        if (packetEventsManager != null) {
            packetEventsManager.shutdown();
        }
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public PlayerStateCache getPlayerStateCache() {
        return playerStateCache;
    }

    public BackendProtocolService getBackendProtocolService() {
        return backendProtocolService;
    }

    public PacketEventsManager getPacketEventsManager() {
        return packetEventsManager;
    }

    public PickBlockFixConfig getConfig() {
        return configManager.current();
    }

    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    public String getPluginVersion() {
        return pluginContainer.getDescription().getVersion().map(Object::toString).orElse("unknown");
    }

    public PickBlockFixConfig setDebug(boolean debug) {
        PickBlockFixConfig config = configManager.setDebug(debug);
        if (packetEventsManager != null) {
            packetEventsManager.applyRuntimeSettings(config);
        }
        return config;
    }

    public PickBlockFixConfig reloadConfig() {
        PickBlockFixConfig config = configManager.load();
        if (packetEventsManager != null) {
            packetEventsManager.applyRuntimeSettings(config);
        }
        return config;
    }

    public boolean isViaVersionPresent() {
        return proxyServer.getPluginManager().getPlugin("viaversion").isPresent();
    }

    public boolean isViaBackwardsPresent() {
        return proxyServer.getPluginManager().getPlugin("viabackwards").isPresent();
    }

    public Optional<PlayerState> getState(Player player) {
        return playerStateCache == null
                ? Optional.empty()
                : Optional.ofNullable(playerStateCache.get(player.getUniqueId()));
    }
}
