package org.example.todo.client.protocol;

import com.google.gson.JsonObject;


public class Envelope {
    public String type;
    public String reqId;
    public String action;
    public String token;
    public JsonObject payload;
}
