package cn.cctstudio.velocity.pickblockfix.packet;

import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.util.InventorySlotUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.velocitypowered.api.proxy.Player;

import java.util.Collections;
import java.util.Optional;

/**
 * Best-effort proxy-side emulation when direct legacy rewrite is not available.
 */
public final class FallbackExecutor {

    public FallbackExecutor() {
    }

    public FallbackResult tryFallback(Player player, PlayerState state, PickResolution resolution) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return new FallbackResult(false, "PacketEvents user is not available");
        }

        Integer resolvedSlot = resolution.inventorySlot();
        if (resolvedSlot != null && InventorySlotUtil.isHotbarSlot(resolvedSlot)) {
            if (resolvedSlot == state.getHeldHotbarSlot()) {
                return new FallbackResult(true, "target item is already selected in hotbar slot " + resolvedSlot);
            }

            user.receivePacketSilently(new WrapperPlayClientHeldItemChange(resolvedSlot));
            user.sendPacket(new WrapperPlayServerHeldItemChange(resolvedSlot));
            state.setHeldHotbarSlot(resolvedSlot);
            return new FallbackResult(true, "selected existing hotbar slot " + resolvedSlot + " through frontend proxy emulation");
        }

        if (resolvedSlot != null && !InventorySlotUtil.isHotbarSlot(resolvedSlot)) {
            int sourceInventorySlot = resolvedSlot;
            int sourceContainerSlot = InventorySlotUtil.inventoryIndexToContainerSlot(sourceInventorySlot);
            int targetHotbarSlot = state.getHeldHotbarSlot();

            user.receivePacketSilently(new WrapperPlayClientClickWindow(
                    0,
                    Optional.of(state.getPlayerInventoryStateId()),
                    sourceContainerSlot,
                    targetHotbarSlot,
                    Optional.empty(),
                    WrapperPlayClientClickWindow.WindowClickType.SWAP,
                    Optional.of(Collections.emptyMap()),
                    ItemStack.EMPTY
            ));

            state.getInventorySnapshot().swapSlots(sourceInventorySlot, targetHotbarSlot);
            return new FallbackResult(
                    true,
                    "swapped main inventory slot " + sourceInventorySlot
                            + " into hotbar slot " + targetHotbarSlot
                            + " through frontend proxy emulation"
            );
        }

        if (!state.isCreative()) {
            return new FallbackResult(false, "non-creative fallback requires an existing inventory item");
        }

        ItemStack stack = resolution.preferredStack();
        if (stack == null || stack.isEmpty()) {
            stack = ItemStack.builder()
                    .version(state.getClientVersion())
                    .type(resolution.itemType())
                    .build();
        } else {
            stack = stack.copy();
        }
        stack.setAmount(Math.max(1, stack.getAmount()));

        int targetHotbarSlot = state.getHeldHotbarSlot();
        int containerSlot = InventorySlotUtil.hotbarIndexToContainerSlot(targetHotbarSlot);

        user.receivePacketSilently(new WrapperPlayClientCreativeInventoryAction(containerSlot, stack.copy()));
        user.sendPacket(new WrapperPlayServerSetPlayerInventory(targetHotbarSlot, stack.copy()));
        state.getInventorySnapshot().applySetPlayerInventory(targetHotbarSlot, stack.copy());
        return new FallbackResult(
                true,
                "cloned creative item into hotbar slot " + targetHotbarSlot + " through frontend proxy emulation"
        );
    }

    public record FallbackResult(boolean success, String detail) {
    }
}
