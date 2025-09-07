package org.example.todo.server.model;

/**
 * Common fields for persisted entities.
 */
public abstract class BaseEntity {
    protected String id;       // UUID string
    protected long createdAt;  // epoch millis UTC

    protected BaseEntity() {}

    protected BaseEntity(String id, long createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
