package org.example.todo.client.protocol;

import com.google.gson.JsonObject;

/**
 * Client-side copy of the Envelope used over TCP.
 */
public class Envelope {
    public String type;       // request | response | error | push
    public String reqId;
    public String action;
    public String token;      // optional
    public JsonObject payload;// optional
}
