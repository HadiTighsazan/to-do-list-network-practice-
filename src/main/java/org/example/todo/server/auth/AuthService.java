package org.example.todo.server.auth;

import org.example.todo.server.repository.SessionRepository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

/**
 * Validates a token: signature, expiry, and jti presence in sessions (not revoked).
 */
public class AuthService {
    public static class AuthContext {
        public final String userId; public final String username; public final String jti;
        public AuthContext(String userId, String username, String jti) { this.userId = userId; this.username = username; this.jti = jti; }
    }

    private final JwtService jwt;
    private final SessionRepository sessions;

    public AuthService(JwtService jwt, SessionRepository sessions) {
        this.jwt = Objects.requireNonNull(jwt);
        this.sessions = Objects.requireNonNull(sessions);
    }

    public AuthContext authenticate(String token) throws SQLException {
        var claims = jwt.parseAndValidate(token);
        var opt = sessions.findByJti(claims.jti);
        if (opt.isEmpty()) throw new SecurityException("Token revoked or not found");
        long now = Instant.now().toEpochMilli();
        if (opt.get().getExpiresAt() < now) throw new SecurityException("Token expired");
        return new AuthContext(claims.userId, claims.username, claims.jti);
    }
}
