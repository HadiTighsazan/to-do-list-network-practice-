package org.example.todo.server.protocol;

public class BoardMemberView {
    public String userId;
    public String username;
    public String role;
    public long joinedAt;

    public BoardMemberView() {}
    public BoardMemberView(String userId, String username, String role, long joinedAt) {
        this.userId=userId; this.username=username; this.role=role; this.joinedAt=joinedAt;
    }
}
