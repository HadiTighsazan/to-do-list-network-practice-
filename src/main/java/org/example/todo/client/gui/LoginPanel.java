package org.example.todo.client.gui;

import org.example.todo.client.api.ClientApi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ClientApi api;

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JButton loginButton = new JButton("Login");
    private final JButton registerButton = new JButton("Register");
    private final JLabel statusLabel = new JLabel("Please login or register.", SwingConstants.CENTER);

    public LoginPanel(MainFrame mainFrame, ClientApi api) {
        this.mainFrame = mainFrame;
        this.api = api;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel titleLabel = new JLabel("Todo List Management System");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        add(titleLabel, gbc);

        // Username
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(usernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(passwordField, gbc);

        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        add(buttonPanel, gbc);

        // Status Label
        gbc.gridy++;
        statusLabel.setForeground(Color.GRAY);
        add(statusLabel, gbc);

        // Action Listeners
        loginButton.addActionListener(this::performLogin);
        registerButton.addActionListener(this::performRegister);
    }

    private void setUIEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        if (username.isBlank() || password.length == 0) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("Logging in...");
        setUIEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                api.login(username, new String(password));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Login successful!");
                    // FIX: Call showDashboard() to also load the boards data
                    mainFrame.showDashboard();
                } catch (Exception ex) {
                    statusLabel.setText("Login failed.");
                    // Check for cause, as the primary exception is from SwingWorker
                    String errorMessage = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(mainFrame, "Login failed: " + errorMessage, "Login Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setUIEnabled(true);
                    passwordField.setText("");
                }
            }
        }.execute();
    }

    private void performRegister(ActionEvent e) {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        if (username.isBlank() || password.length < 4) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty and password must be at least 4 characters.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("Registering...");
        setUIEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                api.register(username, new String(password));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Registration successful. Please login.");
                    JOptionPane.showMessageDialog(mainFrame, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    // FIX: Clear fields after successful registration for better UX
                    usernameField.setText("");
                } catch (Exception ex) {
                    statusLabel.setText("Registration failed.");
                    // Check for cause, as the primary exception is from SwingWorker
                    String errorMessage = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(mainFrame, "Registration failed: " + errorMessage, "Registration Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setUIEnabled(true);
                    passwordField.setText("");
                }
            }
        }.execute();
    }
}