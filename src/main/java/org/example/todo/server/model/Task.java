package org.example.todo.server.model;

public class Task extends BaseEntity {
    private String boardId;
    private String title;
    private String description;
    private String status;    // TODO | IN_PROGRESS | DONE
    private String priority;  // LOW | MEDIUM | HIGH
    private Long dueDate;     // epoch millis, nullable

    public Task() {}

    public Task(String id, long createdAt, String boardId, String title, String description, String status, String priority, Long dueDate) {
        super(id, createdAt);
        this.boardId = boardId; this.title = title; this.description = description; this.status = status; this.priority = priority; this.dueDate = dueDate;
    }

    public String getBoardId() { return boardId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Long getDueDate() { return dueDate; }

    public void setBoardId(String boardId) { this.boardId = boardId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }
}
