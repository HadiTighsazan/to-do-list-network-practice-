package org.example.todo.client.gui;

import com.google.gson.JsonObject;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.PushNotificationListener;

import javax.swing.*;

public class NotificationHandler implements PushNotificationListener {
    private final MainFrame mainFrame;
    private final ClientState state;
    private final BoardViewPanel boardViewPanel;

    public NotificationHandler(MainFrame mainFrame, ClientState state, BoardViewPanel boardViewPanel) {
        this.mainFrame = mainFrame;
        this.state = state;
        this.boardViewPanel = boardViewPanel;
    }

    @Override
    public void onPushNotification(JsonObject payload) {
        SwingUtilities.invokeLater(() -> {
            String event = payload.get("event").getAsString();
            String boardId = payload.get("boardId").getAsString();

            JOptionPane.showMessageDialog(mainFrame, "Received notification: " + event, "Push Notification", JOptionPane.INFORMATION_MESSAGE);

            state.getCurrentBoard().ifPresent(currentBoardId -> {
                if (currentBoardId.equals(boardId)) {
                    boardViewPanel.loadTasks();
                }
            });
        });
    }
}