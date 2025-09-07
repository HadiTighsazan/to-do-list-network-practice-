package org.example.todo.server.repository;

import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.*;
import java.util.Optional;

public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final DataSourceProvider dsp;

    public UserRepository(DataSourceProvider dsp) {
        this.dsp = dsp;
    }

    public void insert(User u) throws SQLException {
        String sql = "INSERT INTO users(id, username, password_hash, password_salt, created_at) VALUES(?,?,?,?,?)";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getPasswordSalt());
            ps.setLong(5, u.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, password_salt, created_at FROM users WHERE username = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT id, username, password_hash, password_salt, created_at FROM users WHERE id = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getLong("created_at"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt")
        );
    }
}
