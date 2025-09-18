package org.example.todo.server.protocol;

import com.google.gson.JsonObject;


public class Envelope {
    public String type;
    public String reqId;
    public String action;
    public String token;
    public JsonObject payload;
}
