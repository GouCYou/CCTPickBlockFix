package cn.cctstudio.velocity.pickblockfix.packet;

import cn.cctstudio.velocity.pickblockfix.config.PickBlockFixConfig;
import cn.cctstudio.velocity.pickblockfix.debug.DebugLogger;
import cn.cctstudio.velocity.pickblockfix.debug.PickAttemptTrace;
import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import cn.cctstudio.velocity.pickblockfix.util.InventorySlotUtil;
import cn.cctstudio.velocity.pickblockfix.util.ItemStackUtil;
import cn.cctstudio.velocity.pickblockfix.util.VersionUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Orchestrates activation checks, direct legacy rewrite and the safe fallback path.
 */
public final class PickPacketRewriter {

    private final Supplier<PickBlockFixConfig> configSupplier;
    private final DebugLogger debugLogger;
    private final PickPacketRecognizer recognizer;
    private final FallbackExecutor fallbackExecutor;

    public PickPacketRewriter(
            Supplier<PickBlockFixConfig> configSupplier,
            DebugLogger debugLogger,
            PickPacketRecognizer recognizer,
            FallbackExecutor fallbackExecutor
    ) {
        this.configSupplier = configSupplier;
        this.debugLogger = debugLogger;
        this.recognizer = recognizer;
        this.fallbackExecutor = fallbackExecutor;
    }

    public boolean handle(PacketReceiveEvent event, Player player, PlayerState state) {
        Optional<PickPacketRecognizer.RecognizedPickPacket> recognizedOptional = recognizer.recognize(event);
        if (recognizedOptional.isEmpty()) {
            if (!recognizer.looksLikePickPacket(event)) {
                return false;
            }

            PickAttemptTrace trace = createTrace(player, state, event);
            trace.addFailure("Observed a pick-like packet that this build does not understand.");
            if (configSupplier.get().cancelUnknownPickPackets()) {
                event.setCancelled(true);
                trace.markCancelled();
                trace.addAction("Unknown pick-like packet cancelled by configuration.");
            }
            trace.setOutcome("Unhandled modern pick packet.");
            debugLogger.storeTrace(state, trace);
            return true;
        }

        PickPacketRecognizer.RecognizedPickPacket recognized = recognizedOptional.get();
        PickAttemptTrace trace = createTrace(player, state, event);
        if (recognized.kind() == PickPacketRecognizer.Kind.BLOCK) {
            trace.setTargetBlock(recognized.blockPos(), recognized.includeData());
        } else {
            trace.setTargetEntity(recognized.entityId(), recognized.includeData());
        }

        PickBlockFixConfig config = configSupplier.get();
        if (!config.enabled()) {
            trace.setOutcome("Plugin disabled, packet bypassed.");
            debugLogger.storeTrace(state, trace);
            return true;
        }
        if (!VersionUtil.isModernPickClient(state.getClientVersion())) {
            trace.setOutcome("Native 1.21.1-or-older client, packet bypassed.");
            debugLogger.storeTrace(state, trace);
            return true;
        }
        if (config.onlyWhenBackendProtocolIs1211() && !VersionUtil.isTargetBackend1211(state.getBackendProtocolInfo())) {
            trace.addFailure("Backend protocol gate rejected " + state.getBackendProtocolInfo().render());
            trace.setOutcome("Backend does not look like 1.21.1, packet bypassed.");
            debugLogger.storeTrace(state, trace);
            return true;
        }
        if (!isGamemodeAllowed(config, state)) {
            trace.addFailure("Gamemode " + state.getGameMode() + " is outside the configured activation range.");
            trace.setOutcome("Gamemode gate rejected the fix path.");
            debugLogger.storeTrace(state, trace);
            return true;
        }

        event.setCancelled(true);
        trace.markCancelled();
        trace.addAction("Cancelled modern frontend pick packet before backend forwarding.");

        PickResolution resolution = resolve(state, recognized, trace);
        if (resolution == null) {
            trace.setOutcome("Target resolution failed.");
            debugLogger.storeTrace(state, trace);
            return true;
        }

        trace.addAction("Resolved target to " + resolution.itemType().getName() + ", slot=" + resolution.inventorySlot());

        if (resolution.inventorySlot() != null) {
            FallbackExecutor.FallbackResult inventoryResult = fallbackExecutor.tryFallback(player, state, resolution);
            if (inventoryResult.success()) {
                if (InventorySlotUtil.isHotbarSlot(resolution.inventorySlot())) {
                    trace.addAction("Hotbar selection succeeded: " + inventoryResult.detail());
                    trace.setOutcome("Existing hotbar item selected.");
                } else if (state.isCreative()) {
                    trace.addAction("Creative inventory move succeeded: " + inventoryResult.detail());
                    trace.setOutcome("Existing inventory item moved into the hotbar.");
                } else {
                    trace.addAction("Survival inventory swap succeeded: " + inventoryResult.detail());
                    trace.setOutcome("Survival inventory swap succeeded.");
                }
                debugLogger.storeTrace(state, trace);
                return true;
            }
            trace.addFailure("Inventory-based handling failed: " + inventoryResult.detail());
        } else if (!state.isCreative()) {
            trace.addFailure("Inventory snapshot does not contain the resolved item, direct rewrite skipped.");
        }

        if (config.emulateWhenDirectRewriteFails()) {
            FallbackExecutor.FallbackResult fallbackResult = fallbackExecutor.tryFallback(player, state, resolution);
            if (fallbackResult.success()) {
                trace.addAction("Fallback emulate succeeded: " + fallbackResult.detail());
                trace.setOutcome("Fallback emulate succeeded.");
                debugLogger.storeTrace(state, trace);
                return true;
            }
            trace.addFailure("Fallback emulate failed: " + fallbackResult.detail());
        } else {
            trace.addFailure("Fallback path disabled by configuration.");
        }

        trace.setOutcome("Modern packet cancelled but no direct rewrite or fallback succeeded.");
        debugLogger.storeTrace(state, trace);
        return true;
    }

    private PickAttemptTrace createTrace(Player player, PlayerState state, PacketReceiveEvent event) {
        PickAttemptTrace trace = new PickAttemptTrace();
        trace.setPlayerName(player.getUsername());
        trace.setClientVersion(state.getClientVersion());
        trace.setBackend(state.getBackendServerName(), state.getBackendProtocolInfo());
        trace.setPacket(recognizer.packetName(event), event.getPacketId());
        return trace;
    }

    private PickResolution resolve(
            PlayerState state,
            PickPacketRecognizer.RecognizedPickPacket recognized,
            PickAttemptTrace trace
    ) {
        if (recognized.kind() == PickPacketRecognizer.Kind.BLOCK) {
            WrappedBlockState blockState = state.getWorldCache().resolveBlockState(state, recognized.blockPos());
            if (blockState == null) {
                trace.addFailure(
                        "World cache does not contain block state for "
                                + recognized.blockPos()
                                + " (" + state.getWorldCache().describeLookup(state, recognized.blockPos()) + ")"
                );
                return null;
            }
            ItemType itemType = BlockItemHeuristics.mapBlockState(blockState);
            if (itemType == null) {
                trace.addFailure("No block-to-item mapping is available for translated state " + blockState.getType().getName());
                return null;
            }
            return buildResolution(state, itemType, "block state " + blockState.getType().getName(), trace);
        }

        EntityType entityType = state.getEntities().get(recognized.entityId());
        if (entityType == null) {
            trace.addFailure("Entity cache does not contain entity id " + recognized.entityId());
            return null;
        }
        ItemType itemType = BlockItemHeuristics.mapEntity(entityType);
        if (itemType == null) {
            trace.addFailure("No entity-to-item mapping is available for entity type " + entityType.getName());
            return null;
        }
        return buildResolution(state, itemType, "entity type " + entityType.getName(), trace);
    }

    private PickResolution buildResolution(
            PlayerState state,
            ItemType itemType,
            String description,
            PickAttemptTrace trace
    ) {
        ItemStack vanillaTemplate = ItemStackUtil.createVanillaTemplate(state.getClientVersion(), itemType);
        Integer slot = null;

        if (state.isCreative()) {
            int heldSlot = state.getHeldHotbarSlot();
            ItemStack heldStack = state.getInventorySnapshot().getSlot(heldSlot);
            if (ItemStackUtil.isPlainVanillaMatch(heldStack, state.getClientVersion(), itemType)) {
                slot = heldSlot;
            } else {
                slot = state.getInventorySnapshot().findMainInventorySlot(
                        itemType,
                        stack -> ItemStackUtil.isPlainVanillaMatch(stack, state.getClientVersion(), itemType)
                );
                if (slot == null) {
                    slot = state.getInventorySnapshot().findHotbarSlot(
                            itemType,
                            stack -> ItemStackUtil.isPlainVanillaMatch(stack, state.getClientVersion(), itemType),
                            heldSlot
                    );
                    if (slot != null) {
                        trace.addAction("Creative mode is reusing plain hotbar slot " + slot + " after main inventory lookup missed.");
                    }
                }
            }
        } else {
            slot = state.getInventorySnapshot().findFirstSlot(itemType);
        }

        if (slot == null && state.isCreative()) {
            Integer decoratedSlot = state.getInventorySnapshot().findFirstSlot(itemType);
            if (decoratedSlot != null) {
                trace.addAction(
                        "Ignored inventory slot " + decoratedSlot
                                + " because the matching item is not a plain vanilla stack."
                );
            }
        }
        ItemStack preferredStack = slot == null
                ? vanillaTemplate
                : state.getInventorySnapshot().getSlot(slot);
        return new PickResolution(itemType, preferredStack, slot, description);
    }

    private boolean isGamemodeAllowed(PickBlockFixConfig config, PlayerState state) {
        GameMode gameMode = state.getGameMode();
        if (gameMode == GameMode.CREATIVE) {
            return true;
        }
        if (config.creativeOnly()) {
            return false;
        }
        return config.experimentalSurvival() && gameMode == GameMode.SURVIVAL;
    }
}
