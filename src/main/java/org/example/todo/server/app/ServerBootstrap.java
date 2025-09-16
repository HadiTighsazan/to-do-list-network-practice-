package org.example.todo.server.app;

import org.example.todo.server.auth.JwtService;
import org.example.todo.server.auth.PasswordHasher;
import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.db.SchemaMigrator;
import org.example.todo.server.repository.SessionRepository;
import org.example.todo.server.repository.UserRepository;
import org.example.todo.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public final class ServerBootstrap {
    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    public static void main(String[] args) throws Exception {
        ServerConfig cfg = ServerConfig.loadFromEnvOrDefaults();
        log.info("Starting bootstrap. TCP:{} UDP:{} DB:{}", cfg.getTcpPort(), cfg.getUdpPort(), cfg.getJdbcUrl());

        DataSourceProvider dsp = new DataSourceProvider(cfg);
        new SchemaMigrator(dsp).ensureSchema();

        PasswordHasher hasher = new PasswordHasher();
        JwtService jwt = new JwtService(cfg.getJwtSecret());
        UserRepository userRepo = new UserRepository(dsp);
        SessionRepository sessRepo = new SessionRepository(dsp);
        UserService userService = new UserService(cfg, hasher, jwt, userRepo, sessRepo);

        log.info("Bootstrap complete. Core services initialized. (Networking comes in Step 2)");
    }
}
