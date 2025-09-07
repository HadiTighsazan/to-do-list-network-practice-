package org.example.todo.client.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ProtocolJson {
    private ProtocolJson() {}
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
}
