package org.example.todo.client.net;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class UdpListener implements Runnable, Closeable {
    private final DatagramSocket socket;
    private volatile boolean running = true;

    public UdpListener() throws SocketException {
        this.socket = new DatagramSocket(0);
        this.socket.setSoTimeout(1000);
        System.out.println("[UDP] Listener bound on port " + socket.getLocalPort());
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public void run() {
        byte[] buf = new byte[65535];
        System.out.println("[UDP] Listener thread started");

        while (running && !socket.isClosed()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                String json = new String(p.getData(), p.getOffset(), p.getLength(), StandardCharsets.UTF_8);
                handle(json);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (Exception e) {
                if (running && !socket.isClosed()) {
                    System.err.println("[UDP] Receive error: " + e.getMessage());
                }
            }
        }
        System.out.println("[UDP] Listener thread stopped");
    }

    private void handle(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("type") || !"push".equals(obj.get("type").getAsString())) {
                return;
            }

            String event = obj.has("event") ? obj.get("event").getAsString() : "?";
            String boardId = obj.has("boardId") ? obj.get("boardId").getAsString() : "?";

            System.out.println("\n════════════════════════════════════════");
            System.out.println("[PUSH NOTIFICATION] Event: " + event);
            System.out.println("Board: " + boardId);

            if (obj.has("actorUserId")) {
                System.out.println("By User: " + obj.get("actorUserId").getAsString());
            }

            if (obj.has("task")) {
                var t = obj.getAsJsonObject("task");
                System.out.println("Task Details:");
                if (t.has("id")) System.out.println("  ID: " + t.get("id").getAsString());
                if (t.has("title")) System.out.println("  Title: " + t.get("title").getAsString());
                if (t.has("status")) System.out.println("  Status: " + t.get("status").getAsString());
                if (t.has("priority")) System.out.println("  Priority: " + t.get("priority").getAsString());

                String dueStr = (t.has("dueDate") && !t.get("dueDate").isJsonNull())
                        ? new java.util.Date(t.get("dueDate").getAsLong()).toString()
                        : "Not set";
                System.out.println("  Due: " + dueStr);
            }

            if (obj.has("member")) {
                var m = obj.getAsJsonObject("member");
                System.out.printf("Member Details:\n  %s (%s) - Role: %s%n",
                        m.get("userId").getAsString(),
                        m.get("username").getAsString(),
                        m.get("role").getAsString());
            }

            System.out.println("════════════════════════════════════════");
            System.out.print("todo> ");

        } catch (Exception e) {
            System.out.println("\n[UDP PUSH - Raw]: " + json);
            System.out.print("todo> ");
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (Exception ignore) {}
    }
}
