package org.example.todo.client.app;

import java.util.Optional;

public class ClientState {
    private volatile String token;
    private volatile String currentBoardId;

    public synchronized void setToken(String token) { this.token = token; }
    public synchronized Optional<String> getToken() { return Optional.ofNullable(token); }
    public synchronized void clearToken() { this.token = null; }

    public synchronized void setCurrentBoard(String boardId) { this.currentBoardId = boardId; }
    public synchronized Optional<String> getCurrentBoard() { return Optional.ofNullable(currentBoardId); }
    public synchronized void clearCurrentBoard() { this.currentBoardId = null; }
}
