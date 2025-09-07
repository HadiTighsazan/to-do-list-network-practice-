package org.example.todo.server.service;

import org.example.todo.server.app.ServerConfig;
import org.example.todo.server.auth.JwtService;
import org.example.todo.server.auth.PasswordHasher;
import org.example.todo.server.core.AppException;
import org.example.todo.server.model.Session;
import org.example.todo.server.model.User;
import org.example.todo.server.repository.SessionRepository;
import org.example.todo.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Step 1: Business logic for register/login/logout.
 */
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public record LoginResult(String token, User user, long expiresAt) {}

    private final ServerConfig config;
    private final PasswordHasher hasher;
    private final JwtService jwt;
    private final UserRepository users;
    private final SessionRepository sessions;

    public UserService(ServerConfig config, PasswordHasher hasher, JwtService jwt,
                       UserRepository users, SessionRepository sessions) {
        this.config = Objects.requireNonNull(config);
        this.hasher = Objects.requireNonNull(hasher);
        this.jwt = Objects.requireNonNull(jwt);
        this.users = Objects.requireNonNull(users);
        this.sessions = Objects.requireNonNull(sessions);
    }

    public User register(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank()) throw AppException.validation("نام کاربری خالی است");
        if (password == null || password.length < 4) throw AppException.validation("طول رمز عبور کم است");
        Optional<User> existing = users.findByUsername(username);
        if (existing.isPresent()) throw AppException.conflict("نام کاربری تکراری است");

        byte[] salt = hasher.generateSalt();
        byte[] hash = hasher.hash(password, salt, hasher.defaultIterations());
        long now = Instant.now().toEpochMilli();
        User u = new User(UUID.randomUUID().toString(), now, username,
                PasswordHasher.toBase64(hash), PasswordHasher.toBase64(salt));
        users.insert(u);
        log.info("User registered: {}", username);
        return u;
    }

    public LoginResult login(String username, char[] password) throws SQLException {
        Optional<User> opt = users.findByUsername(username);
        if (opt.isEmpty()) throw AppException.auth("کاربری با این نام یافت نشد");
        User u = opt.get();
        byte[] salt = PasswordHasher.fromBase64(u.getPasswordSalt());
        byte[] expected = PasswordHasher.fromBase64(u.getPasswordHash());
        boolean ok = hasher.verify(password, salt, expected, hasher.defaultIterations());
        if (!ok) throw AppException.auth("رمز عبور اشتباه است");

        long now = Instant.now().toEpochMilli();
        long exp = now + config.getTokenTtl().toMillis();
        String token = jwt.createToken(u.getId(), u.getUsername(), now, exp);
        String jti = jwt.getJti(token);
        sessions.insert(new Session(jti, u.getId(), exp, now));
        return new LoginResult(token, u, exp);
    }

    public void logout(String token) throws SQLException {
        String jti = jwt.getJti(token);
        sessions.deleteByJti(jti);
    }
}
