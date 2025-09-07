package org.example.todo.server.protocol;

public class UnsubscribeBoardRequest {
    public String boardId;
    public Integer udpPort; // optional: if provided, target that port specifically
}
