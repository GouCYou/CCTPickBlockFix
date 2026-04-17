package cn.cctstudio.velocity.pickblockfix.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat runtime configuration for the plugin.
 */
public final class PickBlockFixConfig {

    private final boolean enabled;
    private final boolean debug;
    private final boolean creativeOnly;
    private final boolean experimentalSurvival;
    private final boolean onlyWhenBackendProtocolIs1211;
    private final boolean logPacketNames;
    private final boolean logPacketIds;
    private final boolean cancelUnknownPickPackets;
    private final boolean emulateWhenDirectRewriteFails;

    public PickBlockFixConfig(
            boolean enabled,
            boolean debug,
            boolean creativeOnly,
            boolean experimentalSurvival,
            boolean onlyWhenBackendProtocolIs1211,
            boolean logPacketNames,
            boolean logPacketIds,
            boolean cancelUnknownPickPackets,
            boolean emulateWhenDirectRewriteFails
    ) {
        this.enabled = enabled;
        this.debug = debug;
        this.creativeOnly = creativeOnly;
        this.experimentalSurvival = experimentalSurvival;
        this.onlyWhenBackendProtocolIs1211 = onlyWhenBackendProtocolIs1211;
        this.logPacketNames = logPacketNames;
        this.logPacketIds = logPacketIds;
        this.cancelUnknownPickPackets = cancelUnknownPickPackets;
        this.emulateWhenDirectRewriteFails = emulateWhenDirectRewriteFails;
    }

    public static PickBlockFixConfig defaults() {
        return new PickBlockFixConfig(
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                true
        );
    }

    public PickBlockFixConfig withDebug(boolean debug) {
        return new PickBlockFixConfig(
                this.enabled,
                debug,
                this.creativeOnly,
                this.experimentalSurvival,
                this.onlyWhenBackendProtocolIs1211,
                this.logPacketNames,
                this.logPacketIds,
                this.cancelUnknownPickPackets,
                this.emulateWhenDirectRewriteFails
        );
    }

    public List<String> toLines() {
        List<String> lines = new ArrayList<>();
        lines.add("# CCTPickBlockFix experimental proxy-side configuration.");
        lines.add("# This file is intentionally flat so it can be parsed without external YAML libraries.");
        lines.add("enabled: " + enabled);
        lines.add("debug: " + debug);
        lines.add("creative_only: " + creativeOnly);
        lines.add("experimental_survival: " + experimentalSurvival);
        lines.add("only_when_backend_protocol_is_1_21_1: " + onlyWhenBackendProtocolIs1211);
        lines.add("log_packet_names: " + logPacketNames);
        lines.add("log_packet_ids: " + logPacketIds);
        lines.add("cancel_unknown_pick_packets: " + cancelUnknownPickPackets);
        lines.add("emulate_when_direct_rewrite_fails: " + emulateWhenDirectRewriteFails);
        return lines;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean debug() {
        return debug;
    }

    public boolean creativeOnly() {
        return creativeOnly;
    }

    public boolean experimentalSurvival() {
        return experimentalSurvival;
    }

    public boolean onlyWhenBackendProtocolIs1211() {
        return onlyWhenBackendProtocolIs1211;
    }

    public boolean logPacketNames() {
        return logPacketNames;
    }

    public boolean logPacketIds() {
        return logPacketIds;
    }

    public boolean cancelUnknownPickPackets() {
        return cancelUnknownPickPackets;
    }

    public boolean emulateWhenDirectRewriteFails() {
        return emulateWhenDirectRewriteFails;
    }
}
