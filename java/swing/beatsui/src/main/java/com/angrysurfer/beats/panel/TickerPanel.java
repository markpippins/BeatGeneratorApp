package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.proxy.ProxyTicker;
import com.angrysurfer.core.service.TickerManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(TickerPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private ProxyTicker activeTicker;

    private Dial levelDial;
    private Dial noteDial;
    private Dial swingDial;
    private Dial probabilityDial;
    private Dial velocityMinDial;
    private Dial velocityMaxDial;
    private Dial randomDial;
    private Dial panDial;
    private Dial sparseDial;

    private ProxyStrike selectedPlayer; // Add this line
    private JPanel controlPanel; // Add this field

    public TickerPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status, this.ruleTablePanel);

        setupComponents();
        setupCommandBusListener();
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
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setMinimumSize(new Dimension(getMinimumSize().width, 100));
        controlPanel.setPreferredSize(new Dimension(getPreferredSize().width, 100));

        controlPanel
                .add(createVerticalAdjustPanel("Shift", "↑", "↓", Commands.TRANSPOSE_UP, Commands.TRANSPOSE_DOWN));

        // Add PianoPanel to the LEFT
        PianoPanel pianoPanel = new PianoPanel(statusConsumer);
        controlPanel.add(pianoPanel);

        // Add horizontal spacer
        // controlPanel.add(Box.createHorizontalStrut(2)); // Adjust the width as needed

        JPanel navPanel = createOctavePanel();
        controlPanel.add(navPanel);

        noteDial = createDial("Note", 64, 0, 127, 1);
        levelDial = createDial("Level", 64, 0, 127, 1);
        controlPanel.add(createLabeledControl("Note", noteDial));
        controlPanel.add(createLabeledControl("Level", levelDial));

        panDial = createDial("Pan", 64, 0, 127, 1);
        controlPanel.add(createLabeledControl("Pan", panDial));

        velocityMinDial = createDial("Min Vel", 0, 0, 127, 1);
        velocityMaxDial = createDial("Max Vel", 127, 0, 127, 1);
        controlPanel.add(createLabeledControl("Min Vel", velocityMinDial));
        controlPanel.add(createLabeledControl("Max Vel", velocityMaxDial));

        swingDial = createDial("Swing", 50, 0, 100, 1);
        probabilityDial = createDial("Probability", 50, 0, 100, 1);
        controlPanel.add(createLabeledControl("Swing", swingDial));
        controlPanel.add(createLabeledControl("Probability", probabilityDial));

        randomDial = createDial("Random", 0, 0, 100, 1);
        controlPanel.add(createLabeledControl("Random", randomDial));

        sparseDial = createDial("Sparse", 0, 0, 100, 1);
        controlPanel.add(createLabeledControl("Sparse", sparseDial));

        disableDials();

        return controlPanel;
    }

    private void enableDials() {
        levelDial.setEnabled(true);
        noteDial.setEnabled(true);
        swingDial.setEnabled(true);
        probabilityDial.setEnabled(true);
        velocityMinDial.setEnabled(true);
        velocityMaxDial.setEnabled(true);
        randomDial.setEnabled(true);
        panDial.setEnabled(true);
        sparseDial.setEnabled(true);
    }

    private void disableDials() {
        levelDial.setEnabled(false);
        noteDial.setEnabled(false);
        swingDial.setEnabled(false);
        probabilityDial.setEnabled(false);
        velocityMinDial.setEnabled(false);
        velocityMaxDial.setEnabled(false);
        randomDial.setEnabled(false);
        panDial.setEnabled(false);
        sparseDial.setEnabled(false);
    }

    private void setDialValues(ProxyStrike player) {
        levelDial.setValue(player.getLevel().intValue());
        noteDial.setValue(player.getNote().intValue());
        swingDial.setValue(player.getSwing().intValue());
        probabilityDial.setValue(player.getProbability().intValue());
        velocityMinDial.setValue(player.getMinVelocity().intValue());
        velocityMaxDial.setValue(player.getMaxVelocity().intValue());
        randomDial.setValue(player.getRandomDegree().intValue());
        panDial.setValue(player.getPanPosition().intValue());
        sparseDial.setValue((int) (player.getSparse() * 100));
    }

    static final int BUTTON_SIZE = 30;

    private JPanel createOctavePanel() {
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);

        // Create up and down buttons
        JButton prevButton = new JButton("↑");
        JButton nextButton = new JButton("↓");
        prevButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        nextButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        return navPanel;
    }

    private JPanel createVerticalAdjustPanel(String label, String upLabel, String downLabel, String upCommand,
            String downCommand) {

        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel(label, JLabel.CENTER);

        // Create up and down buttons
        JButton prevButton = new JButton(upLabel);
        prevButton.setActionCommand(upCommand);
        prevButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this, selectedPlayer));
        prevButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        prevButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        JButton nextButton = new JButton(downLabel);
        nextButton.setActionCommand(downCommand);
        nextButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this, selectedPlayer));
        nextButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        nextButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        // Enable/disable buttons based on player selection
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

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
        dial.setValue((int) value);
        dial.setPreferredSize(new Dimension(50, 50));
        dial.setMinimumSize(new Dimension(50, 50));
        dial.setMaximumSize(new Dimension(50, 50));
        dial.setCommand(null); // Clear command
        switch (name) {
            case "Level":
                levelDial = dial;
                dial.setCommand(Commands.NEW_VALUE_LEVEL);
                break;
            case "Note":
                noteDial = dial;
                dial.setCommand(Commands.NEW_VALUE_NOTE);
                break;
            case "Swing":
                swingDial = dial;
                dial.setCommand(Commands.NEW_VALUE_SWING);
                break;
            case "Probability":
                probabilityDial = dial;
                dial.setCommand(Commands.NEW_VALUE_PROBABILITY);
                break;
            case "Min Vel":
                velocityMinDial = dial;
                dial.setCommand(Commands.NEW_VALUE_VELOCITY_MIN);
                break;
            case "Max Vel":
                velocityMaxDial = dial;
                dial.setCommand(Commands.NEW_VALUE_VELOCITY_MAX);
                break;
            case "Random":
                randomDial = dial;
                dial.setCommand(Commands.NEW_VALUE_RANDOM);
                break;
            case "Pan":
                panDial = dial;
                dial.setCommand(Commands.NEW_VALUE_PAN);
                break;
            case "Sparse":
                sparseDial = dial;
                dial.setCommand(Commands.NEW_VALUE_SPARSE);
                break;
        }

        // Add change listener to publish value changes
        dial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Dial sourceDial = (Dial) e.getSource();
                if (sourceDial.getCommand() != null) {
                    CommandBus.getInstance().publish(sourceDial.getCommand(), this, sourceDial.getValue());
                }
            }
        });

        return dial;
    }

    private JPanel createLabeledControl(String label, Dial dial) {
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

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> {
                        enableDials();
                        if (action.getData() instanceof ProxyStrike player) {
                            selectedPlayer = player; // Cache the selected player
                            setDialValues(player);
                            // Enable vertical adjust buttons
                            updateVerticalAdjustButtons(true);
                        }
                    }
                    case Commands.PLAYER_UNSELECTED -> {
                        disableDials();
                        selectedPlayer = null; // Clear the cached player
                        // Disable vertical adjust buttons
                        updateVerticalAdjustButtons(false);
                    }
                    case Commands.NEW_VALUE_LEVEL -> {
                        if (action.getData() instanceof Integer level) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerLevel(selectedPlayer, level);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_NOTE -> {
                        if (action.getData() instanceof Integer note) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerNote(selectedPlayer, note);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_SWING -> {
                        if (action.getData() instanceof Integer swing) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerSwing(selectedPlayer, swing);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_PROBABILITY -> {
                        if (action.getData() instanceof Integer probability) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerProbability(selectedPlayer, probability);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_VELOCITY_MIN -> {
                        if (action.getData() instanceof Integer velocityMin) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerVelocityMin(selectedPlayer, velocityMin);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_VELOCITY_MAX -> {
                        if (action.getData() instanceof Integer velocityMax) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerVelocityMax(selectedPlayer, velocityMax);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_RANDOM -> {
                        if (action.getData() instanceof Integer random) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerRandom(selectedPlayer, random);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_PAN -> {
                        if (action.getData() instanceof Integer pan) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerPan(selectedPlayer, pan);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                    case Commands.NEW_VALUE_SPARSE -> {
                        if (action.getData() instanceof Integer sparse) {
                            if (selectedPlayer != null) {
                                TickerManager.getInstance().updatePlayerSparse(selectedPlayer, sparse);
                                TickerManager.getInstance().savePlayerProperties(selectedPlayer);
                            }
                        }
                    }
                }
            }
        });
    }

    private void updateVerticalAdjustButtons(boolean enabled) {
        // Find and update all buttons in vertical adjust panels
        for (Component comp : controlPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component inner : ((JPanel) comp).getComponents()) {
                    if (inner instanceof JButton) {
                        inner.setEnabled(enabled && selectedPlayer != null);
                    }
                }
            }
        }
    }

    private ProxyStrike getSelectedPlayer() {
        return selectedPlayer; // Return the cached player
    }
}