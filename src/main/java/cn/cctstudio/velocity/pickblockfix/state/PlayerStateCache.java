package cn.cctstudio.velocity.pickblockfix.state;

import com.velocitypowered.api.proxy.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe player state registry.
 */
public final class PlayerStateCache {

    private final ConcurrentHashMap<UUID, PlayerState> states = new ConcurrentHashMap<>();

    public PlayerState getOrCreate(Player player) {
        return states.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new PlayerState(player.getUniqueId(), player.getUsername())
        );
    }

    public PlayerState get(UUID uuid) {
        return states.get(uuid);
    }

    public void remove(UUID uuid) {
        states.remove(uuid);
    }

    public Collection<PlayerState> values() {
        return states.values();
    }
}
