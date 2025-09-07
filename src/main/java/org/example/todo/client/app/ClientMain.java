package org.example.todo.client.app;
import org.example.todo.client.api.ClientApi;
import org.example.todo.client.cli.CommandLoop;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

import java.util.Arrays;

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
                // We will implement this in the next step
                // org.example.todo.client.gui.GuiMain.start(api, state, udp);
                System.out.println("GUI mode requested. (Implementation pending)");
                System.out.println("Please run without --gui to use the command-line interface.");
                // For now, we just exit if GUI is requested.
                System.exit(0);
            } else {
                new CommandLoop(api, state, udp).run();
            }

            System.out.println("\nShutting down...");
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Goodbye!");
    }
}