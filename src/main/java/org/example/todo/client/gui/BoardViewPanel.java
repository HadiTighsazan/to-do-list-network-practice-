package org.example.todo.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.todo.client.api.ClientApi;
import org.example.todo.client.app.ClientState;
import org.example.todo.server.protocol.BoardMemberView;
import org.example.todo.server.protocol.TaskView;
import org.example.todo.server.protocol.ViewBoardResponse;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoardViewPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ClientApi api;
    private final ClientState state;
    private String currentBoardId;

    private final JLabel boardTitleLabel = new JLabel();
    private final DefaultListModel<TaskView> taskListModel = new DefaultListModel<>();
    private final JList<TaskView> taskList = new JList<>(taskListModel);
    private final DefaultListModel<BoardMemberView> memberListModel = new DefaultListModel<>();
    private final JList<BoardMemberView> memberList = new JList<>(memberListModel);

    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Creation Date", "Due Date", "Priority"});
    private final JComboBox<String> orderBox = new JComboBox<>(new String[]{"Ascending", "Descending"});
    private final JComboBox<String> statusFilterBox = new JComboBox<>(new String[]{"All Statuses", "Todo", "In Progress", "Done"});

    public BoardViewPanel(MainFrame mainFrame, ClientApi api, ClientState state) {
        this.mainFrame = mainFrame;
        this.api = api;
        this.state = state;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        boardTitleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        header.add(boardTitleLabel, BorderLayout.WEST);
        JButton backButton = new JButton("Back to Dashboard");
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);

        JPanel tasksPanel = new JPanel(new BorderLayout(5, 5));



        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlsPanel.add(new JLabel("Filter by Status:"));
        controlsPanel.add(statusFilterBox);
        controlsPanel.add(new JLabel("Sort by:"));
        controlsPanel.add(sortBox);
        controlsPanel.add(orderBox);
        JButton applyFiltersButton = new JButton("Apply");
        controlsPanel.add(applyFiltersButton);

        JPanel tasksHeaderPanel = new JPanel(new BorderLayout());
        tasksHeaderPanel.add(new JLabel("Tasks"), BorderLayout.WEST);
        tasksHeaderPanel.add(controlsPanel, BorderLayout.EAST);
        tasksPanel.add(tasksHeaderPanel, BorderLayout.NORTH);


        taskList.setCellRenderer(new TaskRenderer());
        tasksPanel.add(new JScrollPane(taskList), BorderLayout.CENTER);

        JPanel taskActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addTaskBtn = new JButton("Add Task");
        JButton updateStatusBtn = new JButton("Update Status");
        JButton deleteTaskBtn = new JButton("Delete Task");
        taskActions.add(addTaskBtn);
        taskActions.add(updateStatusBtn);
        taskActions.add(deleteTaskBtn);
        tasksPanel.add(taskActions, BorderLayout.SOUTH);
        splitPane.setLeftComponent(tasksPanel);

        JPanel membersPanel = new JPanel(new BorderLayout(5, 5));
        membersPanel.add(new JLabel("Members"), BorderLayout.NORTH);
        memberList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BoardMemberView member) {
                    setText(String.format("<html><b>%s</b> <font color='gray'>(%s)</font></html>", member.username, member.role));
                }
                return this;
            }
        });
        membersPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);
        JButton addMemberBtn = new JButton("Add Member");
        membersPanel.add(addMemberBtn, BorderLayout.SOUTH);
        splitPane.setRightComponent(membersPanel);

        backButton.addActionListener(e -> mainFrame.showDashboard());
        addTaskBtn.addActionListener(e -> addTask());
        addMemberBtn.addActionListener(e -> addMember());
        updateStatusBtn.addActionListener(e -> updateTaskStatus());
        deleteTaskBtn.addActionListener(e -> deleteTask());
        applyFiltersButton.addActionListener(e -> loadTasks());
    }

    public void loadBoard(String boardId) {
        this.currentBoardId = boardId;
        state.setCurrentBoard(boardId);

        new SwingWorker<ViewBoardResponse, Void>() {
            @Override
            protected ViewBoardResponse doInBackground() throws Exception {
                return api.viewBoard(boardId);
            }

            @Override
            protected void done() {
                try {
                    ViewBoardResponse response = get();
                    boardTitleLabel.setText(String.format("%s (%s)", response.board.name, response.board.id.substring(0,8)));

                    memberListModel.clear();
                    memberListModel.addAll(response.members);

                    loadTasks();

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to load board details: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    mainFrame.showDashboard();
                }
            }
        }.execute();
    }

    public void loadTasks() {
        if (currentBoardId == null) return;


        JsonObject filters = new JsonObject();
        String selectedStatus = (String) statusFilterBox.getSelectedItem();
        if (selectedStatus != null && !selectedStatus.equals("All Statuses")) {
            JsonArray statusArray = new JsonArray();
            String protocolStatus = switch (selectedStatus) {
                case "In Progress" -> "inProgress";
                case "Done" -> "done";
                default -> "todo";
            };
            statusArray.add(protocolStatus);
            filters.add("status", statusArray);
        }

        JsonObject sort = new JsonObject();
        String sortBy = (String) sortBox.getSelectedItem();
        if (sortBy != null) {
            String protocolSortBy = switch (sortBy) {
                case "Due Date" -> "due";
                case "Priority" -> "priority";
                default -> "createdAt";
            };
            sort.addProperty("by", protocolSortBy);
        }

        String sortOrder = (String) orderBox.getSelectedItem();
        if (sortOrder != null) {
            sort.addProperty("order", sortOrder.equals("Ascending") ? "asc" : "desc");
        }

        new SwingWorker<List<TaskView>, Void>() {
            @Override
            protected List<TaskView> doInBackground() throws Exception {
                return api.listTasks(currentBoardId, filters, sort).tasks;
            }
            @Override
            protected void done() {
                try {
                    taskListModel.clear();
                    taskListModel.addAll(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to load tasks: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addTask() {
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"low", "medium", "high"});
        final JComponent[] inputs = new JComponent[] {
                new JLabel("Title"), titleField,
                new JLabel("Description"), descField,
                new JLabel("Priority"), priorityBox
        };
        int result = JOptionPane.showConfirmDialog(this, inputs, "Add New Task", JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText();
            String desc = descField.getText();
            String priority = (String) priorityBox.getSelectedItem();
            if (title.isBlank()) {
                JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.addTask(currentBoardId, title, desc, priority, null);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Failed to add task: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void addMember() {
        String userId = JOptionPane.showInputDialog(this, "Enter the User ID to add:", "Add Member", JOptionPane.PLAIN_MESSAGE);
        if (userId != null && !userId.isBlank()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.addUserToBoard(currentBoardId, userId);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Failed to add member: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void updateTaskStatus() {
        TaskView selectedTask = taskList.getSelectedValue();
        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "Please select a task to update.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] statuses = {"todo", "inProgress", "done"};
        String newStatus = (String) JOptionPane.showInputDialog(this, "Select new status:", "Update Task Status",
                JOptionPane.PLAIN_MESSAGE, null, statuses, selectedTask.status);

        if (newStatus != null) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.updateTaskStatus(currentBoardId, selectedTask.id, newStatus);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Failed to update status: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void deleteTask() {
        TaskView selectedTask = taskList.getSelectedValue();
        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete task '" + selectedTask.title + "'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    api.deleteTask(currentBoardId, selectedTask.id);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); } catch (Exception e) {
                        JOptionPane.showMessageDialog(mainFrame, "Failed to delete task: " + e.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}