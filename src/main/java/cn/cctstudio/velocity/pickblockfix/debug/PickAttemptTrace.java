package cn.cctstudio.velocity.pickblockfix.debug;

import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolInfo;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3i;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the last experimental pick attempt for one player.
 */
public final class PickAttemptTrace {

    private final Instant timestamp = Instant.now();
    private final List<String> actions = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    private String playerName = "unknown";
    private String clientVersion = "unknown";
    private String backendName = "unknown";
    private String backendProtocol = "unknown";
    private String packetName = "unknown";
    private int packetId = -1;
    private String target = "unknown";
    private boolean cancelled;
    private String outcome = "not finished";

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setClientVersion(ClientVersion version) {
        if (version != null) {
            this.clientVersion = version.getReleaseName() + " (" + version.getProtocolVersion() + ")";
        }
    }

    public void setBackend(String backendName, BackendProtocolInfo protocolInfo) {
        this.backendName = backendName == null ? "unknown" : backendName;
        this.backendProtocol = protocolInfo == null ? "unknown" : protocolInfo.render();
    }

    public void setPacket(String packetName, int packetId) {
        this.packetName = packetName;
        this.packetId = packetId;
    }

    public void setTargetBlock(Vector3i position, boolean includeData) {
        this.target = "block " + position + ", includeData=" + includeData;
    }

    public void setTargetEntity(int entityId, boolean includeData) {
        this.target = "entity " + entityId + ", includeData=" + includeData;
    }

    public void markCancelled() {
        this.cancelled = true;
    }

    public void addAction(String action) {
        actions.add(action);
    }

    public void addFailure(String failure) {
        failures.add(failure);
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public List<String> renderLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Timestamp: " + timestamp);
        lines.add("Player: " + playerName);
        lines.add("Client version: " + clientVersion);
        lines.add("Backend server: " + backendName);
        lines.add("Backend protocol: " + backendProtocol);
        lines.add("Packet: " + packetName + " (id=" + packetId + ")");
        lines.add("Target: " + target);
        lines.add("Cancelled: " + cancelled);
        lines.add("Outcome: " + outcome);
        if (actions.isEmpty()) {
            lines.add("Actions: none");
        } else {
            for (String action : actions) {
                lines.add("Action: " + action);
            }
        }
        if (failures.isEmpty()) {
            lines.add("Failures: none");
        } else {
            for (String failure : failures) {
                lines.add("Failure: " + failure);
            }
        }
        return lines;
    }
}
