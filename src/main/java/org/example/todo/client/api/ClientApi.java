package org.example.todo.client.api;

import com.google.gson.JsonObject;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.TcpClient;
import org.example.todo.server.protocol.*;

import java.io.IOException;

import static org.example.todo.client.protocol.ProtocolJson.GSON;

/**
 * A centralized API layer for interacting with the server.
 * This decouples the UI (CLI/GUI) from the raw network communication.
 */
public class ClientApi {
    private final TcpClient tcpClient;
    private final ClientState clientState;
    private static final long TIMEOUT_MS = 5000;

    public ClientApi(TcpClient tcpClient, ClientState clientState) {
        this.tcpClient = tcpClient;
        this.clientState = clientState;
    }

    public UserView register(String username, String password) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("username", username);
        p.addProperty("password", password);
        TcpClient.Env response = send("register", p, false);
        return GSON.fromJson(response.payload.get("user"), UserView.class);
    }

    public LoginResponse login(String username, String password) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("username", username);
        p.addProperty("password", password);
        TcpClient.Env response = send("login", p, false);
        LoginResponse loginResponse = GSON.fromJson(response.payload, LoginResponse.class);
        clientState.setToken(loginResponse.token);
        return loginResponse;
    }

    public void logout() throws Exception {
        send("logout", new JsonObject(), true);
        clientState.clearToken();
        clientState.clearCurrentBoard();
    }

    public BoardSummary createBoard(String name) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("name", name);
        TcpClient.Env response = send("create_board", p, true);
        return GSON.fromJson(response.payload.get("board"), BoardSummary.class);
    }

    public ListBoardsResponse listBoards() throws Exception {
        TcpClient.Env response = send("list_boards", new JsonObject(), true);
        return GSON.fromJson(response.payload, ListBoardsResponse.class);
    }

    public void addUserToBoard(String boardId, String userId) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("userId", userId);
        send("add_user_to_board", p, true);
    }

    public ViewBoardResponse viewBoard(String boardId) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        TcpClient.Env response = send("view_board", p, true);
        return GSON.fromJson(response.payload, ViewBoardResponse.class);
    }

    public void subscribeToBoard(String boardId, int udpPort) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("udpPort", udpPort);
        send("subscribe_board", p, true);
    }

    public void unsubscribeFromBoard(String boardId, int udpPort) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("udpPort", udpPort);
        send("unsubscribe_board", p, true);
    }

    public TaskView addTask(String boardId, String title, String description, String priority, Long dueDate) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("title", title);
        p.addProperty("description", description);
        p.addProperty("priority", priority);
        if (dueDate != null) {
            p.addProperty("dueDate", dueDate);
        }
        TcpClient.Env response = send("add_task", p, true);
        return GSON.fromJson(response.payload.get("task"), TaskView.class);
    }

    public ListTasksResponse listTasks(String boardId, JsonObject filters, JsonObject sort) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        if (filters != null && filters.entrySet().size() > 0) {
            p.add("filters", filters);
        }
        if (sort != null && sort.entrySet().size() > 0) {
            p.add("sort", sort);
        }
        TcpClient.Env response = send("list_tasks", p, true);
        return GSON.fromJson(response.payload, ListTasksResponse.class);
    }

    public TaskView updateTaskStatus(String boardId, String taskId, String newStatus) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("taskId", taskId);
        p.addProperty("newStatus", newStatus);
        TcpClient.Env response = send("update_task_status", p, true);
        return GSON.fromJson(response.payload.get("task"), TaskView.class);
    }

    public void deleteTask(String boardId, String taskId) throws Exception {
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardId);
        p.addProperty("taskId", taskId);
        send("delete_task", p, true);
    }

    private TcpClient.Env send(String action, JsonObject payload, boolean requireAuth) throws Exception {
        String token = null;
        if (requireAuth) {
            token = clientState.getToken().orElseThrow(() -> new IllegalStateException("Not logged in."));
        }
        TcpClient.Env response = tcpClient.sendAndAwait(action, payload, token, TIMEOUT_MS);

        if ("error".equals(response.type)) {
            JsonObject error = response.payload.getAsJsonObject("error");
            String code = error.get("code").getAsString();
            String message = error.get("message").getAsString();
            throw new ApiException(code, message);
        }
        return response;
    }

    public static class ApiException extends IOException {
        private final String code;
        public ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String getCode() {
            return code;
        }
    }
}