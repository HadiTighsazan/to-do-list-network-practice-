package org.example.todo.server.net.tcp;

import org.example.todo.server.auth.AuthService;
import org.example.todo.server.push.PushService;
import org.example.todo.server.service.BoardService;
import org.example.todo.server.service.TaskService;
import org.example.todo.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Thread-per-connection TCP server with TaskService+Push injection (Phase 5). */
public class TcpServer implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final ExecutorService pool;

    private final UserService userService;
    private final AuthService authService;
    private final BoardService boardService;
    private final TaskService taskService;
    private final PushService push;

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public TcpServer(int port, UserService userService, AuthService authService, BoardService boardService, TaskService taskService, PushService push) {
        this.port = port;
        this.userService = Objects.requireNonNull(userService);
        this.authService = Objects.requireNonNull(authService);
        this.boardService = Objects.requireNonNull(boardService);
        this.taskService = Objects.requireNonNull(taskService);
        this.push = Objects.requireNonNull(push);
        this.pool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log.info("TCP server listening on {}", port);
        new Thread(() -> {
            while (running) {
                try {
                    Socket s = serverSocket.accept();
                    s.setTcpNoDelay(true);
                    pool.submit(new TcpClientHandler(s, userService, authService, boardService, taskService, push));
                } catch (IOException e) {
                    if (running) log.error("Accept failed", e);
                }
            }
        }, "tcp-acceptor").start();
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        pool.shutdown();
        try { pool.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }
}
