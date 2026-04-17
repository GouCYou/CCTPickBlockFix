package cn.cctstudio.velocity.pickblockfix.util;

import cn.cctstudio.velocity.pickblockfix.backend.BackendProtocolInfo;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.velocitypowered.api.network.ProtocolVersion;

/**
 * Shared version helpers used by commands, diagnostics and packet logic.
 */
public final class VersionUtil {

    public static final int PROTOCOL_1_21_1 = 767;
    public static final ClientVersion CLIENT_1_21_1 = ClientVersion.V_1_21;
    public static final ServerVersion SERVER_1_21_1 = ServerVersion.V_1_21_1;

    private VersionUtil() {
    }

    public static ClientVersion toClientVersion(ProtocolVersion protocolVersion) {
        if (protocolVersion == null || !protocolVersion.isSupported()) {
            return ClientVersion.UNKNOWN;
        }
        return ClientVersion.getById(protocolVersion.getProtocol());
    }

    public static boolean isModernPickClient(ClientVersion clientVersion) {
        return clientVersion != null && clientVersion.isNewerThan(CLIENT_1_21_1);
    }

    public static boolean isNative1211Client(ClientVersion clientVersion) {
        return clientVersion != null && !clientVersion.isNewerThan(CLIENT_1_21_1);
    }

    public static boolean isTargetBackend1211(BackendProtocolInfo info) {
        return info != null && info.successful() && info.protocol() == PROTOCOL_1_21_1;
    }

    public static String describeClientVersion(ClientVersion version) {
        if (version == null || version == ClientVersion.UNKNOWN) {
            return "unknown";
        }
        return version.getReleaseName() + " (" + version.getProtocolVersion() + ")";
    }
}
