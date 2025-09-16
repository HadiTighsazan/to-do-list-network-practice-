package org.example.todo.server.app;

import java.nio.charset.StandardCharsets;
import java.time.Duration;


public final class ServerConfig {
    private final int tcpPort;
    private final int udpPort;
    private final String jdbcUrl;
    private final byte[] jwtSecret;
    private final Duration tokenTtl;

    private ServerConfig(int tcpPort, int udpPort, String jdbcUrl, byte[] jwtSecret, Duration tokenTtl) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.jdbcUrl = jdbcUrl;
        this.jwtSecret = jwtSecret;
        this.tokenTtl = tokenTtl;
    }

    public static ServerConfig loadFromEnvOrDefaults() {
        int tcp = parseIntOrDefault(System.getenv("TODO_TCP_PORT"), 5050);
        int udp = parseIntOrDefault(System.getenv("TODO_UDP_PORT"), 5051);
        String dbFile = System.getenv("TODO_DB_FILE");
        if (dbFile == null || dbFile.isBlank()) dbFile = "data/todo.db";
        String jdbc = "jdbc:sqlite:" + dbFile;
        String secret = System.getenv("TODO_JWT_SECRET");
        if (secret == null || secret.isBlank()) secret = "dev-secret-change-me";
        String ttl = System.getenv("TODO_TOKEN_TTL_HOURS");
        Duration tokenTtl = Duration.ofHours(ttl != null ? parseIntOrDefault(ttl, 24) : 24);
        return new ServerConfig(tcp, udp, jdbc, secret.getBytes(StandardCharsets.UTF_8), tokenTtl);
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public int getTcpPort() { return tcpPort; }
    public int getUdpPort() { return udpPort; }
    public String getJdbcUrl() { return jdbcUrl; }
    public byte[] getJwtSecret() { return jwtSecret; }
    public Duration getTokenTtl() { return tokenTtl; }
}
