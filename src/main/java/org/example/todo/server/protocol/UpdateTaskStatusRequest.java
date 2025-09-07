package org.example.todo.server.protocol;

public class UpdateTaskStatusRequest {
    public String boardId;
    public String taskId;
    public String newStatus; // todo | inProgress | done
}
