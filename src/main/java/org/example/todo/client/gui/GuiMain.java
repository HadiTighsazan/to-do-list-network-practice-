package org.example.todo.client.gui;

import org.example.todo.client.api.ClientApi;
import org.example.todo.client.app.ClientState;
import org.example.todo.client.net.UdpListener;

import javax.swing.*;

public class GuiMain {
    public static void start(ClientApi api, ClientState state, UdpListener udp) {
        // Run the GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // Set a modern look and feel
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.err.println("Failed to set Nimbus Look and Feel. Using default.");
            }

            MainFrame frame = new MainFrame(api, state, udp);
            frame.setVisible(true);
        });
    }
}