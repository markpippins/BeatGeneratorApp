package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.angrysurfer.beats.widget.Dial;
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
        splitPane.setResizeWeight(1);
        splitPane.setLeftComponent(playerTablePanel);
        splitPane.setRightComponent(ruleTablePanel);

        // Add piano and grid panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(createControlPanel(), BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(new GridPanel(statusConsumer)), BorderLayout.CENTER);

        // Add all components
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setMinimumSize(new Dimension(getMinimumSize().width, 100));
        controlPanel.setPreferredSize(new Dimension(getPreferredSize().width, 100));

        // Add PianoPanel to the LEFT
        PianoPanel pianoPanel = new PianoPanel(statusConsumer);
        controlPanel.add(pianoPanel);

        // Add horizontal spacer
        controlPanel.add(Box.createHorizontalStrut(10)); // Adjust the width as needed

        // Performance controls
        Dial levelDial = createDial("Level", 64, 0, 127, 1);
        Dial noteDial = createDial("Note", 64, 0, 127, 1);
        controlPanel.add(createLabeledDial("Level", levelDial));
        controlPanel.add(createLabeledDial("Note", noteDial));

        // Modulation controls
        Dial swingDial = createDial("Swing", 50, 0, 100, 1);
        Dial probabilityDial = createDial("Probability", 50, 0, 100, 1);
        controlPanel.add(createLabeledDial("Swing", swingDial));
        controlPanel.add(createLabeledDial("Probability", probabilityDial));

        Dial velocityMinDial = createDial("Min Vel", 0, 0, 127, 1);
        Dial velocityMaxDial = createDial("Max Vel", 127, 0, 127, 1);
        controlPanel.add(createLabeledDial("Min Vel", velocityMinDial));
        controlPanel.add(createLabeledDial("Max Vel", velocityMaxDial));

        Dial randomDial = createDial("Random", 0, 0, 100, 1);
        controlPanel.add(createLabeledDial("Random", randomDial));

        Dial panDial = createDial("Pan", 64, 0, 127, 1);
        controlPanel.add(createLabeledDial("Pan", panDial));

        Dial sparseDial = createDial("Sparse", 0, 0, 100, 1);
        controlPanel.add(createLabeledDial("Sparse", sparseDial));

        // Octave Panel
        JPanel navPanel = createOctavePanel();
        controlPanel.add(navPanel);

        return controlPanel;
    }

    private JPanel createOctavePanel() {
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);

        // Create up and down buttons
        JButton prevButton = new JButton("↑");
        JButton nextButton = new JButton("↓");
        prevButton.setPreferredSize(new Dimension(35, 35));
        nextButton.setPreferredSize(new Dimension(35, 35));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        return navPanel;
    }

    private Dial createDial(String name, long value, int min, int max, int majorTick) {
        Dial dial = new Dial();
        dial.setMinimum(min);
        dial.setMaximum(max);
	    dial.setValue((int)value);
        dial.setPreferredSize(new Dimension(50, 50));
        dial.setMinimumSize(new Dimension(50, 50));
        dial.setMaximumSize(new Dimension(50, 50));
        return dial;
    }

    private JPanel createLabeledDial(String label, Dial dial) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JLabel l = new JLabel(label);
        l.setHorizontalAlignment(JLabel.CENTER);
        panel.add(l, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(8), BorderLayout.CENTER); // Add vertical space
        panel.add(dial, BorderLayout.SOUTH);
        panel.setMinimumSize(new Dimension(60, 80)); // Set minimum size
        panel.setMaximumSize(new Dimension(60, 80)); // Set maximum size
        return panel;
    }
}