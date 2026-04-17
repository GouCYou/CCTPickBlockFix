package cn.cctstudio.velocity.pickblockfix.util;

/**
 * Converts between player inventory indexes and vanilla container slot indexes.
 */
public final class InventorySlotUtil {

    private InventorySlotUtil() {
    }

    public static Integer containerSlotToInventoryIndex(int containerSlot) {
        if (containerSlot >= 36 && containerSlot <= 44) {
            return containerSlot - 36;
        }
        if (containerSlot >= 9 && containerSlot <= 35) {
            return containerSlot;
        }
        return null;
    }

    public static int hotbarIndexToContainerSlot(int hotbarIndex) {
        if (!isHotbarSlot(hotbarIndex)) {
            throw new IllegalArgumentException("Hotbar index must be between 0 and 8.");
        }
        return 36 + hotbarIndex;
    }

    public static int inventoryIndexToContainerSlot(int inventorySlot) {
        if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        }
        if (isHotbarSlot(inventorySlot)) {
            return hotbarIndexToContainerSlot(inventorySlot);
        }
        throw new IllegalArgumentException("Inventory slot must be between 0 and 35.");
    }

    public static boolean isHotbarSlot(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot <= 8;
    }
}
