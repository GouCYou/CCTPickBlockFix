package cn.cctstudio.velocity.pickblockfix.util;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

/**
 * Utility helpers for deciding whether an inventory item is safe to reuse for pick-block.
 */
public final class ItemStackUtil {

    private ItemStackUtil() {
    }

    public static ItemStack createVanillaTemplate(ClientVersion version, ItemType itemType) {
        return ItemStack.builder()
                .version(version)
                .type(itemType)
                .build();
    }

    public static boolean isPlainVanillaMatch(ItemStack stack, ClientVersion version, ItemType itemType) {
        if (stack == null || stack.isEmpty() || stack.getType() != itemType) {
            return false;
        }
        if (stack.hasComponentPatches()) {
            return false;
        }
        if (stack.getNBT() != null && !stack.getNBT().getTagNames().isEmpty()) {
            return false;
        }
        ItemStack vanillaTemplate = createVanillaTemplate(version, itemType);
        return ItemStack.isSameItemSameComponents(stack, vanillaTemplate);
    }
}
