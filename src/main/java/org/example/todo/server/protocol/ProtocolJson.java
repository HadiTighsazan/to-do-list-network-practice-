package org.example.todo.server.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Shared Gson instance/config for protocol.
 */
public final class ProtocolJson {
    private ProtocolJson() {}
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
}
