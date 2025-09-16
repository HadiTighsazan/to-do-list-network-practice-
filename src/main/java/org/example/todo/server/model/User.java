package org.example.todo.server.model;

public class User extends BaseEntity {
    private String username;
    private String passwordHash;
    private String passwordSalt;

    public User() {}

    public User(String id, long createdAt, String username, String passwordHash, String passwordSalt) {
        super(id, createdAt);
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }

    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
}
