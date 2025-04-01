package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

// Update the SessionPanel class to use the new ControlPanel
@Getter
@Setter
public class SessionPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private final ControlPanel controlPanel;
    private final PianoPanel pianoPanel;
    // private final GridPanel gridPanel;
    private final PlayerTimelinePanel playerTimelinePanel;

    private static Long lastProcessedSessionId = null;
    private long lastSessionEventTime = 0;
    private static final long EVENT_THROTTLE_MS = 100;

    public SessionPanel() {
        super(new BorderLayout());

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel();
        this.playerTablePanel = new PlayersPanel();
        this.controlPanel = new ControlPanel();
        this.pianoPanel = new PianoPanel();
        // this.gridPanel = new GridPanel();
        this.playerTimelinePanel = new PlayerTimelinePanel();
        setupComponents();

        CommandBus.getInstance().register(this);
    }

    private void setupComponents() {
        setLayout(new BorderLayout());

        // Create main split pane between players/rules and the bottom section
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(1.0); // Give equal weight to both sections
        
        // Create horizontal split for player and rule tables
        JSplitPane tableSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tableSplitPane.setResizeWeight(0.7); // Give more space to player table
        tableSplitPane.setLeftComponent(playerTablePanel);
        tableSplitPane.setRightComponent(ruleTablePanel);
        
        // Add the table split pane to the top of the main split pane
        mainSplitPane.setTopComponent(tableSplitPane);
        
        // Create the bottom panel with proper constraints
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Piano and control panel at the top of bottom section
        JPanel controlContainerPanel = new JPanel(new BorderLayout());
        controlContainerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlContainerPanel.add(pianoPanel, BorderLayout.WEST);
        controlContainerPanel.add(controlPanel, BorderLayout.CENTER);
        
        // Create a split pane for the control panel and timeline
        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        bottomSplitPane.setResizeWeight(0.3); // Give more space to the timeline
        bottomSplitPane.setTopComponent(controlContainerPanel);
        
        // Create a proper scroll pane with minimum size for the timeline
        JScrollPane timelineScrollPane = new JScrollPane(playerTimelinePanel);
        timelineScrollPane.setMinimumSize(new Dimension(200, 200)); // Ensure minimum size
        timelineScrollPane.setPreferredSize(new Dimension(800, 300)); // Set a good default size
        timelineScrollPane.setBorder(BorderFactory.createTitledBorder("Player Timeline"));
        
        // Add timeline to bottom of the split pane
        bottomSplitPane.setBottomComponent(timelineScrollPane);
        
        // Add bottom split pane to the bottom panel
        bottomPanel.add(bottomSplitPane, BorderLayout.CENTER);
        
        // Add bottom panel to the bottom of the main split pane
        mainSplitPane.setBottomComponent(bottomPanel);
        
        // Add the main split pane to this panel
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Set divider locations (will be applied after components are visible)
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(0.6); // 60% for tables, 40% for timeline
            tableSplitPane.setDividerLocation(0.7); // 70% for player table
            bottomSplitPane.setDividerLocation(120); // Fixed height for control panel
        });
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
        case Commands.SESSION_UPDATED:
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

        if (Objects.nonNull(this.playerTimelinePanel)) {
            // this.playerDetailPanel.setVisible(false);
            this.playerTimelinePanel.setPlayer(null);
        }
    }

    private void handlePlayerSelected(Player player) {
        if (Objects.nonNull(this.playerTimelinePanel)) {
            // Set the player data
            this.playerTimelinePanel.setPlayer(player);
            
            // Force recalculation of the scroll pane's viewport size
            SwingUtilities.invokeLater(() -> {
                JScrollPane scrollPane = (JScrollPane) playerTimelinePanel.getParent().getParent();
                if (scrollPane instanceof JScrollPane) {
                    // Ensure proper sizing is maintained
                    scrollPane.setMinimumSize(new Dimension(200, 200));
                    scrollPane.revalidate();
                    
                    // Make sure the timeline is visible
                    playerTimelinePanel.scrollToCurrentPosition();
                }
            });
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