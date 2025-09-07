package org.example.todo.client.gui;

import org.example.todo.client.api.ClientApi;
import org.example.todo.server.protocol.BoardSummary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

public class DashboardPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ClientApi api;

    private final JList<BoardSummary> boardList = new JList<>();
    private final DefaultListModel<BoardSummary> boardListModel = new DefaultListModel<>();

    public DashboardPanel(MainFrame mainFrame, ClientApi api) {
        this.mainFrame = mainFrame;
        this.api = api;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Your Boards"), BorderLayout.WEST);
        JButton createBoardButton = new JButton("Create New Board");
        headerPanel.add(createBoardButton, BorderLayout.EAST);
        JButton refreshButton = new JButton("Refresh");
        headerPanel.add(refreshButton, BorderLayout.CENTER);


        add(headerPanel, BorderLayout.NORTH);

        // Board List
        boardList.setModel(boardListModel);
        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setCellRenderer(new BoardListRenderer());
        add(new JScrollPane(boardList), BorderLayout.CENTER);

        // Add listeners
        refreshButton.addActionListener(e -> loadBoards());
        createBoardButton.addActionListener(e -> createNewBoard());

        boardList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Double-click
                    BoardSummary selectedBoard = boardList.getSelectedValue();
                    if (selectedBoard != null) {
                        mainFrame.showBoardView(selectedBoard.id);
                    }
                }
            }
        });
    }

    public void loadBoards() {
        new SwingWorker<java.util.List<BoardSummary>, Void>() {
            @Override
            protected java.util.List<BoardSummary> doInBackground() throws Exception {
                return api.listBoards().boards;
            }

            @Override
            protected void done() {
                try {
                    boardListModel.clear();
                    boardListModel.addAll(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to load boards: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void createNewBoard() {
        String boardName = JOptionPane.showInputDialog(mainFrame, "Enter the name for the new board:", "Create Board", JOptionPane.PLAIN_MESSAGE);
        if (boardName != null && !boardName.isBlank()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.createBoard(boardName);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        loadBoards(); // Refresh the list after creating
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Failed to create board: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    // Custom renderer to display board info nicely
    private static class BoardListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof BoardSummary board) {
                setText(String.format("<html><b>%s</b> <font color='gray'>(%s) - Role: %s</font></html>", board.name, board.id.substring(0,8), board.role));
            }
            return this;
        }
    }
}