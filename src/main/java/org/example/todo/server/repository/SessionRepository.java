package org.example.todo.server.repository;


import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.model.Session;

import java.sql.*;
import java.util.Optional;

public class SessionRepository {
    private final DataSourceProvider dsp;

    public SessionRepository(DataSourceProvider dsp) { this.dsp = dsp; }

    public void insert(Session s) throws SQLException {
        String sql = "INSERT INTO sessions(jti, user_id, expires_at, created_at) VALUES(?,?,?,?)";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getJti());
            ps.setString(2, s.getUserId());
            ps.setLong(3, s.getExpiresAt());
            ps.setLong(4, s.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public Optional<Session> findByJti(String jti) throws SQLException {
        String sql = "SELECT jti, user_id, expires_at, created_at FROM sessions WHERE jti = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jti);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public void deleteByJti(String jti) throws SQLException {
        String sql = "DELETE FROM sessions WHERE jti = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jti);
            ps.executeUpdate();
        }
    }

    public void deleteExpired(long now) throws SQLException {
        String sql = "DELETE FROM sessions WHERE expires_at < ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.executeUpdate();
        }
    }

    private Session map(ResultSet rs) throws SQLException {
        return new Session(
                rs.getString("jti"),
                rs.getString("user_id"),
                rs.getLong("expires_at"),
                rs.getLong("created_at")
        );
    }
}
