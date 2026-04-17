package cn.cctstudio.velocity.pickblockfix.config;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and saves the flat config.yml file without bringing an extra YAML library.
 */
public final class ConfigManager {

    private final Path dataDirectory;
    private final Path configPath;
    private final Logger logger;

    private volatile PickBlockFixConfig current = PickBlockFixConfig.defaults();

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
        this.logger = logger;
    }

    public synchronized PickBlockFixConfig load() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.notExists(configPath)) {
                copyDefaultConfig();
            }
            current = parseLines(Files.readAllLines(configPath, StandardCharsets.UTF_8), PickBlockFixConfig.defaults());
        } catch (Exception exception) {
            logger.error("Failed to load config.yml, falling back to defaults.", exception);
            current = PickBlockFixConfig.defaults();
        }
        return current;
    }

    public synchronized PickBlockFixConfig save(PickBlockFixConfig config) {
        try {
            Files.createDirectories(dataDirectory);
            Files.write(configPath, config.toLines(), StandardCharsets.UTF_8);
            current = config;
        } catch (IOException exception) {
            logger.error("Failed to save config.yml.", exception);
        }
        return current;
    }

    public synchronized PickBlockFixConfig setDebug(boolean debug) {
        return save(current.withDebug(debug));
    }

    public PickBlockFixConfig current() {
        return current;
    }

    public static PickBlockFixConfig parseLines(List<String> lines, PickBlockFixConfig defaults) {
        Map<String, Boolean> values = new HashMap<>();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }

            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String rawValue = line.substring(separator + 1).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(rawValue) || "false".equals(rawValue)) {
                values.put(key, Boolean.parseBoolean(rawValue));
            }
        }

        return new PickBlockFixConfig(
                values.getOrDefault("enabled", defaults.enabled()),
                values.getOrDefault("debug", defaults.debug()),
                values.getOrDefault("creative_only", defaults.creativeOnly()),
                values.getOrDefault("experimental_survival", defaults.experimentalSurvival()),
                values.getOrDefault("only_when_backend_protocol_is_1_21_1", defaults.onlyWhenBackendProtocolIs1211()),
                values.getOrDefault("log_packet_names", defaults.logPacketNames()),
                values.getOrDefault("log_packet_ids", defaults.logPacketIds()),
                values.getOrDefault("cancel_unknown_pick_packets", defaults.cancelUnknownPickPackets()),
                values.getOrDefault("emulate_when_direct_rewrite_fails", defaults.emulateWhenDirectRewriteFails())
        );
    }

    private void copyDefaultConfig() throws IOException {
        try (InputStream inputStream = ConfigManager.class.getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null) {
                save(PickBlockFixConfig.defaults());
                return;
            }
            Files.copy(inputStream, configPath);
        }
    }
}
