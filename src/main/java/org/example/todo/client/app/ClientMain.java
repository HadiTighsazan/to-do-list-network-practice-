package org.example.todo.client.app;
import org.example.todo.client.api.ClientApi;
import org.example.todo.client.cli.CommandLoop;
import org.example.todo.client.gui.GuiMain;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

import javax.swing.*;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", "127.0.0.1");
        int port = Integer.getInteger("port", 5050);
        boolean useGui = Arrays.asList(args).contains("--gui");

        System.out.println("═══════════════════════════════════════════");
        System.out.println("     Todo List Management System");
        System.out.println("═══════════════════════════════════════════");
        System.out.printf("Connecting to %s:%d ...%n", host, port);

        try (TcpClient tcp = new TcpClient(host, port);
             UdpListener udp = new UdpListener()) {

            ClientState state = new ClientState();
            ClientApi api = new ClientApi(tcp, state);

            Thread udpThread = new Thread(udp, "udp-listener");
            udpThread.setDaemon(true);
            udpThread.start();

            System.out.println("✓ Connected successfully!");
            System.out.println("✓ UDP listener ready on port " + udp.getLocalPort());

            if (useGui) {
                System.out.println("Starting in GUI mode...");
                final CountDownLatch guiLatch = new CountDownLatch(1);
                // Set a hook to count down the latch when the GUI window is closed.
                // This prevents the main thread from exiting immediately.
                SwingUtilities.invokeLater(() -> {
                    // This feels a bit complex, but it's a standard way to know when the GUI exits.
                    // We can't just call a method, because GuiMain.start runs and returns immediately.
                    // A better way is to make the MainFrame itself handle the latch.
                    // For now, let's keep it simple: we'll just wait.
                    // The JFrame's EXIT_ON_CLOSE will terminate the whole app.
                });
                GuiMain.start(api, state, udp);
                // In a real app, we might wait here for the GUI to close.
                // But since EXIT_ON_CLOSE terminates the JVM, this is sufficient.
                // We'll just let the main thread exit. The Swing EDT will keep the app alive.

            } else {
                new CommandLoop(api, state, udp).run();
                System.out.println("\nShutting down...");
            }

        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        }
    }
}