package cn.cctstudio.velocity.pickblockfix.state;

import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolInfo;
import cn.cctstudio.velocity.pickblockfix.debug.PickAttemptTrace;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates everything the experimental fix tracks per player.
 */
public final class PlayerState {

    private final UUID playerId;
    private final InventorySnapshot inventorySnapshot = new InventorySnapshot();
    private final WorldCache worldCache = new WorldCache();
    private final Map<Integer, EntityType> entities = new ConcurrentHashMap<>();

    private volatile String playerName;
    private volatile ClientVersion clientVersion = ClientVersion.UNKNOWN;
    private volatile GameMode gameMode = GameMode.SURVIVAL;
    private volatile int heldHotbarSlot;
    private volatile String backendServerName = "unknown";
    private volatile BackendProtocolInfo backendProtocolInfo = BackendProtocolInfo.unknown("unknown", "not connected");
    private volatile int minWorldHeight = -64;
    private volatile int totalWorldHeight = 384;
    private volatile int playerInventoryStateId;
    private volatile PickAttemptTrace lastTrace;

    public PlayerState(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public void resetForServerSwitch(String backendServerName, BackendProtocolInfo protocolInfo) {
        this.backendServerName = backendServerName;
        this.backendProtocolInfo = protocolInfo;
        this.gameMode = GameMode.SURVIVAL;
        this.worldCache.clear();
        this.inventorySnapshot.clear();
        this.entities.clear();
        this.playerInventoryStateId = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public ClientVersion getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(ClientVersion clientVersion) {
        if (clientVersion != null) {
            this.clientVersion = clientVersion;
        }
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        if (gameMode != null) {
            this.gameMode = gameMode;
        }
    }

    public boolean isCreative() {
        return this.gameMode == GameMode.CREATIVE;
    }

    public int getHeldHotbarSlot() {
        return heldHotbarSlot;
    }

    public void setHeldHotbarSlot(int heldHotbarSlot) {
        if (heldHotbarSlot >= 0 && heldHotbarSlot <= 8) {
            this.heldHotbarSlot = heldHotbarSlot;
        }
    }

    public String getBackendServerName() {
        return backendServerName;
    }

    public BackendProtocolInfo getBackendProtocolInfo() {
        return backendProtocolInfo;
    }

    public void setBackendProtocolInfo(BackendProtocolInfo backendProtocolInfo) {
        if (backendProtocolInfo != null) {
            this.backendProtocolInfo = backendProtocolInfo;
        }
    }

    public int getMinWorldHeight() {
        return minWorldHeight;
    }

    public int getTotalWorldHeight() {
        return totalWorldHeight;
    }

    public int getSectionCount() {
        return Math.max(1, totalWorldHeight >> 4);
    }

    public void setWorldDimensions(int minWorldHeight, int totalWorldHeight) {
        this.minWorldHeight = minWorldHeight;
        if (totalWorldHeight > 0) {
            this.totalWorldHeight = totalWorldHeight;
        }
    }

    public InventorySnapshot getInventorySnapshot() {
        return inventorySnapshot;
    }

    public int getPlayerInventoryStateId() {
        return playerInventoryStateId;
    }

    public void setPlayerInventoryStateId(int playerInventoryStateId) {
        if (playerInventoryStateId >= 0) {
            this.playerInventoryStateId = playerInventoryStateId;
        }
    }

    public WorldCache getWorldCache() {
        return worldCache;
    }

    public Map<Integer, EntityType> getEntities() {
        return entities;
    }

    public PickAttemptTrace getLastTrace() {
        return lastTrace;
    }

    public void setLastTrace(PickAttemptTrace lastTrace) {
        this.lastTrace = lastTrace;
    }
}
