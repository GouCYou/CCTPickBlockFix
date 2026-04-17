package cn.cctstudio.velocity.pickblockfix.command;

import cn.cctstudio.velocity.pickblockfix.CCTPickBlockFixPlugin;
import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolInfo;
import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.util.VersionUtil;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Runtime diagnostics command surface.
 */
public final class CctPickCommand implements SimpleCommand {

    private final CCTPickBlockFixPlugin plugin;

    public CctPickCommand(CCTPickBlockFixPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            showStatus(invocation.source());
            return;
        }

        if ("debug".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                send(invocation, "Usage: /cctpick debug <on|off>");
                return;
            }
            boolean enabled = "on".equalsIgnoreCase(args[1]);
            plugin.setDebug(enabled);
            send(invocation, "Debug is now " + (enabled ? "enabled" : "disabled") + ".");
            return;
        }

        if ("dump".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                send(invocation, "Usage: /cctpick dump <player>");
                return;
            }
            Player player = plugin.getProxyServer().getPlayer(args[1]).orElse(null);
            if (player == null) {
                send(invocation, "Player not found: " + args[1]);
                return;
            }
            PlayerState state = plugin.getState(player).orElse(null);
            if (state == null) {
                send(invocation, "No cached state exists for " + player.getUsername());
                return;
            }
            for (String line : plugin.getDebugLogger().dump(state)) {
                send(invocation, line);
            }
            return;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.reloadConfig();
            send(invocation, "Config reloaded.");
            return;
        }

        send(invocation, "Usage: /cctpick status | /cctpick debug <on|off> | /cctpick dump <player> | /cctpick reload");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("status", "debug", "dump", "reload");
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return List.of("on", "off");
        }
        if (args.length == 2 && "dump".equalsIgnoreCase(args[0])) {
            return plugin.getProxyServer().getAllPlayers().stream().map(Player::getUsername).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("cctpick.admin");
    }

    private void showStatus(com.velocitypowered.api.command.CommandSource source) {
        send(source, "Plugin version: " + plugin.getPluginVersion());
        send(source, "PacketEvents initialized: " + (plugin.getPacketEventsManager() != null && plugin.getPacketEventsManager().isInitialized()));
        send(source, "ViaVersion detected: " + plugin.isViaVersionPresent());
        send(source, "ViaBackwards detected: " + plugin.isViaBackwardsPresent());
        send(source, "creative_only: " + plugin.getConfig().creativeOnly());
        send(source, "experimental_survival: " + plugin.getConfig().experimentalSurvival());
        send(source, "only_when_backend_protocol_is_1_21_1: " + plugin.getConfig().onlyWhenBackendProtocolIs1211());

        List<String> affectedPlayers = new ArrayList<>();
        for (Player player : plugin.getProxyServer().getAllPlayers()) {
            PlayerState state = plugin.getState(player).orElse(null);
            if (state == null) {
                continue;
            }
            if (!VersionUtil.isModernPickClient(state.getClientVersion())) {
                continue;
            }
            BackendProtocolInfo backendInfo = state.getBackendProtocolInfo();
            affectedPlayers.add(
                    player.getUsername()
                            + ": client=" + VersionUtil.describeClientVersion(state.getClientVersion())
                            + ", backend=" + state.getBackendServerName()
                            + ", backendProtocol=" + backendInfo.render()
                            + ", gamemode=" + state.getGameMode()
            );
        }

        if (affectedPlayers.isEmpty()) {
            send(source, "Online clients above 1.21.1: none");
        } else {
            send(source, "Online clients above 1.21.1:");
            for (String line : affectedPlayers) {
                send(source, " - " + line);
            }
        }
    }

    private void send(Invocation invocation, String message) {
        send(invocation.source(), message);
    }

    private void send(com.velocitypowered.api.command.CommandSource source, String message) {
        source.sendMessage(Component.text(message));
    }
}
