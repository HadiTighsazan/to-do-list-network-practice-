package org.example.todo.server.protocol;

public class BoardSummary {
    public String id;
    public String name;
    public String ownerId;
    public long createdAt;
    public String role; // OWNER | MEMBER

    public BoardSummary() {}
    public BoardSummary(String id, String name, String ownerId, long createdAt, String role) {
        this.id=id; this.name=name; this.ownerId=ownerId; this.createdAt=createdAt; this.role=role;
    }
}
