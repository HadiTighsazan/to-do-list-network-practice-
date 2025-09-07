package org.example.todo.server.protocol;

public class AckResponse {
    public String message;
    public AckResponse() {}
    public AckResponse(String message) { this.message = message; }
}
