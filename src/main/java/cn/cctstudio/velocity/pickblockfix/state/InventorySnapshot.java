package cn.cctstudio.velocity.pickblockfix.state;

import cn.cctstudio.velocity.pickblockfix.util.InventorySlotUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Tracks the player inventory in a backend-agnostic, normalized 0..35 layout.
 */
public final class InventorySnapshot {

    private final ItemStack[] slots = new ItemStack[36];

    public InventorySnapshot() {
        clear();
    }

    public synchronized void clear() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public synchronized void applyWindowItems(List<ItemStack> containerItems) {
        clear();
        for (int containerSlot = 0; containerSlot < containerItems.size(); containerSlot++) {
            Integer inventoryIndex = InventorySlotUtil.containerSlotToInventoryIndex(containerSlot);
            if (inventoryIndex != null) {
                slots[inventoryIndex] = copy(containerItems.get(containerSlot));
            }
        }
    }

    public synchronized void applySetSlot(int windowId, int slot, ItemStack itemStack) {
        if (windowId != 0) {
            return;
        }
        Integer inventoryIndex = InventorySlotUtil.containerSlotToInventoryIndex(slot);
        if (inventoryIndex != null) {
            slots[inventoryIndex] = copy(itemStack);
        }
    }

    public synchronized void applySetPlayerInventory(int slot, ItemStack itemStack) {
        if (slot < 0 || slot >= slots.length) {
            return;
        }
        slots[slot] = copy(itemStack);
    }

    public synchronized Integer findFirstSlot(ItemType itemType) {
        return findFirstSlot(itemType, stack -> true);
    }

    public synchronized Integer findFirstSlot(ItemType itemType, Predicate<ItemStack> filter) {
        Integer hotbarMatch = findHotbarSlot(itemType, filter);
        if (hotbarMatch != null) {
            return hotbarMatch;
        }
        return findMainInventorySlot(itemType, filter);
    }

    public synchronized Integer findMainInventorySlot(ItemType itemType) {
        return findMainInventorySlot(itemType, stack -> true);
    }

    public synchronized Integer findMainInventorySlot(ItemType itemType, Predicate<ItemStack> filter) {
        for (int slot = 9; slot < slots.length; slot++) {
            if (matches(slot, itemType, filter)) {
                return slot;
            }
        }
        return null;
    }

    public synchronized Integer findHotbarSlot(ItemType itemType) {
        return findHotbarSlot(itemType, stack -> true);
    }

    public synchronized Integer findHotbarSlot(ItemType itemType, Predicate<ItemStack> filter) {
        return findHotbarSlot(itemType, filter, -1);
    }

    public synchronized Integer findHotbarSlot(ItemType itemType, Predicate<ItemStack> filter, int excludedSlot) {
        for (int slot = 0; slot <= 8; slot++) {
            if (slot == excludedSlot) {
                continue;
            }
            if (matches(slot, itemType, filter)) {
                return slot;
            }
        }
        return null;
    }

    public synchronized ItemStack getSlot(int slot) {
        if (slot < 0 || slot >= slots.length) {
            return ItemStack.EMPTY;
        }
        return copy(slots[slot]);
    }

    public synchronized void swapSlots(int firstSlot, int secondSlot) {
        if (firstSlot < 0 || firstSlot >= slots.length || secondSlot < 0 || secondSlot >= slots.length) {
            return;
        }
        ItemStack first = slots[firstSlot];
        slots[firstSlot] = copy(slots[secondSlot]);
        slots[secondSlot] = copy(first);
    }

    private boolean matches(int slot, ItemType itemType, Predicate<ItemStack> filter) {
        ItemStack stack = slots[slot];
        return stack != null
                && !stack.isEmpty()
                && stack.getType() == itemType
                && filter.test(stack);
    }

    private ItemStack copy(ItemStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }
}
