package org.example.todo.server.repository;

import org.example.todo.server.model.BoardMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.todo.server.db.DataSourceProvider;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MembershipRepository {
    private static final Logger log = LoggerFactory.getLogger(MembershipRepository.class);
    private final DataSourceProvider dsp;

    public MembershipRepository(DataSourceProvider dsp) { this.dsp = dsp; }

    public void addMember(String boardId, String userId, String role, long createdAt) throws SQLException {
        String sql = "INSERT OR IGNORE INTO board_members(board_id, user_id, role, created_at) VALUES(?,?,?,?)";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardId);
            ps.setString(2, userId);
            ps.setString(3, role);
            ps.setLong(4, createdAt);
            ps.executeUpdate();
        }
    }

    public Optional<String> getRole(String boardId, String userId) throws SQLException {
        String sql = "SELECT role FROM board_members WHERE board_id=? AND user_id=?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardId); ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString(1));
                return Optional.empty();
            }
        }
    }

    public List<BoardMember> listMembers(String boardId) throws SQLException {
        String sql = "SELECT board_id, user_id, role, created_at FROM board_members WHERE board_id=? ORDER BY created_at ASC";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BoardMember> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<MemberWithUsername> listMembersWithUsernames(String boardId) throws SQLException {
        String sql = "SELECT m.board_id, m.user_id, u.username, m.role, m.created_at " +
                "FROM board_members m JOIN users u ON m.user_id=u.id WHERE m.board_id=? ORDER BY m.created_at ASC";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, boardId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MemberWithUsername> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new MemberWithUsername(
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5)
                    ));
                }
                return out;
            }
        }
    }

    public record MemberWithUsername(String boardId, String userId, String username, String role, long createdAt) {}

    private BoardMember map(ResultSet rs) throws SQLException {
        return new BoardMember(
                rs.getString("board_id"),
                rs.getString("user_id"),
                rs.getString("role"),
                rs.getLong("created_at")
        );
    }
}
