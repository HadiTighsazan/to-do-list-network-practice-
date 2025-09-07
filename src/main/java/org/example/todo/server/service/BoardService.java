package org.example.todo.server.service;

import org.example.todo.server.core.AppException;
import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.model.Board;
import org.example.todo.server.repository.BoardRepository;
import org.example.todo.server.repository.MembershipRepository;
import org.example.todo.server.repository.UserRepository;
import org.example.todo.server.push.PushService;
import org.example.todo.server.protocol.BoardMemberView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class BoardService {
    private final BoardRepository boards;
    private final MembershipRepository members;
    private final UserRepository users;
    private final DataSourceProvider dsp;
    private final PushService push;

    public BoardService(BoardRepository boards, MembershipRepository members, UserRepository users, DataSourceProvider dsp, PushService push) {
        this.boards = boards; this.members = members; this.users = users; this.dsp = dsp; this.push = push;
    }

    public Board createBoard(String ownerId, String name) throws SQLException {
        if (name == null || name.isBlank()) throw AppException.validation("نام بورد خالی است");
        long now = Instant.now().toEpochMilli();
        Board b = new Board(UUID.randomUUID().toString(), now, name, ownerId);
        try (Connection c = dsp.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "INSERT INTO boards(id,name,owner_id,created_at) VALUES(?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, b.getId()); ps.setString(2, b.getName()); ps.setString(3, b.getOwnerId()); ps.setLong(4, b.getCreatedAt());
                    ps.executeUpdate();
                }
                String sql2 = "INSERT OR IGNORE INTO board_members(board_id,user_id,role,created_at) VALUES(?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql2)) {
                    ps.setString(1, b.getId()); ps.setString(2, ownerId); ps.setString(3, "OWNER"); ps.setLong(4, now);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
        return b;
    }

    public static class BoardView {
        public final String id; public final String name; public final String ownerId; public final long createdAt; public final String role;
        public BoardView(String id, String name, String ownerId, long createdAt, String role) {
            this.id=id; this.name=name; this.ownerId=ownerId; this.createdAt=createdAt; this.role=role; }
    }

    public List<BoardView> listBoardsForUser(String userId) throws SQLException {
        String sql = "SELECT id, name, owner_id, created_at, role FROM (" +
                "SELECT b.id, b.name, b.owner_id, b.created_at, 'OWNER' AS role FROM boards b WHERE b.owner_id = ? " +
                "UNION " +
                "SELECT b.id, b.name, b.owner_id, b.created_at, m.role FROM boards b JOIN board_members m ON b.id=m.board_id WHERE m.user_id = ?" +
                ") t ORDER BY created_at ASC";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId); ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, BoardView> map = new LinkedHashMap<>();
                while (rs.next()) {
                    String id = rs.getString(1);
                    String role = rs.getString(5);
                    map.put(id, new BoardView(id, rs.getString(2), rs.getString(3), rs.getLong(4), role));
                }
                return new ArrayList<>(map.values());
            }
        }
    }

    public void addMember(String requesterId, String boardId, String targetUserId) throws SQLException {
        Board b = boards.findById(boardId).orElseThrow(() -> AppException.validation("بورد مورد نظر وجود ندارد"));
        if (!b.getOwnerId().equals(requesterId)) throw AppException.auth("دسترسی غیرمجاز: فقط مالک می‌تواند عضو اضافه کند");
        var target = users.findById(targetUserId).orElseThrow(() -> AppException.validation("کاربر هدف یافت نشد"));
        long now = Instant.now().toEpochMilli();
        members.addMember(boardId, targetUserId, "MEMBER", now);
        if (push != null) {
            push.pushMemberAdded(requesterId, boardId, new BoardMemberView(target.getId(), target.getUsername(), "MEMBER", now));
        }
    }

    public Optional<String> checkAccessRole(String userId, String boardId) throws SQLException {
        Board b = boards.findById(boardId).orElse(null);
        if (b == null) return Optional.empty();
        if (b.getOwnerId().equals(userId)) return Optional.of("OWNER");
        return members.getRole(boardId, userId);
    }

    public List<MemberItem> listMembersWithUsername(String boardId) throws SQLException {
        return members.listMembersWithUsernames(boardId).stream()
                .map(m -> new MemberItem(m.userId(), m.username(), m.role(), m.createdAt()))
                .collect(Collectors.toList());
    }

    public static class MemberItem {
        public final String userId; public final String username; public final String role; public final long createdAt;
        public MemberItem(String userId, String username, String role, long createdAt) {
            this.userId=userId; this.username=username; this.role=role; this.createdAt=createdAt; }
    }
}
