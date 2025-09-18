package org.example.todo.client.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.todo.client.api.ClientApi;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.PushNotificationListener;
import org.example.todo.client.net.UdpListener;

import java.util.*;

import static org.example.todo.client.protocol.ProtocolJson.GSON;

public class CommandLoop implements Runnable, PushNotificationListener {
    private final Scanner scanner = new Scanner(System.in);
    private final ClientApi api;
    private final ClientState state;
    private final UdpListener udp;

    public CommandLoop(ClientApi api, ClientState state, UdpListener udp) {
        this.api = api; this.state = state; this.udp = udp;
        this.udp.addListener(this);
    }

    @Override
    public void run() {
        printHelp();
        while (true) {
            String prompt = state.getCurrentBoard().map(b -> "todo["+b.substring(0, 8)+"]> ").orElse("todo> ");
            System.out.print(prompt);
            String line;
            try {
                if (!scanner.hasNextLine())
                    break;
                line = scanner.nextLine();
            }
            catch (Exception e) { break; }
            line = line.trim(); if (line.isEmpty()) continue;
            List<String> args = parseArgs(line);
            String cmd = args.get(0).toLowerCase(Locale.ROOT);
            try {
                switch (cmd) {
                    case "help" -> printHelp();
                    case "exit", "quit" -> { doAutoUnsubscribe(); System.out.println("bye."); return; }
                    case "register" -> doRegister(args);
                    case "login" -> doLogin(args);
                    case "logout" -> { doAutoUnsubscribe(); doLogout(); }
                    case "create_board" -> doCreateBoard(args);
                    case "list_boards" -> doListBoards();
                    case "add_user_to_board" -> doAddUserToBoard(args);
                    case "view_board" -> doViewBoard(args);
                    case "subscribe_board" -> doSubscribe(args);
                    case "unsubscribe_board" -> doUnsubscribe(args);
                    case "add_task" -> doAddTask(args);
                    case "list_tasks" -> doListTasks(args);
                    case "update_task_status" -> doUpdateTaskStatus(args);
                    case "delete_task" -> doDeleteTask(args);
                    default -> System.out.println("unknown command. type 'help'.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void doRegister(List<String> a) throws Exception {
        if (a.size() < 3) { System.out.println("usage: register <username> <password>"); return; }
        var user = api.register(a.get(1), a.get(2));
        System.out.println("User registered: " + GSON.toJson(user));
    }

    private void doLogin(List<String> a) throws Exception {
        if (a.size() < 3) { System.out.println("usage: login <username> <password>"); return; }
        var loginResponse = api.login(a.get(1), a.get(2));
        System.out.println("logged in. user: " + loginResponse.user.username + "id: "+loginResponse.user.id);
    }

    private void doLogout() throws Exception {
        if (state.getToken().isEmpty()) { System.out.println("not logged in."); return; }
        api.logout();
        System.out.println("logged out.");
    }

    private void doCreateBoard(List<String> a) throws Exception {
        if (a.size() < 2) { System.out.println("usage: create_board <name>"); return; }
        var board = api.createBoard(a.get(1));
        System.out.println("board created: " + GSON.toJson(board));
    }

    private void doListBoards() throws Exception {
        var resp = api.listBoards();
        System.out.println("boards:");
        for (var b : resp.boards) {
            System.out.printf("- %s | %s | role=%s | owner=%s | createdAt=%d%n",
                    b.id, b.name, b.role, b.ownerId, b.createdAt);
        }
    }

    private void doAddUserToBoard(List<String> a) throws Exception {
        if (a.size() < 3) { System.out.println("usage: add_user_to_board <boardId> <userId>"); return; }
        api.addUserToBoard(a.get(1), a.get(2));
        System.out.println("user added to board.");
    }

    private void doViewBoard(List<String> a) throws Exception {
        if (a.size() < 2) { System.out.println("usage: view_board <boardId>"); return; }
        var resp = api.viewBoard(a.get(1));
        var b = resp.board;
        String boardId = b.id;
        state.setCurrentBoard(boardId);
        System.out.printf("Board %s (%s) role=%s owner=%s%n", boardId, b.name, b.role, b.ownerId);
        System.out.println("members:");
        for (var m : resp.members) {
            System.out.printf("- %s (%s) role=%s%n", m.userId, m.username, m.role);
        }
        System.out.println("(entered board mode: " + boardId + ")");
        autoSubscribe(boardId);
    }

    private void autoSubscribe(String boardId) throws Exception {
        api.subscribeToBoard(boardId, udp.getLocalPort());
        System.out.println("subscribed for push on board " + boardId + " via UDP:" + udp.getLocalPort());
    }
    private void doSubscribe(List<String> a) throws Exception {
        String boardId = a.size() >= 2 ? a.get(1) : state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("usage: subscribe_board <boardId> (or view a board first)"); return; }
        autoSubscribe(boardId);
    }
    private void doUnsubscribe(List<String> a) throws Exception {
        String boardId = a.size() >= 2 ? a.get(1) : state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("usage: unsubscribe_board <boardId> (or view a board first)"); return; }
        api.unsubscribeFromBoard(boardId, udp.getLocalPort());
        System.out.println("unsubscribed from board " + boardId);
    }

    private void doAddTask(List<String> a) throws Exception {
        var boardId = state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 4) { System.out.println("usage: add_task \"<title>\" \"<desc>\" <low|medium|high> [dueMillis]"); return; }
        Long dueDate = a.size() >= 5 ? Long.parseLong(a.get(4)) : null;
        var task = api.addTask(boardId, a.get(1), a.get(2), a.get(3), dueDate);
        System.out.println("task added: " + GSON.toJson(task));
    }

    private void doListTasks(List<String> a) throws Exception {
        var boardId = state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("not in board mode. run: view_board <boardId>"); return; }

        Map<String,String> kv = parseKeyValues(a.subList(1, a.size()));
        JsonObject filters = new JsonObject();
        if (kv.containsKey("status")) { JsonArray arr = new JsonArray(); for (String s : kv.get("status").split(",")) arr.add(s.trim()); filters.add("status", arr);}
        if (kv.containsKey("priority")) { JsonArray arr = new JsonArray(); for (String s : kv.get("priority").split(",")) arr.add(s.trim()); filters.add("priority", arr);}
        if (kv.containsKey("dueBefore")) { try { filters.addProperty("dueBefore", Long.parseLong(kv.get("dueBefore"))); } catch (Exception ignore) {} }
        if (kv.containsKey("dueAfter"))  { try { filters.addProperty("dueAfter",  Long.parseLong(kv.get("dueAfter")));  } catch (Exception ignore) {} }

        JsonObject sort = new JsonObject();
        if (kv.containsKey("by"))    sort.addProperty("by", kv.get("by"));
        if (kv.containsKey("order")) sort.addProperty("order", kv.get("order"));

        var resp = api.listTasks(boardId, filters, sort);
        System.out.println("tasks:");
        for (var t : resp.tasks) {
            String dueStr = (t.dueDate != null) ? String.valueOf(t.dueDate) : "-";
            System.out.printf("- %s | %s | %s | pr=%s | due=%s | created=%d%n",
                    t.id, t.title, t.status, t.priority, dueStr, t.createdAt);
        }
    }

    private void doUpdateTaskStatus(List<String> a) throws Exception {
        var boardId = state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 3) { System.out.println("usage: update_task_status <taskId> <todo|inProgress|done>"); return; }
        var task = api.updateTaskStatus(boardId, a.get(1), a.get(2));
        System.out.println("task updated: " + GSON.toJson(task));
    }

    private void doDeleteTask(List<String> a) throws Exception {
        var boardId = state.getCurrentBoard().orElse(null);
        if (boardId == null) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 2) { System.out.println("usage: delete_task <taskId>"); return; }
        api.deleteTask(boardId, a.get(1));
        System.out.println("task deleted.");
    }

    private void doAutoUnsubscribe() {
        try {
            var boardOpt = state.getCurrentBoard();
            if (state.getToken().isEmpty() || boardOpt.isEmpty()) return;
            api.unsubscribeFromBoard(boardOpt.get(), udp.getLocalPort());
            state.clearCurrentBoard();
        } catch (Exception e) {
            // Ignore on exit
        }
    }

    @Override
    public void onPushNotification(JsonObject payload) {
        System.out.println("\n[PUSH NOTIFICATION]: " + GSON.toJson(payload));
        String prompt = state.getCurrentBoard().map(b -> "todo["+b.substring(0, 8)+"]> ").orElse("todo> ");
        System.out.print(prompt);
    }

    private static void printHelp() {
        System.out.println("commands:\n  register <u> <p>\n  login <u> <p>\n  logout\n  create_board <name>\n  list_boards\n  add_user_to_board <boardId> <userId>\n  view_board <boardId>\n  subscribe_board [boardId]\n  unsubscribe_board [boardId]\n\n  add_task \"<title>\" \"<desc>\" <low|medium|high> [dueMillis]\n  list_tasks [by=createdAt|due|priority] [order=asc|desc] [status=...] [priority=...] [dueBefore=ms] [dueAfter=ms]\n  update_task_status <taskId> <todo|inProgress|done>\n  delete_task <taskId>\n  help\n  exit");
    }

    private static List<String> parseArgs(String line) {
        List<String> out = new ArrayList<>();
        boolean inQuote = false; StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') { inQuote = !inQuote; continue; }
            if (!inQuote && Character.isWhitespace(ch)) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else { cur.append(ch); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    private static Map<String,String> parseKeyValues(List<String> parts) {
        Map<String,String> m = new LinkedHashMap<>();
        for (String p : parts) {
            int i = p.indexOf('='); if (i <= 0) continue;
            m.put(p.substring(0,i), p.substring(i+1));
        }
        return m;
    }
}