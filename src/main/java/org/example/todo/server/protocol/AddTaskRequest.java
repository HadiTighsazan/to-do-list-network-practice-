package org.example.todo.server.protocol;

public class AddTaskRequest {
    public String boardId;
    public String title;
    public String description;
    public String priority; // low|medium|high
    public Long dueDate;    // epoch millis (optional)
}
