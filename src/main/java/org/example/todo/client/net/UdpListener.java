package org.example.todo.client.net;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UdpListener implements Runnable, Closeable {
    private final DatagramSocket socket;
    private volatile boolean running = true;
    private final List<PushNotificationListener> listeners = new CopyOnWriteArrayList<>();


    public UdpListener() throws SocketException {
        this.socket = new DatagramSocket(0);
        this.socket.setSoTimeout(1000);
        System.out.println("[UDP] Listener bound on port " + socket.getLocalPort());
    }

    public void addListener(PushNotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PushNotificationListener listener) {
        listeners.remove(listener);
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
            // Notify all registered listeners
            for (PushNotificationListener listener : listeners) {
                listener.onPushNotification(obj);
            }

        } catch (Exception e) {
            System.err.println("[UDP] Error parsing push notification: " + json);
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