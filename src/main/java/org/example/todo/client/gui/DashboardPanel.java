package org.example.todo.client.gui;

import org.example.todo.client.api.ClientApi;
import org.example.todo.server.protocol.BoardSummary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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

        // --- START OF CORRECTIONS ---

        // 1. Header Panel Setup
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Your Boards"), BorderLayout.WEST);

        // 2. Button Panel for all actions
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        JButton createBoardButton = new JButton("Create New Board");
        JButton deleteBoardButton = new JButton("Delete Selected Board");

        buttonPanel.add(refreshButton);
        buttonPanel.add(createBoardButton);
        buttonPanel.add(deleteBoardButton);

        // 3. Add the single button panel to the header
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        // 4. Add header to the main panel
        add(headerPanel, BorderLayout.NORTH);

        // --- END OF CORRECTIONS ---

        // Board List Setup
        boardList.setModel(boardListModel);
        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setCellRenderer(new BoardListRenderer());
        add(new JScrollPane(boardList), BorderLayout.CENTER);

        // Action Listeners
        refreshButton.addActionListener(e -> loadBoards());
        createBoardButton.addActionListener(e -> createNewBoard());
        deleteBoardButton.addActionListener(e -> deleteSelectedBoard());

        // Listener to enable/disable the delete button based on list selection
        boardList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                deleteBoardButton.setEnabled(boardList.getSelectedIndex() != -1);
            }
        });
        deleteBoardButton.setEnabled(false); // Disable initially

        // Listener for double-click to view board
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
        new SwingWorker<List<BoardSummary>, Void>() {
            @Override
            protected List<BoardSummary> doInBackground() throws Exception {
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

    private void deleteSelectedBoard() {
        BoardSummary selectedBoard = boardList.getSelectedValue();
        if (selectedBoard == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please select a board to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to delete the board '" + selectedBoard.name + "'?\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.deleteBoard(selectedBoard.id);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadBoards(); // Refresh the list
                    } catch (Exception e) {
                        String errorMessage = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(mainFrame, "Failed to delete board: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
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
                setText(String.format("<html><b>%s</b> <font color='gray'>(%s) - Role: %s</font></html>", board.name, board.id.substring(0, 8), board.role));
            }
            return this;
        }
    }
}