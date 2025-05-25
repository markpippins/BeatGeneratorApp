package com.angrysurfer.beats;

import com.angrysurfer.beats.panel.TransportPanel;
import com.angrysurfer.beats.panel.session.SessionControlPanel;
import com.angrysurfer.beats.panel.session.SessionDisplayPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ToolBar extends JToolBar implements IBusListener {

    static final int PANEL_WIDTH = 450;
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
        CommandBus.getInstance().register(this, new String[]{
                Commands.SESSION_SELECTED,
                Commands.SESSION_UPDATED,
                Commands.SESSION_CREATED
        });

        // Request the initial session state
        SwingUtilities.invokeLater(() -> {
            Session currentSession = SessionManager.getInstance().getActiveSession();
            if (currentSession != null) {
                CommandBus.getInstance().publish(Commands.SESSION_SELECTED, this, currentSession);
            } else {
                CommandBus.getInstance().publish(Commands.SESSION_REQUEST, this);
            }
        });
    }

    @Override
    public void onAction(Command action) {
        if (Objects.nonNull(action.getCommand())) {
            switch (action.getCommand()) {
                case Commands.SESSION_SELECTED:
                case Commands.SESSION_UPDATED:
                case Commands.SESSION_CREATED:
                    if (action.getData() instanceof Session session) {
                        displayPanel.setSession(session);
                        controlPanel.setSession(session);
                        // transportPanel.updateTransportState(session);
                        // sessionNameField.setText(session.getName());
                        currentSession = session;
                    }
                    break;
            }
        }
    }

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
        // transportPanel = new TransportPanel();
        // Let the transport panel take the center space
        // add(transportPanel, BorderLayout.CENTER);
        //add(new SoundParametersPanel(), BorderLayout.CENTER);

        controlPanel = new SessionControlPanel();

        controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 80));
        controlPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 80));
        add(controlPanel, BorderLayout.EAST);
    }
}
