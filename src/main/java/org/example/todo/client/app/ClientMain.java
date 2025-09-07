package org.example.todo.client.app;

import org.example.todo.client.cli.CommandLoop;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = System.getProperty("host", "127.0.0.1");
        int port = Integer.getInteger("port", 5050);
        System.out.printf("Connecting to %s:%d ...%n", host, port);
        try (TcpClient tcp = new TcpClient(host, port); UdpListener udp = new UdpListener()) {
            ClientState state = new ClientState();
            Thread udpThread = new Thread(udp, "udp-listener");
            udpThread.setDaemon(true); udpThread.start();

            System.out.println("commands:\n  register <u> <p>\n  login <u> <p>\n  logout\n  create_board <name>\n  list_boards\n  add_user_to_board <boardId> <userId>\n  view_board <boardId>\n  subscribe_board [boardId]\n  unsubscribe_board [boardId]\n\n  add_task \"<title>\" \"<desc>\" <low|medium|high> [dueMillis]\n  list_tasks [by=createdAt|due|priority] [order=asc|desc] [status=...] [priority=...] [dueBefore=ms] [dueAfter=ms]\n  update_task_status <taskId> <todo|inProgress|done>\n  delete_task <taskId>\n  help\n  exit");

            new CommandLoop(tcp, state, udp).run();
        }
    }
}
