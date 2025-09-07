package org.example.todo.server.repository;

import org.example.todo.server.db.DataSourceProvider;
import org.example.todo.server.model.Task;

import java.sql.*;
import java.util.*;

public class TaskRepository {
    private final DataSourceProvider dsp;

    public TaskRepository(DataSourceProvider dsp) { this.dsp = dsp; }

    public void insert(Task t) throws SQLException {
        String sql = "INSERT INTO tasks(id, board_id, title, description, status, priority, due_date, created_at) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getBoardId());
            ps.setString(3, t.getTitle());
            ps.setString(4, t.getDescription());
            ps.setString(5, t.getStatus());
            ps.setString(6, t.getPriority());
            if (t.getDueDate() == null) ps.setNull(7, Types.BIGINT); else ps.setLong(7, t.getDueDate());
            ps.setLong(8, t.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public Optional<Task> findByIdAndBoard(String id, String boardId) throws SQLException {
        String sql = "SELECT id, board_id, title, description, status, priority, due_date, created_at FROM tasks WHERE id=? AND board_id=?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, boardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public List<Task> listByBoard(String boardId,
                                  Set<String> statuses,
                                  Set<String> priorities,
                                  Long dueBefore,
                                  Long dueAfter,
                                  String sortBy,
                                  String order) throws SQLException {
        StringBuilder sb = new StringBuilder("SELECT id, board_id, title, description, status, priority, due_date, created_at FROM tasks WHERE board_id = ?");
        List<Object> params = new ArrayList<>(); params.add(boardId);
        if (statuses != null && !statuses.isEmpty()) {
            sb.append(" AND status IN ("); appendPlaceholders(sb, statuses.size()); sb.append(")"); params.addAll(statuses);
        }
        if (priorities != null && !priorities.isEmpty()) {
            sb.append(" AND priority IN ("); appendPlaceholders(sb, priorities.size()); sb.append(")"); params.addAll(priorities);
        }
        if (dueBefore != null) { sb.append(" AND due_date IS NOT NULL AND due_date < ?"); params.add(dueBefore); }
        if (dueAfter != null)  { sb.append(" AND due_date IS NOT NULL AND due_date >= ?"); params.add(dueAfter); }

        String ord = (order != null && order.equalsIgnoreCase("desc")) ? "DESC" : "ASC";
        String by;
        if ("due".equalsIgnoreCase(sortBy)) by = "due_date";
        else if ("priority".equalsIgnoreCase(sortBy)) by = null; // handled by CASE
        else by = "created_at";

        sb.append(" ORDER BY ");
        if (by != null) {
            sb.append(by).append(" ").append(ord);
        } else {
            // priority ordering LOW < MEDIUM < HIGH
            sb.append("CASE priority WHEN 'LOW' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'HIGH' THEN 3 END ").append(ord);
        }
        sb.append(", created_at ASC");

        String sql = sb.toString();
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (Object p : params) {
                if (p instanceof String s) ps.setString(i++, s);
                else if (p instanceof Long l) ps.setLong(i++, l);
                else throw new IllegalArgumentException("Unsupported param type: " + p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public void updateStatus(String taskId, String boardId, String newStatus) throws SQLException {
        String sql = "UPDATE tasks SET status=? WHERE id=? AND board_id=?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus); ps.setString(2, taskId); ps.setString(3, boardId);
            ps.executeUpdate();
        }
    }

    public void delete(String taskId, String boardId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id=? AND board_id=?";
        try (Connection c = dsp.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, taskId); ps.setString(2, boardId);
            ps.executeUpdate();
        }
    }

    private static void appendPlaceholders(StringBuilder sb, int n) {
        for (int i=0;i<n;i++) { if (i>0) sb.append(','); sb.append('?'); }
    }

    private Task map(ResultSet rs) throws SQLException {
        Long due = rs.getObject("due_date") == null ? null : rs.getLong("due_date");
        return new Task(
                rs.getString("id"),
                rs.getLong("created_at"),
                rs.getString("board_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("priority"),
                due
        );
    }
}
