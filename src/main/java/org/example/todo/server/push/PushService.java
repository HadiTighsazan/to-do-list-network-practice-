package org.example.todo.server.push;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.example.todo.server.protocol.BoardMemberView;
import org.example.todo.server.protocol.TaskView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Collection;

/** High-level push API used by services. */
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final SubscriptionRegistry registry;
    private final UdpPushServer udp;

    public PushService(SubscriptionRegistry registry, UdpPushServer udp) {
        this.registry = registry; this.udp = udp;
    }

    public void subscribe(String connectionKey, String boardId, String userId, InetAddress addr, int udpPort) {
        var sub = new SubscriptionRegistry.Subscriber(connectionKey, userId, addr, udpPort);
        registry.subscribe(boardId, sub);
        log.info("Subscribed user {} to board {} on {}:{}", userId, boardId, addr.getHostAddress(), udpPort);
    }

    public void unsubscribe(String connectionKey, String boardId, String userId, InetAddress addr, int udpPort) {
        var sub = new SubscriptionRegistry.Subscriber(connectionKey, userId, addr, udpPort);
        registry.unsubscribe(boardId, sub);
        log.info("Unsubscribed user {} from board {} on {}:{}", userId, boardId, addr.getHostAddress(), udpPort);
    }

    public void clearConnection(String connectionKey) {
        registry.unsubscribeAllForConnection(connectionKey);
    }

    public void pushTaskAdded(String actorUserId, String boardId, TaskView tv) {
        JsonObject p = base("task_added", actorUserId, boardId);
        p.add("task", GSON.toJsonTree(tv));
        fanout(boardId, p);
    }

    public void pushTaskUpdated(String actorUserId, String boardId, TaskView tv) {
        JsonObject p = base("task_updated", actorUserId, boardId);
        p.add("task", GSON.toJsonTree(tv));
        fanout(boardId, p);
    }

    public void pushTaskDeleted(String actorUserId, String boardId, String taskId) {
        JsonObject p = base("task_deleted", actorUserId, boardId);
        JsonObject t = new JsonObject(); t.addProperty("id", taskId); t.addProperty("boardId", boardId);
        p.add("task", t);
        fanout(boardId, p);
    }

    public void pushMemberAdded(String actorUserId, String boardId, BoardMemberView mv) {
        JsonObject p = base("member_added", actorUserId, boardId);
        p.add("member", GSON.toJsonTree(mv));
        fanout(boardId, p);
    }

    private static JsonObject base(String event, String actorUserId, String boardId) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "push");
        p.addProperty("event", event);
        p.addProperty("boardId", boardId);
        p.addProperty("actorUserId", actorUserId);
        p.addProperty("ts", Instant.now().toEpochMilli());
        return p;
    }

    private void fanout(String boardId, JsonObject payload) {
        Collection<SubscriptionRegistry.Subscriber> subs = registry.subscribersForBoard(boardId);
        if (subs.isEmpty()) return;
        String json = GSON.toJson(payload);
        for (var s : subs) {
            udp.sendJson(s.address, s.port, json);
        }
    }
}
