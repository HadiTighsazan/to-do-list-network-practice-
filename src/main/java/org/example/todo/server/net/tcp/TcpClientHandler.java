package org.example.todo.server.net.tcp;

import com.google.gson.JsonObject;
import org.example.todo.server.auth.AuthService;
import org.example.todo.server.core.AppException;
import org.example.todo.server.model.User;
import org.example.todo.server.protocol.*;
import org.example.todo.server.push.PushService;
import org.example.todo.server.service.BoardService;
import org.example.todo.server.service.TaskService;
import org.example.todo.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.UUID;

import static org.example.todo.server.protocol.ProtocolJson.GSON;

/** Per-connection handler (Phase 5) with push subscribe/unsubscribe & task routes. */
public class TcpClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);

    private final Socket socket;
    private final UserService userService;
    private final AuthService authService;
    private final BoardService boardService;
    private final TaskService taskService;
    private final PushService push;

    private final String connectionKey = UUID.randomUUID().toString();

    public TcpClientHandler(Socket socket, UserService userService, AuthService authService, BoardService boardService, TaskService taskService, PushService push) {
        this.socket = socket; this.userService = userService; this.authService = authService; this.boardService = boardService; this.taskService = taskService; this.push = push;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        log.info("Client connected: {}", remote);
        try (Socket s = socket; InputStream is = s.getInputStream(); OutputStream os = s.getOutputStream()) {
            while (true) {
                String json = MessageCodec.readJson(is);
                Envelope env = GSON.fromJson(json, Envelope.class);
                Envelope resp;
                try {
                    resp = handle(env);
                } catch (AppException ae) {
                    resp = error(env != null ? env.reqId : null, ae.getCode(), ae.getMessage());
                } catch (SecurityException se) {
                    resp = error(env != null ? env.reqId : null, "AUTH_INVALID", se.getMessage());
                } catch (SQLException sqle) {
                    log.error("SQL error", sqle);
                    resp = error(env != null ? env.reqId : null, "DB_ERROR", "Database error");
                } catch (Exception e) {
                    log.error("Unhandled error", e);
                    resp = error(env != null ? env.reqId : null, "INTERNAL", "Internal server error");
                }
                String out = GSON.toJson(resp);
                MessageCodec.writeJson(os, out);
            }
        } catch (IOException e) {
            log.info("Client disconnected: {}", remote);
        } finally {
            try { push.clearConnection(connectionKey); } catch (Exception ignore) {}
        }
    }

    private Envelope handle(Envelope env) throws Exception {
        if (env == null || env.type == null || !"request".equals(env.type)) {
            throw new AppException("VALIDATION_ERROR", "Invalid envelope");
        }
        String action = env.action != null ? env.action : "";
        return switch (action) {
            case "register" -> handleRegister(env);
            case "login" -> handleLogin(env);
            case "logout" -> handleLogout(env);
            case "create_board" -> handleCreateBoard(env);
            case "list_boards" -> handleListBoards(env);
            case "add_user_to_board" -> handleAddUserToBoard(env);
            case "view_board" -> handleViewBoard(env);
            // tasks
            case "add_task" -> handleAddTask(env);
            case "list_tasks" -> handleListTasks(env);
            case "update_task_status" -> handleUpdateTaskStatus(env);
            case "delete_task" -> handleDeleteTask(env);
            // push
            case "subscribe_board" -> handleSubscribeBoard(env);
            case "unsubscribe_board" -> handleUnsubscribeBoard(env);
            default -> throw new AppException("VALIDATION_ERROR", "Unknown action: " + action);
        };
    }

    private Envelope handleRegister(Envelope env) throws SQLException {
        RegisterRequest req = GSON.fromJson(env.payload, RegisterRequest.class);
        if (req == null || req.username == null || req.password == null)
            throw new AppException("VALIDATION_ERROR", "username/password required");

        // --- START OF CHANGES ---
        var regResult = userService.register(req.username, req.password.toCharArray());
        LoginResponse resp = new LoginResponse();
        resp.token = regResult.token();
        resp.expiresAt = regResult.expiresAt();
        resp.user = new UserView(regResult.user().getId(), regResult.user().getUsername(), regResult.user().getCreatedAt());
        return ok(env, GSON.toJsonTree(resp).getAsJsonObject());
        // --- END OF CHANGES ---
    }

    private Envelope handleLogin(Envelope env) throws SQLException {
        LoginRequest req = GSON.fromJson(env.payload, LoginRequest.class);
        if (req == null || req.username == null || req.password == null)
            throw new AppException("VALIDATION_ERROR", "username/password required");
        var lr = userService.login(req.username, req.password.toCharArray());
        LoginResponse resp = new LoginResponse();
        resp.token = lr.token();
        resp.expiresAt = lr.expiresAt();
        resp.user = new UserView(lr.user().getId(), lr.user().getUsername(), lr.user().getCreatedAt());
        return ok(env, GSON.toJsonTree(resp).getAsJsonObject());
    }

    private Envelope handleLogout(Envelope env) throws SQLException {
        requireToken(env);
        authService.authenticate(env.token);
        userService.logout(env.token);
        return ok(env, GSON.toJsonTree(new AckResponse("logged out")).getAsJsonObject());
    }

    private Envelope handleCreateBoard(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        CreateBoardRequest req = GSON.fromJson(env.payload, CreateBoardRequest.class);
        if (req == null || req.name == null || req.name.isBlank()) throw new AppException("VALIDATION_ERROR", "name required");
        var b = boardService.createBoard(ac.userId, req.name);
        BoardSummary sum = new BoardSummary(b.getId(), b.getName(), b.getOwnerId(), b.getCreatedAt(), "OWNER");
        JsonObject payload = new JsonObject(); payload.add("board", GSON.toJsonTree(sum));
        return ok(env, payload);
    }

    private Envelope handleListBoards(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        var list = boardService.listBoardsForUser(ac.userId);
        var resp = new ListBoardsResponse();
        resp.boards = list.stream().map(v -> new BoardSummary(v.id, v.name, v.ownerId, v.createdAt, v.role)).toList();
        return ok(env, GSON.toJsonTree(resp).getAsJsonObject());
    }

    private Envelope handleAddUserToBoard(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        AddUserToBoardRequest req = GSON.fromJson(env.payload, AddUserToBoardRequest.class);
        if (req == null || req.boardId == null || req.userId == null) throw new AppException("VALIDATION_ERROR", "boardId/userId required");
        boardService.addMember(ac.userId, req.boardId, req.userId);
        return ok(env, GSON.toJsonTree(new AckResponse("member added")).getAsJsonObject());
    }

    private Envelope handleViewBoard(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        ViewBoardRequest req = GSON.fromJson(env.payload, ViewBoardRequest.class);
        if (req == null || req.boardId == null) throw new AppException("VALIDATION_ERROR", "boardId required");
        var roleOpt = boardService.checkAccessRole(ac.userId, req.boardId);
        if (roleOpt.isEmpty()) throw new AppException("FORBIDDEN", "دسترسی غیرمجاز");
        var b = boardService.listBoardsForUser(ac.userId).stream().filter(v -> v.id.equals(req.boardId)).findFirst()
                .orElseThrow(() -> new AppException("NOT_FOUND", "بورد یافت نشد"));
        var members = boardService.listMembersWithUsername(req.boardId);
        ViewBoardResponse resp = new ViewBoardResponse();
        resp.board = new BoardSummary(b.id, b.name, b.ownerId, b.createdAt, roleOpt.get());
        resp.members = members.stream().map(m -> new BoardMemberView(m.userId, m.username, m.role, m.createdAt)).toList();
        return ok(env, GSON.toJsonTree(resp).getAsJsonObject());
    }

    private Envelope handleAddTask(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        AddTaskRequest req = GSON.fromJson(env.payload, AddTaskRequest.class);
        if (req == null || req.boardId == null || req.title == null || req.priority == null)
            throw new AppException("VALIDATION_ERROR", "boardId/title/priority required");
        var t = taskService.addTask(ac.userId, req.boardId, req.title, req.description, req.priority, req.dueDate);
        var tv = toView(t);
        JsonObject p = new JsonObject(); p.add("task", GSON.toJsonTree(tv));
        return ok(env, p);
    }

    private Envelope handleListTasks(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        ListTasksRequest req = GSON.fromJson(env.payload, ListTasksRequest.class);
        if (req == null || req.boardId == null) throw new AppException("VALIDATION_ERROR", "boardId required");
        var statuses = new java.util.LinkedHashSet<String>();
        var priorities = new java.util.LinkedHashSet<String>();
        if (req.filters != null) {
            if (req.filters.status != null) java.util.Collections.addAll(statuses, req.filters.status);
            if (req.filters.priority != null) java.util.Collections.addAll(priorities, req.filters.priority);
        }
        String by = req.sort != null ? req.sort.by : null; String order = req.sort != null ? req.sort.order : null;
        Long dueBefore = req.filters != null ? req.filters.dueBefore : null; Long dueAfter = req.filters != null ? req.filters.dueAfter : null;
        var list = taskService.listTasks(ac.userId, req.boardId, statuses, priorities, dueBefore, dueAfter, by, order);
        var resp = new ListTasksResponse();
        resp.tasks = list.stream().map(TcpClientHandler::toView).toList();
        return ok(env, GSON.toJsonTree(resp).getAsJsonObject());
    }

    private Envelope handleUpdateTaskStatus(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        UpdateTaskStatusRequest req = GSON.fromJson(env.payload, UpdateTaskStatusRequest.class);
        if (req == null || req.boardId == null || req.taskId == null || req.newStatus == null)
            throw new AppException("VALIDATION_ERROR", "boardId/taskId/newStatus required");
        var t = taskService.updateTaskStatus(ac.userId, req.boardId, req.taskId, req.newStatus);
        JsonObject p = new JsonObject(); p.add("task", GSON.toJsonTree(toView(t)));
        return ok(env, p);
    }

    private Envelope handleDeleteTask(Envelope env) throws SQLException {
        var ac = authService.authenticate(requireToken(env));
        DeleteTaskRequest req = GSON.fromJson(env.payload, DeleteTaskRequest.class);
        if (req == null || req.boardId == null || req.taskId == null)
            throw new AppException("VALIDATION_ERROR", "boardId/taskId required");
        taskService.deleteTask(ac.userId, req.boardId, req.taskId);
        return ok(env, GSON.toJsonTree(new AckResponse("task deleted")).getAsJsonObject());
    }

    private Envelope handleSubscribeBoard(Envelope env) throws Exception {
        var ac = authService.authenticate(requireToken(env));
        SubscribeBoardRequest req = GSON.fromJson(env.payload, SubscribeBoardRequest.class);
        if (req == null || req.boardId == null || req.udpPort == null) throw new AppException("VALIDATION_ERROR", "boardId/udpPort required");
        var role = boardService.checkAccessRole(ac.userId, req.boardId);
        if (role.isEmpty()) throw new AppException("FORBIDDEN", "دسترسی غیرمجاز");
        var addr = socket.getInetAddress();
        push.subscribe(connectionKey, req.boardId, ac.userId, addr, req.udpPort);
        return ok(env, GSON.toJsonTree(new AckResponse("subscribed")).getAsJsonObject());
    }

    private Envelope handleUnsubscribeBoard(Envelope env) throws Exception {
        var ac = authService.authenticate(requireToken(env));
        UnsubscribeBoardRequest req = GSON.fromJson(env.payload, UnsubscribeBoardRequest.class);
        if (req == null || req.boardId == null) throw new AppException("VALIDATION_ERROR", "boardId required");
        var addr = socket.getInetAddress();
        int port = req.udpPort != null ? req.udpPort : 0;
        push.unsubscribe(connectionKey, req.boardId, ac.userId, addr, port);
        return ok(env, GSON.toJsonTree(new AckResponse("unsubscribed")).getAsJsonObject());
    }

    private static String requireToken(Envelope env) {
        if (env.token == null || env.token.isBlank()) throw new SecurityException("Token missing");
        return env.token;
    }

    private static Envelope ok(Envelope req, JsonObject payload) {
        Envelope e = new Envelope(); e.type = "response"; e.reqId = req.reqId; e.action = req.action; e.payload = payload; return e;
    }

    private static Envelope error(String reqId, String code, String message) {
        Envelope e = new Envelope(); e.type = "error"; e.reqId = reqId; com.google.gson.JsonObject err = new com.google.gson.JsonObject();
        err.addProperty("code", code); err.addProperty("message", message);
        e.payload = new com.google.gson.JsonObject(); e.payload.add("error", err); return e;
    }

    private static TaskView toView(org.example.todo.server.model.Task t) {
        return new TaskView(t.getId(), t.getBoardId(), t.getTitle(), t.getDescription(),
                toClientStatus(t.getStatus()), toClientPriority(t.getPriority()), t.getDueDate(), t.getCreatedAt());
    }
    private static String toClientStatus(String s) { return switch (s) { case "IN_PROGRESS" -> "inProgress"; case "DONE" -> "done"; default -> "todo"; }; }
    private static String toClientPriority(String s) { return switch (s) { case "LOW" -> "low"; case "HIGH" -> "high"; default -> "medium"; }; }
}
