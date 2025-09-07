package org.example.todo.client.net;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import static org.example.todo.client.protocol.ProtocolJson.GSON;

/**
 * Robust TCP client with proper MessageCodec protocol matching server
 */
public class TcpClient implements Closeable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Map<String, CompletableFuture<Env>> pending = new ConcurrentHashMap<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tcp-client-reader");
        t.setDaemon(true);
        return t;
    });

    public TcpClient(String host, int port) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 5000);
        this.socket.setTcpNoDelay(true);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        reader.submit(this::readLoop);
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                String json = MessageCodec.readJson(in);
                Env env = GSON.fromJson(json, Env.class);
                if (env != null && env.reqId != null) {
                    CompletableFuture<Env> f = pending.remove(env.reqId);
                    if (f != null) {
                        f.complete(env);
                        continue;
                    }
                }

                // Push message or unexpected message
                if (env != null && "push".equals(env.type)) {
                    System.out.println("\n[PUSH from TCP]: " + json);
                    System.out.print("todo> ");
                } else if (env != null) {
                    System.out.println("Unexpected message: " + json);
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Read error: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOException ex = new IOException("Connection closed");
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
        synchronized (out) {
            MessageCodec.writeJson(out, json);
        }

        try {
            return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(reqId);
            throw new IOException("Request timeout after " + timeoutMs + "ms");
        }
    }

    @Override
    public void close() throws IOException {
        try {
            reader.shutdownNow();
            reader.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Exception ignore) {}
        try {
            socket.close();
        } catch (Exception ignore) {}
    }

    public static class Env {
        public String type;
        public String reqId;
        public String action;
        public JsonObject payload;
    }
}
