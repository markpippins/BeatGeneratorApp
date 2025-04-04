package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.panel.SessionControlPanel;
import com.angrysurfer.beats.panel.SessionDisplayPanel;
import com.angrysurfer.beats.panel.TransportPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

public class ToolBar extends JToolBar {

    private final CommandBus commandBus = CommandBus.getInstance();
    private Session currentSession;

    // Main panels
    private SessionDisplayPanel displayPanel;
    private TransportPanel transportPanel;
    private SessionControlPanel controlPanel;

    private JTextField sessionNameField;

    public ToolBar() {
        super();
        setFloatable(false);
        setup();

        // Set up command bus listener
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                // Skip if this ToolBar is the sender
                if (action.getSender() == ToolBar.this) {
                    return;
                }

                if (Objects.nonNull(action.getCommand())) {
                    switch (action.getCommand()) {
                        case Commands.SESSION_SELECTED:
                        case Commands.SESSION_UPDATED:
                        case Commands.SESSION_CREATED:
                            if (action.getData() instanceof Session) {
                                Session session = (Session) action.getData();
                                displayPanel.setSession(session);
                                controlPanel.setSession(session);
                                transportPanel.updateTransportState(session);
                                sessionNameField.setText(session.getName());
                                currentSession = session;
                            }
                            break;
                    }
                }
            }
        });

        // Request the initial session state
        SwingUtilities.invokeLater(() -> {
            Session currentSession = SessionManager.getInstance().getActiveSession();
            if (currentSession != null) {
                commandBus.publish(Commands.SESSION_SELECTED, this, currentSession);
            } else {
                commandBus.publish(Commands.SESSION_REQUEST, this);
            }
        });
    }

    static final int PANEL_WIDTH = 450;

    private void setup() {
        setFloatable(false);
        setPreferredSize(new Dimension(getPreferredSize().width, 110));

        // Use a BorderLayout for the ToolBar to better control space distribution
        setLayout(new BorderLayout());

        // Create the three main panels
        displayPanel = new SessionDisplayPanel();
        // Set maximum width to 500px
        displayPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 80));
        displayPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 80));
        add(displayPanel, BorderLayout.WEST);

        // Transport controls
        transportPanel = new TransportPanel();
        // Let the transport panel take the center space
        add(transportPanel, BorderLayout.CENTER);

        JLabel sessionNameLabel = new JLabel("Session Name: ");
        sessionNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sessionNameField = new JTextField(40);

        JButton sessionNameButton = new JButton("Set Session Name");
        sessionNameButton.addActionListener(e -> {
            String newName = sessionNameField.getText().trim();
            if (currentSession != null && !newName.isEmpty()) {
                currentSession.setName(newName);
                commandBus.publish(Commands.SESSION_UPDATED, this, currentSession);
            }
        });

        JPanel sessionNamePanel = new JPanel(new BorderLayout());
        sessionNamePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        sessionNamePanel.add(sessionNameLabel, BorderLayout.WEST);
        sessionNamePanel.add(sessionNameField);
        sessionNamePanel.add(sessionNameButton, BorderLayout.EAST);
        
        add(sessionNamePanel, BorderLayout.SOUTH); // Spacer to center the transport panel
        // Session control panel
        controlPanel = new SessionControlPanel();
        // Set maximum width to PANEL_WIDTHpx
        controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 80));
        controlPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 80));
        add(controlPanel, BorderLayout.EAST);
    }
}
