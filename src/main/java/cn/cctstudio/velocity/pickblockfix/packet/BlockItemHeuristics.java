package cn.cctstudio.velocity.pickblockfix.packet;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps translated block states and entity types to likely pick-result items.
 */
public final class BlockItemHeuristics {

    private static final Map<String, String> BLOCK_ALIASES = new HashMap<>();

    static {
        BLOCK_ALIASES.put("wall_torch", "torch");
        BLOCK_ALIASES.put("redstone_wall_torch", "redstone_torch");
        BLOCK_ALIASES.put("redstone_wire", "redstone");
        BLOCK_ALIASES.put("sweet_berry_bush", "sweet_berries");
        BLOCK_ALIASES.put("cave_vines", "glow_berries");
        BLOCK_ALIASES.put("cave_vines_plant", "glow_berries");
        BLOCK_ALIASES.put("bamboo_sapling", "bamboo");
        BLOCK_ALIASES.put("carrots", "carrot");
        BLOCK_ALIASES.put("potatoes", "potato");
        BLOCK_ALIASES.put("beetroots", "beetroot_seeds");
        BLOCK_ALIASES.put("wheat", "wheat_seeds");
        BLOCK_ALIASES.put("nether_wart", "nether_wart");
        BLOCK_ALIASES.put("torchflower_crop", "torchflower_seeds");
        BLOCK_ALIASES.put("pitcher_crop", "pitcher_pod");
    }

    private BlockItemHeuristics() {
    }

    public static ItemType mapBlockState(WrappedBlockState blockState) {
        if (blockState == null || blockState.getType().isAir()) {
            return null;
        }

        ItemType direct = ItemTypes.getTypePlacingState(blockState.getType());
        if (direct != null) {
            return direct;
        }

        String key = blockState.getType().getName();
        if (key.startsWith("potted_")) {
            ItemType unpotted = itemByKey(key.substring("potted_".length()));
            if (unpotted != null) {
                return unpotted;
            }
        }

        String alias = BLOCK_ALIASES.get(key);
        if (alias != null) {
            return itemByKey(alias);
        }

        // Wall-mounted signs use distinct block ids but still pick the handheld sign item.
        if (key.endsWith("_wall_hanging_sign")) {
            return itemByKey(key.substring(0, key.length() - "_wall_hanging_sign".length()) + "_hanging_sign");
        }
        if (key.endsWith("_wall_sign")) {
            return itemByKey(key.substring(0, key.length() - "_wall_sign".length()) + "_sign");
        }

        return itemByKey(key);
    }

    public static ItemType mapEntity(EntityType entityType) {
        if (entityType == null) {
            return null;
        }

        String key = entityType.getName().getKey();
        ItemType directItem = itemByKey(key);
        if (directItem != null) {
            return directItem;
        }

        return itemByKey(key + "_spawn_egg");
    }

    private static ItemType itemByKey(String key) {
        return ItemTypes.getByName("minecraft:" + key);
    }
}
