package cn.cctstudio.velocity.pickblockfix.backend;

import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.util.VersionUtil;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItem;
import com.velocitypowered.api.proxy.Player;

/**
 * Experimental direct rewrite path: send a raw legacy 1.21.1 PICK_ITEM packet into the backend
 * connection after cancelling the modern frontend packet.
 */
public final class BackendLegacyPickSender {

    private final BackendInboundPacketInjector packetInjector;

    public BackendLegacyPickSender(BackendInboundPacketInjector packetInjector) {
        this.packetInjector = packetInjector;
    }

    public DirectRewriteResult trySendLegacyPick(Player player, PlayerState state, int inventorySlot) {
        try {
            WrapperPlayClientPickItem wrapper = new WrapperPlayClientPickItem(inventorySlot);
            BackendInboundPacketInjector.InjectionResult injectionResult = packetInjector.inject(
                    player,
                    wrapper,
                    PacketType.Play.Client.PICK_ITEM.getId(VersionUtil.CLIENT_1_21_1)
            );
            if (!injectionResult.success()) {
                return new DirectRewriteResult(false, injectionResult.detail());
            }
            return new DirectRewriteResult(
                    true,
                    "legacy PICK_ITEM inventorySlot=" + inventorySlot
                            + ", " + injectionResult.detail()
            );
        } catch (Throwable throwable) {
            return new DirectRewriteResult(false, throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    public record DirectRewriteResult(boolean success, String detail) {
    }
}
