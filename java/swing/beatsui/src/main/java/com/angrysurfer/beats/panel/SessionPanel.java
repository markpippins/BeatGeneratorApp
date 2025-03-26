package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

// Update the SessionPanel class to use the new ControlPanel
@Getter
@Setter
public class SessionPanel extends StatusProviderPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private final ControlPanel controlPanel;
    private final PianoPanel pianoPanel;
    private final GridPanel gridPanel;
    private final PlayerTimelinePanel playerDetailPanel;

    private static Long lastProcessedSessionId = null;
    private long lastSessionEventTime = 0;
    private static final long EVENT_THROTTLE_MS = 100;

    public SessionPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status);
        this.controlPanel = new ControlPanel(status);
        this.pianoPanel = new PianoPanel(status);
        this.gridPanel = new GridPanel(statusConsumer);
        this.playerDetailPanel = new PlayerTimelinePanel(statusConsumer);
        setupComponents();

        CommandBus.getInstance().register(this);
    }

    private void setupComponents() {
        setLayout(new BorderLayout());

        // Create and configure split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(1);
        splitPane.setLeftComponent(playerTablePanel);
        splitPane.setRightComponent(ruleTablePanel);

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        containerPanel.add(controlPanel, BorderLayout.CENTER);
        containerPanel.add(pianoPanel, BorderLayout.EAST);

        // Add piano and grid panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(containerPanel, BorderLayout.NORTH);

        bottomPanel.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(playerDetailPanel), BorderLayout.CENTER);

        this.playerDetailPanel.setVisible(true);
        // Add all components
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null || action.getSender() == this) {
            return;
        }

        switch (action.getCommand()) {
        case Commands.PLAYER_SELECTED:
            handlePlayerSelected((Player) action.getData());
            break;
        case Commands.PLAYER_UNSELECTED:
            handlePlayerUnselected();
            break;
        // Add handlers for session changes
        case Commands.SESSION_CHANGED:
        case Commands.SESSION_SELECTED:
            if (action.getData() instanceof Session session) {
                // Add throttling check
                long now = System.currentTimeMillis();
                Long sessionId = session.getId();
                
                // Check if we've already processed this exact session recently
                if (sessionId != null && 
                    sessionId.equals(lastProcessedSessionId) && 
                    (now - lastSessionEventTime) < EVENT_THROTTLE_MS) {
                    logger.debug("SessionPanel: Ignoring duplicate session event: {}", sessionId);
                    return;
                }
                
                lastProcessedSessionId = sessionId;
                lastSessionEventTime = now;
                
                logger.info("SessionPanel received session update: {}", sessionId);
                
                // Use SwingUtilities.invokeLater to break potential call stack loops
                final Session finalSession = session;
                SwingUtilities.invokeLater(() -> {
                    updateSessionDisplay(finalSession);
                });
            }
            break;
        default:
            break;
        }
    }

    private void handlePlayerUnselected() {
        // if (Objects.nonNull(this.gridPanel))
        //     this.gridPanel.setVisible(true);

        if (Objects.nonNull(this.playerDetailPanel)) {
            // this.playerDetailPanel.setVisible(false);
            this.playerDetailPanel.setPlayer(null);
        }
    }

    private void handlePlayerSelected(Player player) {
        // if (Objects.nonNull(this.gridPanel))
        //     this.gridPanel.setVisible(false);
        if (Objects.nonNull(this.playerDetailPanel)) {
            // this.playerDetailPanel.setVisible(true);
            this.playerDetailPanel.setPlayer(player);
        }
    }

    // Update the updateSessionDisplay method
    private void updateSessionDisplay(Session session) {
        if (session == null) {
            logger.warn("Attempted to update session display with null session");
            return;
        }
        
        logger.info("Updating session display for session: {}", session.getId());
        
        // Just update this panel's own components - don't try to update other panels
        // that already listen to the same events
        
        // Force a repaint of this panel
        revalidate();
        repaint();
    }
}