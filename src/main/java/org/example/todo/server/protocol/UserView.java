package org.example.todo.server.protocol;

public class UserView {
    public String id;
    public String username;
    public long createdAt;

    public UserView() {}
    public UserView(String id, String username, long createdAt) {
        this.id = id; this.username = username; this.createdAt = createdAt;
    }
}
