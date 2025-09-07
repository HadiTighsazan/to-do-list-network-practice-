package org.example.todo.client.net;

import com.google.gson.JsonObject;
import org.example.todo.client.protocol.ProtocolJson;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.example.todo.client.protocol.ProtocolJson.GSON;

/**
 * Robust TCP client: connects in ctor, starts reader thread,
 * supports sendAndAwait with reqId correlation.
 */
public class TcpClient implements Closeable {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private final Map<String, CompletableFuture<Env>> pending = new ConcurrentHashMap<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tcp-client-reader");
        t.setDaemon(true);
        return t;
    });

    public TcpClient(String host, int port) throws IOException {
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(host, port), 5000);
            this.socket.setTcpNoDelay(true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ensure fields not null if ctor succeeds
            throw e;
        }
        reader.submit(this::readLoop);
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Env env = GSON.fromJson(line, Env.class);
                if (env != null && env.reqId != null) {
                    CompletableFuture<Env> f = pending.remove(env.reqId);
                    if (f != null) { f.complete(env); continue; }
                }
                // If no waiter, just print raw line for visibility
                System.out.println(line);
            }
        } catch (IOException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // complete all waiters exceptionally
            IOException ex = new IOException("connection closed");
            pending.values().forEach(f -> f.completeExceptionally(ex));
            pending.clear();
        }
    }

    public Env sendAndAwait(String action, JsonObject payload, String token, long timeoutMs) throws Exception {
        String reqId = UUID.randomUUID().toString();
        JsonObject env = new JsonObject();
        env.addProperty("type", "request");
        env.addProperty("reqId", reqId);
        env.addProperty("action", action);
        if (token != null) env.addProperty("token", token);
        if (payload != null) env.add("payload", payload);

        CompletableFuture<Env> fut = new CompletableFuture<>();
        pending.put(reqId, fut);

        String json = GSON.toJson(env);
        synchronized (out) { // guard writes
            out.write(json);
            out.write('\n');
            out.flush();
        }
        return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        try { reader.shutdownNow(); } catch (Exception ignore) {}
        try { socket.close(); } finally {
            // nothing
        }
    }

    // Minimal envelope view used on the client side
    public static class Env {
        public String type;
        public String reqId;
        public String action;
        public JsonObject payload;
    }
}
