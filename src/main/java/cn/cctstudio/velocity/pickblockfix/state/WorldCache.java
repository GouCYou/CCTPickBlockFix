package cn.cctstudio.velocity.pickblockfix.state;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-player best-effort cache of translated block states visible to the client.
 */
public final class WorldCache {

    /**
     * Lobby-style joins can easily stream several hundred columns in one burst.
     * Keeping only 64 columns causes the spawn area to be evicted before the player
     * finishes loading, which makes proxy-side pick resolution fail immediately.
     */
    private static final int MAX_COLUMNS = 1024;

    private final Map<Long, Column> columns = new LinkedHashMap<>(MAX_COLUMNS, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Column> eldest) {
            return size() > MAX_COLUMNS;
        }
    };

    public synchronized void clear() {
        columns.clear();
    }

    public synchronized void storeColumn(Column column) {
        columns.put(PacketWrapper.getChunkKey(column.getX(), column.getZ()), column);
    }

    public synchronized void unloadColumn(int chunkX, int chunkZ) {
        columns.remove(PacketWrapper.getChunkKey(chunkX, chunkZ));
    }

    public synchronized void applyBlockChange(PlayerState state, Vector3i position, WrappedBlockState blockState) {
        Column column = getOrCreateColumn(state, Math.floorDiv(position.getX(), 16), Math.floorDiv(position.getZ(), 16));
        int sectionIndex = toSectionIndex(state, position.getY());
        if (column == null || sectionIndex < 0 || sectionIndex >= column.getChunks().length) {
            return;
        }
        BaseChunk chunk = column.getChunks()[sectionIndex];
        if (chunk == null) {
            chunk = BaseChunk.create();
            column.getChunks()[sectionIndex] = chunk;
        }
        int localY = Math.floorMod(position.getY() - state.getMinWorldHeight(), 16);
        chunk.set(
                Math.floorMod(position.getX(), 16),
                localY,
                Math.floorMod(position.getZ(), 16),
                blockState.getGlobalId()
        );
    }

    public synchronized void applyMultiBlockChange(
            PlayerState state,
            Vector3i chunkPosition,
            WrapperPlayServerMultiBlockChange.EncodedBlock[] blocks
    ) {
        if (blocks == null) {
            return;
        }
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            applyBlockChange(
                    state,
                    new Vector3i(block.getX(), block.getY(), block.getZ()),
                    block.getBlockState(state.getClientVersion())
            );
        }
    }

    public synchronized WrappedBlockState resolveBlockState(PlayerState state, Vector3i position) {
        Column column = columns.get(PacketWrapper.getChunkKey(Math.floorDiv(position.getX(), 16), Math.floorDiv(position.getZ(), 16)));
        if (column == null) {
            return null;
        }
        int sectionIndex = toSectionIndex(state, position.getY());
        if (sectionIndex < 0 || sectionIndex >= column.getChunks().length) {
            return null;
        }
        BaseChunk chunk = column.getChunks()[sectionIndex];
        if (chunk == null || chunk.isEmpty()) {
            return null;
        }
        ClientVersion clientVersion = state.getClientVersion();
        if (clientVersion == null) {
            return null;
        }
        return chunk.get(
                clientVersion,
                Math.floorMod(position.getX(), 16),
                Math.floorMod(position.getY() - state.getMinWorldHeight(), 16),
                Math.floorMod(position.getZ(), 16)
        );
    }

    public synchronized String describeLookup(PlayerState state, Vector3i position) {
        int chunkX = Math.floorDiv(position.getX(), 16);
        int chunkZ = Math.floorDiv(position.getZ(), 16);
        Column column = columns.get(PacketWrapper.getChunkKey(chunkX, chunkZ));
        if (column == null) {
            return "cacheColumns=" + columns.size()
                    + ", targetChunk=" + chunkX + "," + chunkZ
                    + ", chunkPresent=false";
        }

        int sectionIndex = toSectionIndex(state, position.getY());
        BaseChunk[] chunks = column.getChunks();
        if (sectionIndex < 0 || sectionIndex >= chunks.length) {
            return "cacheColumns=" + columns.size()
                    + ", targetChunk=" + chunkX + "," + chunkZ
                    + ", chunkPresent=true, sectionIndex=" + sectionIndex
                    + ", sectionCount=" + chunks.length;
        }

        BaseChunk section = chunks[sectionIndex];
        return "cacheColumns=" + columns.size()
                + ", targetChunk=" + chunkX + "," + chunkZ
                + ", chunkPresent=true, sectionIndex=" + sectionIndex
                + ", sectionPresent=" + (section != null)
                + ", sectionEmpty=" + (section == null || section.isEmpty());
    }

    private Column getOrCreateColumn(PlayerState state, int chunkX, int chunkZ) {
        long key = PacketWrapper.getChunkKey(chunkX, chunkZ);
        Column existing = columns.get(key);
        if (existing != null) {
            return existing;
        }
        int sectionCount = Math.max(1, state.getSectionCount());
        Column created = new Column(chunkX, chunkZ, true, new BaseChunk[sectionCount], new TileEntity[0]);
        columns.put(key, created);
        return created;
    }

    private int toSectionIndex(PlayerState state, int y) {
        int shifted = y - state.getMinWorldHeight();
        if (shifted < 0) {
            return -1;
        }
        return shifted >> 4;
    }
}
