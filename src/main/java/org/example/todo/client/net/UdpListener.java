package org.example.todo.client.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/** Background UDP listener that prints push notifications. */
public class UdpListener implements Runnable, Closeable {
    private static final Logger log = LoggerFactory.getLogger(UdpListener.class);

    private final DatagramSocket socket;
    private volatile boolean running = true;

    public UdpListener() throws SocketException {
        this.socket = new DatagramSocket(0); // bind to ephemeral port
        this.socket.setSoTimeout(0); // blocking
        log.info("UDP listener bound on {}", socket.getLocalPort());
    }

    public int getLocalPort() { return socket.getLocalPort(); }

    @Override
    public void run() {
        byte[] buf = new byte[65535];
        while (running && !socket.isClosed()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                String json = new String(p.getData(), p.getOffset(), p.getLength(), StandardCharsets.UTF_8);
                handle(json);
            } catch (Exception e) {
                if (running) log.warn("UDP receive error: {}", e.toString());
            }
        }
    }

    private void handle(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("type") || !"push".equals(obj.get("type").getAsString())) return;
            String event = obj.has("event") ? obj.get("event").getAsString() : "?";
            String boardId = obj.has("boardId") ? obj.get("boardId").getAsString() : "?";
            System.out.println("\n[PUSH] " + event + " @board=" + boardId);
            if (obj.has("task")) {
                var t = obj.getAsJsonObject("task");
                String dueStr = (t.has("dueDate") && !t.get("dueDate").isJsonNull()) ? String.valueOf(t.get("dueDate").getAsLong()) : "-";
                System.out.printf("  task: %s | %s | %s | pr=%s | due=%s%n",
                        t.get("id").getAsString(),
                        t.get("title").getAsString(),
                        t.get("status").getAsString(),
                        t.get("priority").getAsString(),
                        dueStr);
            }
            if (obj.has("member")) {
                var m = obj.getAsJsonObject("member");
                System.out.printf("  member: %s (%s) role=%s%n",
                        m.get("userId").getAsString(),
                        m.get("username").getAsString(),
                        m.get("role").getAsString());
            }
            System.out.print("todo> "); // re-prompt (best-effort)
        } catch (Exception ignore) {
            System.out.println("\n[PUSH] " + json);
            System.out.print("todo> ");
        }
    }

    @Override
    public void close() {
        running = false;
        socket.close();
    }
}
