package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;

import lombok.Getter;
import lombok.Setter;

// Update the SessionPanel class to use the new ControlPanel
@Getter
@Setter
public class SessionPanel extends StatusProviderPanel implements IBusListener {

    private static final Logger logger = Logger.getLogger(SessionPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private final ControlPanel controlPanel;
    private final PianoPanel pianoPanel;
    private final GridPanel gridPanel;
    private final PlayerTimelinePanel playerDetailPanel;

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

        this.playerDetailPanel.setVisible(false);
        // Add all components
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
        case Commands.PLAYER_SELECTED:
            handlePlayerSelected((Player) action.getData());
            break;
        case Commands.PLAYER_UNSELECTED:
            handlePlayerUnselected();
            break;
        default:
            break;
        }
    }

    private void handlePlayerUnselected() {
        if (Objects.nonNull(this.gridPanel))
            this.gridPanel.setVisible(true);

        if (Objects.nonNull(this.playerDetailPanel)) {
            this.playerDetailPanel.setVisible(false);
        }
    }

    private void handlePlayerSelected(Player player) {
        if (Objects.nonNull(this.gridPanel))
            this.gridPanel.setVisible(false);
        if (Objects.nonNull(this.playerDetailPanel)) {
            this.playerDetailPanel.setVisible(true);
            this.playerDetailPanel.setPlayer(player);
        }
    }
}