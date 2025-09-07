package org.example.todo.client.app;
import org.example.todo.client.cli.CommandLoop;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", "127.0.0.1");
        int port = Integer.getInteger("port", 5050);

        System.out.println("═══════════════════════════════════════════");
        System.out.println("     Todo List Management System");
        System.out.println("═══════════════════════════════════════════");
        System.out.printf("Connecting to %s:%d ...%n", host, port);

        try (TcpClient tcp = new TcpClient(host, port);
             UdpListener udp = new UdpListener()) {

            ClientState state = new ClientState();

            // Start UDP listener thread - CRITICAL FIX
            Thread udpThread = new Thread(udp, "udp-listener");
            udpThread.setDaemon(true);
            udpThread.start();

            System.out.println("✓ Connected successfully!");
            System.out.println("✓ UDP listener ready on port " + udp.getLocalPort());
            System.out.println("\nAvailable Commands:");
            System.out.println("───────────────────────────────────────────");
            System.out.println("  User Management:");
            System.out.println("    register <username> <password>");
            System.out.println("    login <username> <password>");
            System.out.println("    logout");
            System.out.println("\n  Board Management:");
            System.out.println("    create_board <name>");
            System.out.println("    list_boards");
            System.out.println("    add_user_to_board <boardId> <userId>");
            System.out.println("    view_board <boardId>");
            System.out.println("\n  Task Management (requires board context):");
            System.out.println("    add_task \"<title>\" \"<description>\" <low|medium|high> [dueMillis]");
            System.out.println("    list_tasks [filters...]");
            System.out.println("    update_task_status <taskId> <todo|inProgress|done>");
            System.out.println("    delete_task <taskId>");
            System.out.println("\n  Push Notifications:");
            System.out.println("    subscribe_board [boardId]");
            System.out.println("    unsubscribe_board [boardId]");
            System.out.println("\n  Other:");
            System.out.println("    help");
            System.out.println("    exit");
            System.out.println("───────────────────────────────────────────");

            new CommandLoop(tcp, state, udp).run();

            System.out.println("\nShutting down...");
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Goodbye!");
    }
}
