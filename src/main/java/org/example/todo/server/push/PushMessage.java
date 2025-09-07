package org.example.todo.server.push;

import com.google.gson.JsonElement;

/** JSON payload container for UDP push events. */
public class PushMessage {
    public String type = "push";   // always "push"
    public String event;           // task_added | task_updated | task_deleted | member_added
    public String boardId;
    public Long ts;                // epoch millis
    public String actorUserId;     // optional

    // Optional sections depending on event
    public JsonElement task;       // TaskView JSON
    public JsonElement member;     // BoardMemberView JSON
}
