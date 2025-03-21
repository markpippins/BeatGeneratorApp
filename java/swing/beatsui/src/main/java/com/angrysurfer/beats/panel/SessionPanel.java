package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.angrysurfer.core.api.StatusConsumer;

import lombok.Getter;
import lombok.Setter;

// Update the SessionPanel class to use the new ControlPanel
@Getter
@Setter
public class SessionPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(SessionPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private final ControlPanel controlPanel;
    private final PianoPanel pianoPanel;

    public SessionPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status);
        this.controlPanel = new ControlPanel(status);
        this.pianoPanel = new PianoPanel(status);

        setupComponents();
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

        bottomPanel.add(new JScrollPane(new GridPanel(statusConsumer)), BorderLayout.CENTER);

        // Add all components
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}