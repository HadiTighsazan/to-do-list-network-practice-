package org.example.todo.server.model;

public class BoardMember {
    private String boardId;
    private String userId;
    private String role; // OWNER | MEMBER
    private long createdAt;

    public BoardMember() {}

    public BoardMember(String boardId, String userId, String role, long createdAt) {
        this.boardId = boardId; this.userId = userId; this.role = role; this.createdAt = createdAt;
    }

    public String getBoardId() { return boardId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public long getCreatedAt() { return createdAt; }

    public void setBoardId(String boardId) { this.boardId = boardId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
