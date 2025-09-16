package org.example.todo.server.model;

public class Session {
    private String jti;
    private String userId;
    private long expiresAt;
    private long createdAt;

    public Session() {}

    public Session(String jti, String userId, long expiresAt, long createdAt) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getJti() { return jti; }
    public String getUserId() { return userId; }
    public long getExpiresAt() { return expiresAt; }
    public long getCreatedAt() { return createdAt; }

    public void setJti(String jti) { this.jti = jti; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
