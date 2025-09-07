package org.example.todo.server.app;

import org.example.todo.server.auth.AuthService;
import org.example.todo.server.auth.JwtService;
import org.example.todo.server.auth.PasswordHasher;
import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.db.SchemaMigrator;
import org.example.todo.server.net.tcp.TcpServer;
import org.example.todo.server.push.PushService;
import org.example.todo.server.push.SubscriptionRegistry;
import org.example.todo.server.push.UdpPushServer;
import org.example.todo.server.repository.*;
import org.example.todo.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {
    private static final Logger log = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) throws Exception {
        ServerConfig cfg = ServerConfig.loadFromEnvOrDefaults();
        log.info("Booting with TCP:{} UDP:{} DB:{}", cfg.getTcpPort(), cfg.getUdpPort(), cfg.getJdbcUrl());

        DataSourceProvider dsp = new DataSourceProvider(cfg);
        new SchemaMigrator(dsp).ensureSchema();

        PasswordHasher hasher = new PasswordHasher();
        JwtService jwt = new JwtService(cfg.getJwtSecret());
        UserRepository userRepo = new UserRepository(dsp);
        SessionRepository sessRepo = new SessionRepository(dsp);
        UserService userService = new UserService(cfg, hasher, jwt, userRepo, sessRepo);
        AuthService authService = new AuthService(jwt, sessRepo);

        BoardRepository boardRepo = new BoardRepository(dsp);
        MembershipRepository membershipRepo = new MembershipRepository(dsp);

        // Push infra (UDP fanout)
        SubscriptionRegistry registry = new SubscriptionRegistry();
        UdpPushServer udp = new UdpPushServer(cfg.getUdpPort());
        PushService push = new PushService(registry, udp);

        BoardService boardService = new BoardService(boardRepo, membershipRepo, userRepo, dsp, push);

        TaskRepository taskRepo = new TaskRepository(dsp);
        TaskService taskService = new TaskService(taskRepo, boardService, push);

        TcpServer tcpServer = new TcpServer(cfg.getTcpPort(), userService, authService, boardService, taskService, push);
        tcpServer.start();

        log.info("Server TCP started. Press Ctrl+C to exit.");
    }
}
