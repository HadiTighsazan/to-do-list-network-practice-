package org.example.todo.client.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

import java.util.*;

import static org.example.todo.client.protocol.ProtocolJson.GSON;

public class CommandLoop implements Runnable {
    private final Scanner scanner = new Scanner(System.in);
    private final TcpClient client;
    private final ClientState state;
    private final UdpListener udp;

    public CommandLoop(TcpClient client, ClientState state, UdpListener udp) {
        this.client = client; this.state = state; this.udp = udp;
    }

    @Override
    public void run() {
        printHelp();
        while (true) {
            String prompt = state.getCurrentBoard().map(b -> "todo["+b+"]> ").orElse("todo> ");
            System.out.print(prompt);
            String line = null;
            try { if (!scanner.hasNextLine()) break; line = scanner.nextLine(); } catch (Exception e) { break; }
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
        JsonObject p = new JsonObject(); p.addProperty("username", a.get(1)); p.addProperty("password", a.get(2));
        var env = client.sendAndAwait("register", p, null, 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doLogin(List<String> a) throws Exception {
        if (a.size() < 3) { System.out.println("usage: login <username> <password>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("username", a.get(1)); p.addProperty("password", a.get(2));
        var env = client.sendAndAwait("login", p, null, 5000);
        if ("response".equals(env.type) && env.payload != null && env.payload.has("token")) { state.setToken(env.payload.get("token").getAsString()); System.out.println("logged in. token set."); }
        else System.out.println(GSON.toJson(env));
    }

    private void doLogout() throws Exception {
        var tokenOpt = state.getToken(); if (tokenOpt.isEmpty()) { System.out.println("not logged in."); return; }
        var env = client.sendAndAwait("logout", new JsonObject(), tokenOpt.get(), 5000);
        if ("response".equals(env.type)) { state.clearToken(); state.clearCurrentBoard(); System.out.println("logged out."); }
        else System.out.println(GSON.toJson(env));
    }

    private void doCreateBoard(List<String> a) throws Exception {
        var tok = state.getToken(); if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (a.size() < 2) { System.out.println("usage: create_board <name>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("name", a.get(1));
        var env = client.sendAndAwait("create_board", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doListBoards() throws Exception {
        var tok = state.getToken(); if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        var env = client.sendAndAwait("list_boards", new JsonObject(), tok.get(), 5000);
        if ("response".equals(env.type)) {
            var boards = env.payload.getAsJsonArray("boards");
            System.out.println("boards:");
            for (int i=0;i<boards.size();i++) {
                var b = boards.get(i).getAsJsonObject();
                System.out.printf("- %s | %s | role=%s | owner=%s | createdAt=%d%n",
                        b.get("id").getAsString(), b.get("name").getAsString(), b.get("role").getAsString(),
                        b.get("ownerId").getAsString(), b.get("createdAt").getAsLong());
            }
        } else System.out.println(GSON.toJson(env));
    }

    private void doAddUserToBoard(List<String> a) throws Exception {
        var tok = state.getToken(); if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (a.size() < 3) { System.out.println("usage: add_user_to_board <boardId> <userId>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("boardId", a.get(1)); p.addProperty("userId", a.get(2));
        var env = client.sendAndAwait("add_user_to_board", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doViewBoard(List<String> a) throws Exception {
        var tok = state.getToken(); if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (a.size() < 2) { System.out.println("usage: view_board <boardId>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("boardId", a.get(1));
        var env = client.sendAndAwait("view_board", p, tok.get(), 5000);
        if ("response".equals(env.type)) {
            var b = env.payload.getAsJsonObject("board");
            String boardId = b.get("id").getAsString();
            state.setCurrentBoard(boardId);
            System.out.printf("Board %s (%s) role=%s owner=%s%n", boardId, b.get("name").getAsString(), b.get("role").getAsString(), b.get("ownerId").getAsString());
            var mems = env.payload.getAsJsonArray("members");
            System.out.println("members:");
            for (int i=0;i<mems.size();i++) {
                var m = mems.get(i).getAsJsonObject();
                System.out.printf("- %s (%s) role=%s%n", m.get("userId").getAsString(), m.get("username").getAsString(), m.get("role").getAsString());
            }
            System.out.println("(entered board mode: " + boardId + ")");
            // auto-subscribe for push
            autoSubscribe(boardId);
        } else System.out.println(GSON.toJson(env));
    }

    private void autoSubscribe(String boardId) throws Exception {
        var tok = state.getToken(); if (tok.isEmpty()) return;
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardId); p.addProperty("udpPort", udp.getLocalPort());
        var env = client.sendAndAwait("subscribe_board", p, tok.get(), 5000);
        if ("response".equals(env.type)) System.out.println("subscribed for push on board " + boardId + " via UDP:" + udp.getLocalPort());
        else System.out.println(GSON.toJson(env));
    }
    private void doSubscribe(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        String boardId = a.size() >= 2 ? a.get(1) : boardOpt.orElse(null);
        if (boardId == null) { System.out.println("usage: subscribe_board <boardId>"); return; }
        autoSubscribe(boardId);
    }
    private void doUnsubscribe(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        String boardId = a.size() >= 2 ? a.get(1) : boardOpt.orElse(null);
        if (boardId == null) { System.out.println("usage: unsubscribe_board <boardId>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardId); p.addProperty("udpPort", udp.getLocalPort());
        var env = client.sendAndAwait("unsubscribe_board", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doAddTask(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (boardOpt.isEmpty()) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 4) { System.out.println("usage: add_task \"<title>\" \"<desc>\" <low|medium|high> [dueMillis]"); return; }
        JsonObject p = new JsonObject();
        p.addProperty("boardId", boardOpt.get());
        p.addProperty("title", a.get(1));
        p.addProperty("description", a.get(2));
        p.addProperty("priority", a.get(3));
        if (a.size() >= 5) try { p.addProperty("dueDate", Long.parseLong(a.get(4))); } catch (NumberFormatException ignore) {}
        var env = client.sendAndAwait("add_task", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doListTasks(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (boardOpt.isEmpty()) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        Map<String,String> kv = parseKeyValues(a.subList(1, a.size()));
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardOpt.get());
        JsonObject filters = new JsonObject();
        if (kv.containsKey("status")) { JsonArray arr = new JsonArray(); for (String s : kv.get("status").split(",")) arr.add(s.trim()); filters.add("status", arr);}
        if (kv.containsKey("priority")) { JsonArray arr = new JsonArray(); for (String s : kv.get("priority").split(",")) arr.add(s.trim()); filters.add("priority", arr);}
        if (kv.containsKey("dueBefore")) { try { filters.addProperty("dueBefore", Long.parseLong(kv.get("dueBefore"))); } catch (Exception ignore) {} }
        if (kv.containsKey("dueAfter"))  { try { filters.addProperty("dueAfter",  Long.parseLong(kv.get("dueAfter")));  } catch (Exception ignore) {} }
        if (filters.entrySet().size() > 0) p.add("filters", filters);
        JsonObject sort = new JsonObject();
        if (kv.containsKey("by"))    sort.addProperty("by", kv.get("by"));
        if (kv.containsKey("order")) sort.addProperty("order", kv.get("order"));
        if (sort.entrySet().size() > 0) p.add("sort", sort);
        var env = client.sendAndAwait("list_tasks", p, tok.get(), 5000);
        if ("response".equals(env.type)) {
            var tasks = env.payload.getAsJsonArray("tasks");
            System.out.println("tasks:");
            for (int i=0;i<tasks.size();i++) {
                var t = tasks.get(i).getAsJsonObject();
                String dueStr = (t.has("dueDate") && !t.get("dueDate").isJsonNull()) ? String.valueOf(t.get("dueDate").getAsLong()) : "-";
                System.out.printf("- %s | %s | %s | pr=%s | due=%s | created=%d%n",
                        t.get("id").getAsString(), t.get("title").getAsString(), t.get("status").getAsString(),
                        t.get("priority").getAsString(), dueStr, t.get("createdAt").getAsLong());
            }
        } else System.out.println(GSON.toJson(env));
    }

    private void doUpdateTaskStatus(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (boardOpt.isEmpty()) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 3) { System.out.println("usage: update_task_status <taskId> <todo|inProgress|done>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardOpt.get()); p.addProperty("taskId", a.get(1)); p.addProperty("newStatus", a.get(2));
        var env = client.sendAndAwait("update_task_status", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doDeleteTask(List<String> a) throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty()) { System.out.println("not logged in."); return; }
        if (boardOpt.isEmpty()) { System.out.println("not in board mode. run: view_board <boardId>"); return; }
        if (a.size() < 2) { System.out.println("usage: delete_task <taskId>"); return; }
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardOpt.get()); p.addProperty("taskId", a.get(1));
        var env = client.sendAndAwait("delete_task", p, tok.get(), 5000);
        System.out.println(GSON.toJson(env));
    }

    private void doAutoUnsubscribe() throws Exception {
        var tok = state.getToken(); var boardOpt = state.getCurrentBoard();
        if (tok.isEmpty() || boardOpt.isEmpty()) return;
        JsonObject p = new JsonObject(); p.addProperty("boardId", boardOpt.get()); p.addProperty("udpPort", udp.getLocalPort());
        client.sendAndAwait("unsubscribe_board", p, tok.get(), 3000);
        state.clearCurrentBoard();
    }

    private static void printHelp() {
        System.out.println("commands:\n  register <u> <p>\n  login <u> <p>\n  logout\n  create_board <name>\n  list_boards\n  add_user_to_board <boardId> <userId>\n  view_board <boardId>\n  subscribe_board [boardId]\n  unsubscribe_board [boardId]\n\n  add_task \"<title>\" \"<desc>\" <low|medium|high> [dueMillis]\n  list_tasks [by=createdAt|due|priority] [order=asc|desc] [status=...] [priority=...] [dueBefore=ms] [dueAfter=ms]\n  update_task_status <taskId> <todo|inProgress|done>\n  delete_task <taskId>\n  help\n  exit");
    }

    // simple args parser with quotes support
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
