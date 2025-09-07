package org.example.todo.server.protocol;

public class TaskView {
    public String id;
    public String boardId;
    public String title;
    public String description;
    public String status;    // todo | inProgress | done (client-friendly)
    public String priority;  // low | medium | high
    public Long dueDate;
    public long createdAt;

    public TaskView() {}
    public TaskView(String id, String boardId, String title, String description, String status, String priority, Long dueDate, long createdAt) {
        this.id=id; this.boardId=boardId; this.title=title; this.description=description; this.status=status; this.priority=priority; this.dueDate=dueDate; this.createdAt=createdAt;
    }
}
