package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyTicker;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(TickerPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private ProxyTicker activeTicker;

    public TickerPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status, this.ruleTablePanel);

        setupComponents();
    }

    private void setupComponents() {
        setLayout(new BorderLayout());

        // Create and configure split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setLeftComponent(playerTablePanel);
        splitPane.setRightComponent(ruleTablePanel);

        // Add piano and grid panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new PianoPanel(statusConsumer), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(new GridPanel(statusConsumer)), BorderLayout.CENTER);

        // Add all components
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}
