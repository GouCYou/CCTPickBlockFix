package cn.cctstudio.velocity.pickblockfix.backend;

import cn.cctstudio.velocity.pickblockfix.util.VersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;

import java.util.Optional;

/**
 * Encodes synthetic client packets for a 1.21.1 backend and writes them out through Velocity's
 * backend connection toward the downstream server.
 *
 * <p>These are logically "serverbound" packets, but on the proxy<->backend Netty channel they
 * must travel as outbound traffic. Feeding them into the inbound side corrupts the backend decode
 * stream and causes ViaVersion to misread subsequent clientbound packets.</p>
 */
public final class BackendInboundPacketInjector {

    private final BackendConnectionLocator connectionLocator;

    public BackendInboundPacketInjector(BackendConnectionLocator connectionLocator) {
        this.connectionLocator = connectionLocator;
    }

    public InjectionResult inject(Player player, PacketWrapper<?> wrapper, int nativePacketId) {
        try {
            Optional<Channel> channelOptional = connectionLocator.findBackendChannel(player);
            if (channelOptional.isEmpty()) {
                return new InjectionResult(false, "backend channel not found");
            }

            Channel channel = channelOptional.get();
            wrapper.setClientVersion(VersionUtil.CLIENT_1_21_1);
            wrapper.setServerVersion(VersionUtil.SERVER_1_21_1);
            wrapper.setNativePacketId(nativePacketId);

            Object buffer;
            synchronized (wrapper.bufferLock) {
                wrapper.prepareForSend(channel, false, false);
                buffer = wrapper.getBuffer();
                wrapper.setBuffer(null);
            }

            PacketEvents.getAPI().getProtocolManager().sendPacket(channel, buffer);
            return new InjectionResult(true, "sent to backend channel " + channel.remoteAddress());
        } catch (Throwable throwable) {
            return new InjectionResult(false, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    public record InjectionResult(boolean success, String detail) {
    }
}
