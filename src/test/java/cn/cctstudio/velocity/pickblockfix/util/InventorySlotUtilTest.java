package cn.cctstudio.velocity.pickblockfix.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySlotUtilTest {

    @Test
    void translatesPlayerContainerSlots() {
        assertEquals(0, InventorySlotUtil.containerSlotToInventoryIndex(36));
        assertEquals(8, InventorySlotUtil.containerSlotToInventoryIndex(44));
        assertEquals(9, InventorySlotUtil.containerSlotToInventoryIndex(9));
        assertEquals(35, InventorySlotUtil.containerSlotToInventoryIndex(35));
        assertNull(InventorySlotUtil.containerSlotToInventoryIndex(45));
    }

    @Test
    void mapsHotbarIndicesBackToContainerSlots() {
        assertEquals(36, InventorySlotUtil.hotbarIndexToContainerSlot(0));
        assertEquals(44, InventorySlotUtil.hotbarIndexToContainerSlot(8));
        assertTrue(InventorySlotUtil.isHotbarSlot(4));
    }

    @Test
    void mapsNormalizedInventoryIndicesToContainerSlots() {
        assertEquals(36, InventorySlotUtil.inventoryIndexToContainerSlot(0));
        assertEquals(41, InventorySlotUtil.inventoryIndexToContainerSlot(5));
        assertEquals(9, InventorySlotUtil.inventoryIndexToContainerSlot(9));
        assertEquals(35, InventorySlotUtil.inventoryIndexToContainerSlot(35));
    }
}
