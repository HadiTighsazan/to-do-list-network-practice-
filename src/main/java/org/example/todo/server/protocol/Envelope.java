package org.example.todo.server.protocol;

import com.google.gson.JsonObject;

/**
 * Generic JSON envelope for all messages over TCP.
 * request:  {type:"request", reqId, action, token?, payload}
 * response: {type:"response", reqId, action, payload}
 * error:    {type:"error",    reqId, error:{code,message}}
 */
public class Envelope {
    public String type;       // request | response | error | push (later)
    public String reqId;      // client-supplied (for request/response correlation)
    public String action;     // e.g. register | login | logout | ...
    public String token;      // optional for protected endpoints
    public JsonObject payload;// nullable
}
