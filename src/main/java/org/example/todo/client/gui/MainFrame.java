package org.example.todo.client.gui;

import org.example.todo.client.api.ClientApi;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.TcpClient;
import org.example.todo.client.net.UdpListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final ClientApi api;
    private final ClientState state;

    private final TcpClient tcp;
    private final UdpListener udp;

    private final DashboardPanel dashboardPanel;
    private final BoardViewPanel boardViewPanel;
    private final LoginPanel loginPanel;

    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String DASHBOARD_PANEL = "DashboardPanel";
    public static final String BOARD_VIEW_PANEL = "BoardViewPanel";


    public MainFrame(ClientApi api, ClientState state, TcpClient tcp, UdpListener udp) {
        this.api = api;
        this.state = state;
        this.tcp = tcp;
        this.udp = udp;

        setTitle("Todo List Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        loginPanel = new LoginPanel(this, api);
        dashboardPanel = new DashboardPanel(this, api);
        boardViewPanel = new BoardViewPanel(this, api, state);

        NotificationHandler notificationHandler = new NotificationHandler(this, state, boardViewPanel);
        udp.addListener(notificationHandler);

        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(dashboardPanel, DASHBOARD_PANEL);
        mainPanel.add(boardViewPanel, BOARD_VIEW_PANEL);

        add(mainPanel);


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (state.getToken().isPresent()) {
                        if (state.getCurrentBoard().isPresent()) {
                            api.unsubscribeFromBoard(state.getCurrentBoard().get(), udp.getLocalPort());
                        }
                        api.logout();
                    }
                } catch (Exception ex) {
                    System.err.println("Exception during pre-shutdown cleanup: " + ex.getMessage());
                } finally {
                    try {
                        System.out.println("Closing TCP connection...");
                        tcp.close();
                    } catch (IOException ex) {
                        System.err.println("Exception while closing TCP client: " + ex.getMessage());
                    }
                    try {
                        System.out.println("Closing UDP listener...");
                        udp.close();
                    } catch (Exception ex) {
                        System.err.println("Exception while closing UDP listener: " + ex.getMessage());
                    }
                }
                super.windowClosing(e);
            }
        });

        showPanel(LOGIN_PANEL);
    }

    public void showDashboard() {
        dashboardPanel.loadBoards();
        showPanel(DASHBOARD_PANEL);
    }

    public void showBoardView(String boardId) {
        try {
            api.subscribeToBoard(boardId, udp.getLocalPort());
            boardViewPanel.loadBoard(boardId);
            showPanel(BOARD_VIEW_PANEL);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not subscribe to board: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }
}