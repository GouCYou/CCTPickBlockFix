package cn.cctstudio.velocity.pickblockfix.debug;

import cn.cctstudio.velocity.pickblockfix.config.PickBlockFixConfig;
import cn.cctstudio.velocity.pickblockfix.state.PlayerState;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

/**
 * Centralizes debug decisions so packet logic can stay focused on behavior.
 */
public final class DebugLogger {

    private final Logger logger;
    private final Supplier<PickBlockFixConfig> configSupplier;

    public DebugLogger(Logger logger, Supplier<PickBlockFixConfig> configSupplier) {
        this.logger = logger;
        this.configSupplier = configSupplier;
    }

    public void debug(PlayerState state, String message) {
        if (!configSupplier.get().debug()) {
            return;
        }
        logger.info("[CCTPickBlockFix][debug][{}] {}", state.getPlayerName(), message);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn("[CCTPickBlockFix] {}", message, throwable);
    }

    public void info(String message) {
        logger.info("[CCTPickBlockFix] {}", message);
    }

    public void storeTrace(PlayerState state, PickAttemptTrace trace) {
        state.setLastTrace(trace);
        if (!configSupplier.get().debug()) {
            return;
        }
        for (String line : trace.renderLines()) {
            logger.info("[CCTPickBlockFix][debug][{}] {}", state.getPlayerName(), line);
        }
    }

    public List<String> dump(PlayerState state) {
        PickAttemptTrace trace = state.getLastTrace();
        if (trace == null) {
            return List.of("No pick attempt has been recorded for this player yet.");
        }
        return trace.renderLines();
    }
}
