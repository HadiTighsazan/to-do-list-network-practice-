package org.example.todo.server.service;

import org.example.todo.server.core.AppException;
import org.example.todo.server.model.Task;
import org.example.todo.server.repository.TaskRepository;
import org.example.todo.server.push.PushService;
import org.example.todo.server.protocol.TaskView;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class TaskService {
    private final TaskRepository tasks;
    private final BoardService boards;
    private final PushService push;

    public TaskService(TaskRepository tasks, BoardService boards, PushService push) {
        this.tasks = tasks; this.boards = boards; this.push = push;
    }

    public Task addTask(String userId, String boardId, String title, String description, String priority, Long dueDate) throws SQLException {
        ensureAccess(userId, boardId);
        if (title == null || title.isBlank()) throw AppException.validation("عنوان خالی است");
        String pr = normalizePriority(priority);
        long now = Instant.now().toEpochMilli();
        Task t = new Task(UUID.randomUUID().toString(), now, boardId, title, description, "TODO", pr, dueDate);
        tasks.insert(t);
        if (push != null) push.pushTaskAdded(userId, boardId, toView(t));
        return t;
    }

    public List<Task> listTasks(String userId, String boardId,
                                Set<String> statuses, Set<String> priorities,
                                Long dueBefore, Long dueAfter,
                                String sortBy, String order) throws SQLException {
        ensureAccess(userId, boardId);
        Set<String> sts = normalizeStatuses(statuses);
        Set<String> prs = normalizePriorities(priorities);
        return tasks.listByBoard(boardId, sts, prs, dueBefore, dueAfter, sortBy, order);
    }

    public Task updateTaskStatus(String userId, String boardId, String taskId, String newStatus) throws SQLException {
        ensureAccess(userId, boardId);
        String st = normalizeStatus(newStatus);
        var opt = tasks.findByIdAndBoard(taskId, boardId);
        if (opt.isEmpty()) throw AppException.validation("وظیفه یافت نشد");
        tasks.updateStatus(taskId, boardId, st);
        Task updated = tasks.findByIdAndBoard(taskId, boardId).orElseThrow();
        if (push != null) push.pushTaskUpdated(userId, boardId, toView(updated));
        return updated;
    }

    public void deleteTask(String userId, String boardId, String taskId) throws SQLException {
        ensureAccess(userId, boardId);
        var opt = tasks.findByIdAndBoard(taskId, boardId);
        if (opt.isEmpty()) throw AppException.validation("وظیفه یافت نشد");
        tasks.delete(taskId, boardId);
        if (push != null) push.pushTaskDeleted(userId, boardId, taskId);
    }

    private void ensureAccess(String userId, String boardId) throws SQLException {
        if (boards.checkAccessRole(userId, boardId).isEmpty()) throw AppException.auth("دسترسی غیرمجاز");
    }

    public static String normalizeStatus(String s) {
        if (s == null) throw AppException.validation("status required");
        String x = s.trim().toUpperCase(Locale.ROOT);
        return switch (x) {
            case "TODO" -> "TODO";
            case "INPROGRESS", "IN_PROGRESS" -> "IN_PROGRESS";
            case "DONE" -> "DONE";
            default -> throw AppException.validation("وضعیت نامعتبر");
        };
    }

    public static String normalizePriority(String s) {
        if (s == null) return "MEDIUM";
        String x = s.trim().toUpperCase(Locale.ROOT);
        return switch (x) {
            case "LOW" -> "LOW";
            case "MEDIUM" -> "MEDIUM";
            case "HIGH" -> "HIGH";
            default -> throw AppException.validation("اولویت نامعتبر");
        };
    }

    private static Set<String> normalizeStatuses(Set<String> in) {
        if (in == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String s : in) out.add(normalizeStatus(s));
        return out;
    }

    private static Set<String> normalizePriorities(Set<String> in) {
        if (in == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String s : in) out.add(normalizePriority(s));
        return out;
    }

    private static TaskView toView(Task t) {
        return new TaskView(t.getId(), t.getBoardId(), t.getTitle(), t.getDescription(),
                toClientStatus(t.getStatus()), toClientPriority(t.getPriority()), t.getDueDate(), t.getCreatedAt());
    }
    private static String toClientStatus(String s) { return switch (s) { case "IN_PROGRESS" -> "inProgress"; case "DONE" -> "done"; default -> "todo"; }; }
    private static String toClientPriority(String s) { return switch (s) { case "LOW" -> "low"; case "HIGH" -> "high"; default -> "medium"; }; }
}
