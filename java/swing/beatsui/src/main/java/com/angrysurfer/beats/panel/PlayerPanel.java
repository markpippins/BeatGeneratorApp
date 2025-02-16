package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.angrysurfer.core.api.StatusConsumer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerPanel extends StatusProviderPanel {
    private final PlayerTablePanel playerTablePanel;
    private final RuleTablePanel ruleTablePanel;

    public PlayerPanel(StatusConsumer status) {
        super(new BorderLayout(), status);
        this.ruleTablePanel = new RuleTablePanel(status);
        this.playerTablePanel = new PlayerTablePanel(status, ruleTablePanel);
        setup();
    }

    public PlayerPanel() {
        this(null);
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createBeatsPanel(), BorderLayout.CENTER);
    }

    private JPanel createBeatsPanel() {
        JPanel beatsPanel = new JPanel(new BorderLayout());
        JPanel mainPane = new JPanel(new BorderLayout());

        // Create and configure split pane
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setResizeWeight(1.0);
        tablesSplitPane.setLeftComponent(playerTablePanel);
        tablesSplitPane.setRightComponent(ruleTablePanel);

        // Bottom section with button grid
        JPanel buttonGridPanel = new GridPanel(statusConsumer);
        JScrollPane buttonScrollPane = new JScrollPane(buttonGridPanel);

        // Add components to main split pane
        mainPane.add(tablesSplitPane, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new PianoPanel(statusConsumer), BorderLayout.NORTH);
        panel.add(buttonScrollPane, BorderLayout.SOUTH);

        mainPane.add(panel, BorderLayout.SOUTH);
        beatsPanel.add(mainPane, BorderLayout.CENTER);

        return beatsPanel;
    }
}
