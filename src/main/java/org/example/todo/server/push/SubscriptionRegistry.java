package org.example.todo.server.push;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionRegistry {
    public static final class Subscriber {
        public final String connectionKey;
        public final String userId;
        public final InetAddress address;
        public final int port;
        public Subscriber(String connectionKey, String userId, InetAddress address, int port) {
            this.connectionKey = connectionKey; this.userId = userId; this.address = address; this.port = port;
        }
        @Override public int hashCode() { return Objects.hash(connectionKey, userId, address, port); }
        @Override public boolean equals(Object o) {
            if (this == o) return true; if (!(o instanceof Subscriber s)) return false;
            return port == s.port && Objects.equals(connectionKey, s.connectionKey) &&
                    Objects.equals(userId, s.userId) && Objects.equals(address, s.address);
        }
    }

    private final Map<String, Set<Subscriber>> byBoard = new ConcurrentHashMap<>();
    private final Map<String, Set<Subscriber>> byConn = new ConcurrentHashMap<>();

    public void subscribe(String boardId, Subscriber sub) {
        byBoard.computeIfAbsent(boardId, k -> ConcurrentHashMap.newKeySet()).add(sub);
        byConn.computeIfAbsent(sub.connectionKey, k -> ConcurrentHashMap.newKeySet()).add(sub);
    }

    public void unsubscribe(String boardId, Subscriber sub) {
        var set = byBoard.get(boardId);
        if (set != null) set.remove(sub);
        var cset = byConn.get(sub.connectionKey);
        if (cset != null) cset.remove(sub);
    }

    public void unsubscribeAllForConnection(String connectionKey) {
        byConn.remove(connectionKey);
        for (var e : byBoard.entrySet()) {
            e.getValue().removeIf(s -> connectionKey.equals(s.connectionKey));
        }
    }

    public Collection<Subscriber> subscribersForBoard(String boardId) {
        return byBoard.getOrDefault(boardId, Set.of());
    }
}
