package org.example.todo.client.gui;

import org.example.todo.server.protocol.TaskView;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaskRenderer extends JPanel implements ListCellRenderer<TaskView> {
    private final JLabel titleLabel = new JLabel();
    private final JLabel detailsLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public TaskRenderer() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setOpaque(true);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.add(titleLabel);
        centerPanel.add(detailsLabel);

        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TaskView> list, TaskView task, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        titleLabel.setText(task.title);
        String dueDate = (task.dueDate != null) ? dateFormat.format(new Date(task.dueDate)) : "No due date";
        detailsLabel.setText(String.format("Priority: %s | Due: %s", task.priority, dueDate));

        statusLabel.setText(task.status.toUpperCase());

        switch (task.status) {
            case "inProgress" -> statusLabel.setBackground(Color.ORANGE);
            case "done" -> statusLabel.setBackground(Color.GREEN);
            default -> statusLabel.setBackground(Color.LIGHT_GRAY);
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}