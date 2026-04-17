package cn.cctstudio.velocity.pickblockfix.packet;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;

/**
 * The result of resolving a modern pick target into an item and, optionally, an existing slot.
 */
public record PickResolution(
        ItemType itemType,
        ItemStack preferredStack,
        Integer inventorySlot,
        String description
) {
}
