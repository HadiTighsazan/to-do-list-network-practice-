package org.example.todo.server.repository;

import org.example.todo.server.model.Board;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.todo.server.db.DataSourceProvider;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoardRepository {
    private static final Logger log = LoggerFactory.getLogger(BoardRepository.class);
    private final DataSourceProvider dsp;

    public BoardRepository(DataSourceProvider dsp) { this.dsp = dsp; }

    public void insert(Board b) throws SQLException {
        String sql = "INSERT INTO boards(id, name, owner_id, created_at) VALUES(?,?,?,?)";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getName());
            ps.setString(3, b.getOwnerId());
            ps.setLong(4, b.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public Optional<Board> findById(String id) throws SQLException {
        String sql = "SELECT id, name, owner_id, created_at FROM boards WHERE id = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public List<Board> listOwnedBy(String userId) throws SQLException {
        String sql = "SELECT id, name, owner_id, created_at FROM boards WHERE owner_id = ? ORDER BY created_at ASC";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Board> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM boards WHERE id = ?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
    private Board map(ResultSet rs) throws SQLException {
        return new Board(
                rs.getString("id"),
                rs.getLong("created_at"),
                rs.getString("name"),
                rs.getString("owner_id")
        );
    }
}
