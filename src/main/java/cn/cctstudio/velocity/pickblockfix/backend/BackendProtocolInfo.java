package cn.cctstudio.velocity.pickblockfix.backend;

import java.time.Instant;

/**
 * Cached backend ping information used for activation gating and diagnostics.
 */
public record BackendProtocolInfo(
        String serverName,
        int protocol,
        String versionName,
        Instant observedAt,
        boolean successful,
        String failureReason
) {

    public static BackendProtocolInfo unknown(String serverName, String reason) {
        return new BackendProtocolInfo(serverName, -1, "unknown", Instant.now(), false, reason);
    }

    public String render() {
        if (successful) {
            return versionName + " (" + protocol + ")";
        }
        return "unknown" + (failureReason == null || failureReason.isBlank() ? "" : " [" + failureReason + "]");
    }
}
