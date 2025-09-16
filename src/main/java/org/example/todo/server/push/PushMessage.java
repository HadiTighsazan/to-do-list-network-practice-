package org.example.todo.server.push;

import com.google.gson.JsonElement;

public class PushMessage {
    public String type = "push";
    public String event;
    public String boardId;
    public Long ts;
    public String actorUserId;

    public JsonElement task;
    public JsonElement member;
}
