package cn.cctstudio.velocity.pickblockfix.backend;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.netty.channel.Channel;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * PacketEvents' Velocity module does not expose backend channels directly, so this class performs
 * a bounded reflective search starting from Velocity's current server connection object.
 */
public final class BackendConnectionLocator {

    public Optional<Channel> findBackendChannel(Player player) {
        Object root = player.getCurrentServer().orElse(null);
        if (root == null) {
            return Optional.empty();
        }

        List<Channel> discovered = new ArrayList<>();
        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        scan(root, 6, visited, discovered);
        if (discovered.isEmpty()) {
            return Optional.empty();
        }

        InetSocketAddress targetAddress = player.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getAddress)
                .orElse(null);

        for (Channel channel : discovered) {
            if (!channel.isOpen()) {
                continue;
            }
            if (targetAddress == null || sameRemote(channel.remoteAddress(), targetAddress)) {
                return Optional.of(channel);
            }
        }

        return discovered.stream().filter(Channel::isOpen).findFirst();
    }

    private void scan(Object root, int depth, Set<Object> visited, List<Channel> discovered) {
        if (root == null || depth < 0) {
            return;
        }
        if (root instanceof Channel channel) {
            discovered.add(channel);
            return;
        }
        if (!visited.add(root)) {
            return;
        }

        if (root instanceof Optional<?> optional) {
            optional.ifPresent(value -> scan(value, depth - 1, visited, discovered));
            return;
        }
        if (root instanceof Iterable<?> iterable) {
            for (Object value : iterable) {
                scan(value, depth - 1, visited, discovered);
            }
            return;
        }
        if (root instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                scan(value, depth - 1, visited, discovered);
            }
            return;
        }
        if (root.getClass().isArray()) {
            int length = Array.getLength(root);
            for (int index = 0; index < length; index++) {
                scan(Array.get(root, index), depth - 1, visited, discovered);
            }
            return;
        }
        if (isSimpleType(root.getClass())) {
            return;
        }

        for (Field field : getAllFields(root.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                continue;
            }
            try {
                field.setAccessible(true);
                scan(field.get(root), depth - 1, visited, discovered);
            } catch (Throwable ignored) {
                // This is diagnostic reflection over Velocity internals. Fail closed and continue.
            }
        }
    }

    private boolean sameRemote(SocketAddress actual, InetSocketAddress expected) {
        if (!(actual instanceof InetSocketAddress actualInet)) {
            return false;
        }
        return actualInet.getPort() == expected.getPort()
                && Objects.equals(actualInet.getHostString(), expected.getHostString());
    }

    private boolean isSimpleType(Class<?> type) {
        if (type.isEnum()) {
            return true;
        }
        String name = type.getName();
        return name.startsWith("java.lang.")
                || name.startsWith("java.time.")
                || name.startsWith("java.net.")
                || name.startsWith("java.util.UUID");
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            java.util.Collections.addAll(fields, declared);
            current = current.getSuperclass();
        }
        return fields;
    }
}
