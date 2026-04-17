package cn.cctstudio.velocity.pickblockfix.packet;

import cn.cctstudio.velocity.pickblockfix.debug.DebugLogger;
import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.state.PlayerStateCache;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.velocitypowered.api.proxy.Player;

/**
 * PacketEvents listener that gathers state and delegates modern pick packets to the rewriter.
 */
public final class PickPacketListener extends PacketListenerAbstract {

    private final PlayerStateCache stateCache;
    private final PickPacketRewriter packetRewriter;
    private final DebugLogger debugLogger;

    public PickPacketListener(
            PlayerStateCache stateCache,
            PickPacketRewriter packetRewriter,
            DebugLogger debugLogger
    ) {
        super(PacketListenerPriority.HIGHEST);
        this.stateCache = stateCache;
        this.packetRewriter = packetRewriter;
        this.debugLogger = debugLogger;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (event.getConnectionState() != ConnectionState.PLAY || !(event.getPlayer() instanceof Player player)) {
                return;
            }

            PlayerState state = stateCache.getOrCreate(player);
            state.setPlayerName(player.getUsername());
            state.setClientVersion(event.getUser().getClientVersion());
            state.setWorldDimensions(event.getUser().getMinWorldHeight(), event.getUser().getTotalWorldHeight());

            if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
                WrapperPlayClientHeldItemChange wrapper = new WrapperPlayClientHeldItemChange(event);
                state.setHeldHotbarSlot(wrapper.getSlot());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
                WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
                state.getInventorySnapshot().applySetSlot(0, wrapper.getSlot(), wrapper.getItemStack());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
                if (wrapper.getWindowId() == 0) {
                    wrapper.getStateId().ifPresent(state::setPlayerInventoryStateId);
                    wrapper.getSlots().ifPresent(changedSlots -> {
                        for (var entry : changedSlots.entrySet()) {
                            state.getInventorySnapshot().applySetSlot(0, entry.getKey(), entry.getValue());
                        }
                    });
                }
                return;
            }

            packetRewriter.handle(event, player, state);
        } catch (Throwable throwable) {
            debugLogger.warn("Failed to process inbound packet.", throwable);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        try {
            if (event.getConnectionState() != ConnectionState.PLAY || !(event.getPlayer() instanceof Player player)) {
                return;
            }

            PlayerState state = stateCache.getOrCreate(player);
            state.setPlayerName(player.getUsername());
            state.setClientVersion(event.getUser().getClientVersion());
            state.setWorldDimensions(event.getUser().getMinWorldHeight(), event.getUser().getTotalWorldHeight());

            if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
                WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
                state.getInventorySnapshot().clear();
                state.getWorldCache().clear();
                state.getEntities().clear();
                state.setGameMode(wrapper.getGameMode());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
                WrapperPlayServerRespawn wrapper = new WrapperPlayServerRespawn(event);
                state.getWorldCache().clear();
                state.getEntities().clear();
                state.setGameMode(wrapper.getGameMode());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
                WrapperPlayServerChangeGameState wrapper = new WrapperPlayServerChangeGameState(event);
                if (wrapper.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
                    state.setGameMode(GameMode.getById((int) wrapper.getValue()));
                }
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.HELD_ITEM_CHANGE) {
                WrapperPlayServerHeldItemChange wrapper = new WrapperPlayServerHeldItemChange(event);
                state.setHeldHotbarSlot(wrapper.getSlot());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
                if (wrapper.getWindowId() == 0) {
                    state.setPlayerInventoryStateId(wrapper.getStateId());
                    state.getInventorySnapshot().applyWindowItems(wrapper.getItems());
                }
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
                if (wrapper.getWindowId() == 0) {
                    state.setPlayerInventoryStateId(wrapper.getStateId());
                }
                state.getInventorySnapshot().applySetSlot(wrapper.getWindowId(), wrapper.getSlot(), wrapper.getItem());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
                WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
                state.getInventorySnapshot().applySetPlayerInventory(wrapper.getSlot(), wrapper.getStack());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
                WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
                state.getWorldCache().storeColumn(wrapper.getColumn());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
                WrapperPlayServerUnloadChunk wrapper = new WrapperPlayServerUnloadChunk(event);
                state.getWorldCache().unloadColumn(wrapper.getChunkX(), wrapper.getChunkZ());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
                state.getWorldCache().applyBlockChange(state, wrapper.getBlockPosition(), wrapper.getBlockState());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);
                state.getWorldCache().applyMultiBlockChange(state, wrapper.getChunkPosition(), wrapper.getBlocks());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
                state.getEntities().put(wrapper.getEntityId(), wrapper.getEntityType());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
                WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
                state.getEntities().put(wrapper.getEntityId(), wrapper.getEntityType());
                return;
            }
            if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
                WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(event);
                for (int entityId : wrapper.getEntityIds()) {
                    state.getEntities().remove(entityId);
                }
            }
        } catch (Throwable throwable) {
            debugLogger.warn("Failed to process outbound packet.", throwable);
        }
    }
}
