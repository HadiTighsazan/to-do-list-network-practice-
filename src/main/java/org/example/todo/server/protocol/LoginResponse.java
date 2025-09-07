package org.example.todo.server.protocol;

public class LoginResponse {
    public String token;
    public long expiresAt;
    public UserView user;
}
