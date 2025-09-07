package org.example.todo.server.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/** Simple UDP sender bound to a fixed port. */
public class UdpPushServer implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(UdpPushServer.class);

    private final int port;
    private DatagramSocket socket;

    public UdpPushServer(int port) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
        log.info("UDP push server bound on {}", port);
    }

    public synchronized void sendJson(InetAddress addr, int clientPort, String json) {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        // clamp packet size (safety)
        if (data.length > 60_000) {
            log.warn("Push too large: {} bytes. Dropping.", data.length);
            return;
        }
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, clientPort);
            socket.send(packet);
        } catch (Exception e) {
            log.warn("UDP send failed to {}:{} - {}", addr, clientPort, e.toString());
        }
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
