package org.example.todo.server.model;

public class Board extends BaseEntity {
    private String name;
    private String ownerId;

    public Board() {}

    public Board(String id, long createdAt, String name, String ownerId) {
        super(id, createdAt);
        this.name = name;
        this.ownerId = ownerId;
    }

    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }

    public void setName(String name) { this.name = name; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}
