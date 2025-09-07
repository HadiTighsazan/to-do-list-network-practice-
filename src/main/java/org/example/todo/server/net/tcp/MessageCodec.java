package org.example.todo.server.net.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Length-prefixed (big-endian int32) UTF-8 JSON frames.
 */
public final class MessageCodec {
    private MessageCodec() {}

    public static void writeJson(OutputStream os, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    public static String readJson(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        int len;
        try {
            len = dis.readInt();
        } catch (IOException e) {
            throw e; // socket closed/stream ended
        }
        if (len < 0 || len > (16 * 1024 * 1024)) throw new IOException("Invalid frame length: " + len);
        byte[] buf = new byte[len];
        dis.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }
}
