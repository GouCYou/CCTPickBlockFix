package cn.cctstudio.velocity.pickblockfix.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    @Test
    void parsesFlatBooleanConfig() {
        PickBlockFixConfig config = ConfigManager.parseLines(List.of(
                "enabled: false",
                "debug: true",
                "creative_only: false",
                "experimental_survival: true",
                "log_packet_ids: true"
        ), PickBlockFixConfig.defaults());

        assertFalse(config.enabled());
        assertTrue(config.debug());
        assertFalse(config.creativeOnly());
        assertTrue(config.experimentalSurvival());
        assertTrue(config.logPacketIds());
    }
}
