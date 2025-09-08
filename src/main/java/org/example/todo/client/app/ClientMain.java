package org.example.todo.client.app;
import org.example.todo.client.api.ClientApi;
import org.example.todo.client.cli.CommandLoop;
import org.example.todo.client.gui.GuiMain;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class ClientMain {
    public static void main(String[] args) {
        String host = System.getProperty("host", "127.0.0.1");
        int port = Integer.getInteger("port", 5050);
        boolean useGui = Arrays.asList(args).contains("--gui");

        System.out.println("═══════════════════════════════════════════");
        System.out.println("     Todo List Management System");
        System.out.println("═══════════════════════════════════════════");
        System.out.printf("Connecting to %s:%d ...%n", host, port);

        // --- START OF CHANGES ---
        // REMOVED try-with-resources to manage connection lifecycle manually for GUI
        TcpClient tcp = null;
        UdpListener udp = null;
        try {
            tcp = new TcpClient(host, port);
            udp = new UdpListener();

            ClientState state = new ClientState();
            ClientApi api = new ClientApi(tcp, state);

            Thread udpThread = new Thread(udp, "udp-listener");
            udpThread.setDaemon(true);
            udpThread.start();

            System.out.println("✓ Connected successfully!");
            System.out.println("✓ UDP listener ready on port " + udp.getLocalPort());

            if (useGui) {
                System.out.println("Starting in GUI mode...");
                // Pass resources to the GUI to be managed and closed there
                GuiMain.start(api, state, tcp, udp);
            } else {
                // CLI mode manages its own lifecycle
                try {
                    new CommandLoop(api, state, udp).run();
                } finally {
                    System.out.println("\nShutting down...");
                    tcp.close();
                    udp.close();
                }
            }
            // --- END OF CHANGES ---
        } catch (Exception e) {
            System.err.println("Failed to connect or run client: " + e.getMessage());
            // Ensure resources are closed on startup failure
            try { if (tcp != null) tcp.close(); } catch (IOException ignored) {}
            try { if (udp != null) udp.close(); } catch (Exception ignored) {}
            System.exit(1);
        }
    }
}