package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;
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
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.Scale;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(TickerPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private Ticker activeTicker;

    private Dial levelDial;
    private Dial noteDial;
    private Dial swingDial;
    private Dial probabilityDial;
    private Dial velocityMinDial;
    private Dial velocityMaxDial;
    private Dial randomDial;
    private Dial panDial;
    private Dial sparseDial;

    private Player selectedPlayer; // Add this line
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

        // Add PianoPanel to the LEFT
        PianoPanel pianoPanel = new PianoPanel(statusConsumer);
        controlPanel.add(pianoPanel);

        // Add new vertical button panel
        // controlPanel.add(createVerticalButtonPanel());

        controlPanel
                .add(createVerticalAdjustPanel("Shift", "↑", "↓", Commands.TRANSPOSE_UP, Commands.TRANSPOSE_DOWN));

        controlPanel
                .add(createVerticalAdjustPanel("Spread", "↑", "↓", Commands.TRANSPOSE_UP, Commands.TRANSPOSE_DOWN));

        controlPanel
                .add(createScaleAdjustPanel());

        // Add horizontal spacer
        // controlPanel.add(Box.createHorizontalStrut(2)); // Adjust the width as needed

        JPanel navPanel = createOctavePanel();
        controlPanel.add(navPanel);

        noteDial = createDial("note", 60, 0, 127, 1);
        noteDial.setCommand(Commands.NEW_VALUE_NOTE);

        levelDial = createDial("level", 100, 0, 127, 1);
        levelDial.setCommand(Commands.NEW_VALUE_LEVEL);

        panDial = createDial("pan", 64, 0, 127, 1);
        panDial.setCommand(Commands.NEW_VALUE_PAN);

        velocityMinDial = createDial("minVelocity", 64, 0, 127, 1);
        velocityMinDial.setCommand(Commands.NEW_VALUE_VELOCITY_MIN);

        velocityMaxDial = createDial("maxVelocity", 127, 0, 127, 1);
        velocityMaxDial.setCommand(Commands.NEW_VALUE_VELOCITY_MAX);

        swingDial = createDial("swing", 50, 0, 100, 1);
        swingDial.setCommand(Commands.NEW_VALUE_SWING);

        probabilityDial = createDial("probability", 100, 0, 100, 1);
        probabilityDial.setCommand(Commands.NEW_VALUE_PROBABILITY);

        randomDial = createDial("random", 0, 0, 100, 1);
        randomDial.setCommand(Commands.NEW_VALUE_RANDOM);

        sparseDial = createDial("sparse", 0, 0, 100, 1);
        sparseDial.setCommand(Commands.NEW_VALUE_SPARSE);

        controlPanel.add(createLabeledControl("Note", noteDial));
        controlPanel.add(createLabeledControl("Level", levelDial));

        controlPanel.add(createLabeledControl("Pan", panDial));

        controlPanel.add(createLabeledControl("Min Vel", velocityMinDial));
        controlPanel.add(createLabeledControl("Max Vel", velocityMaxDial));

        controlPanel.add(createLabeledControl("Swing", swingDial));
        controlPanel.add(createLabeledControl("Probability", probabilityDial));

        controlPanel.add(createLabeledControl("Random", randomDial));

        controlPanel.add(createLabeledControl("Sparse", sparseDial));

        disableDials();

        return controlPanel;
    }

    // private JPanel createVerticalButtonPanel() {
    // JPanel panel = new JPanel(new GridLayout(3, 1, 2, 2));
    // panel.setPreferredSize(new Dimension(25, 80));
    // panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

    // JButton button1 = new JButton();
    // JButton button2 = new JButton();
    // JButton button3 = new JButton();

    // // Configure each button
    // for (JButton button : new JButton[] { button1, button2, button3 }) {
    // button.setPreferredSize(new Dimension(15, 15));
    // button.setMinimumSize(new Dimension(15, 15));
    // button.setMaximumSize(new Dimension(15, 15));
    // button.setFocusPainted(false);
    // button.setBorderPainted(true);
    // panel.add(button);
    // }

    // return panel;
    // }

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

    private void setDialValues(Player player) {
        if (player == null)
            return;

        levelDial.setValue(player.getLevel().intValue());
        noteDial.setValue(player.getNote().intValue());
        swingDial.setValue(player.getSwing().intValue());
        probabilityDial.setValue(player.getProbability().intValue());
        velocityMinDial.setValue(player.getMinVelocity().intValue());
        velocityMaxDial.setValue(player.getMaxVelocity().intValue());
        randomDial.setValue(player.getRandomDegree().intValue());
        panDial.setValue(player.getPanPosition().intValue());
        sparseDial.setValue((int) (player.getSparse() * 100)); // Convert from 0-1.0 to 0-100
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

    private JPanel createScaleAdjustPanel() {
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JLabel scaleLabel = new JLabel("Scale", JLabel.CENTER);

        // Create scale navigation buttons
        JButton prevButton = new JButton("↑");
        prevButton.setActionCommand(Commands.PREV_SCALE_SELECTED);
        prevButton.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.PREV_SCALE_SELECTED, this, Scale.SCALE_PATTERNS.keySet());
        });
        prevButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        prevButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        JButton nextButton = new JButton("↓");
        nextButton.setActionCommand(Commands.NEXT_SCALE_SELECTED);
        nextButton.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.NEXT_SCALE_SELECTED, this, Scale.SCALE_PATTERNS.keySet());
        });
        nextButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        nextButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        // Enable buttons by default
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

        // Add command bus listener for scale events
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (action.getSender() == TickerPanel.this) {
                    return;
                }
                switch (action.getCommand()) {
                    case Commands.FIRST_SCALE_SELECTED -> prevButton.setEnabled(false);
                    case Commands.LAST_SCALE_SELECTED -> nextButton.setEnabled(false);
                    case Commands.SCALE_SELECTED -> {
                        prevButton.setEnabled(true);
                        nextButton.setEnabled(true);
                    }
                }
            }
        });

        // Layout buttons vertically
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(scaleLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        return navPanel;
    }

    private Dial createDial(String propertyName, long value, int min, int max, int majorTick) {
        Dial dial = new Dial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue((int) value);
        dial.setPreferredSize(new Dimension(50, 50));
        dial.setMinimumSize(new Dimension(50, 50));
        dial.setMaximumSize(new Dimension(50, 50));

        // Store the property name in the dial
        dial.setName(propertyName); // Add this line

        dial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Dial sourceDial = (Dial) e.getSource();
                if (sourceDial.getCommand() != null) {
                    // Send both property name and value
                    CommandBus.getInstance().publish(sourceDial.getCommand(), this,
                            Map.of("property", sourceDial.getName(), "value", sourceDial.getValue()));
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
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof Player player) {
                            selectedPlayer = player;
                            setDialValues(player);
                            enableDials();
                            updateVerticalAdjustButtons(true);
                        }
                    }
                    case Commands.PLAYER_UNSELECTED -> {
                        selectedPlayer = null;
                        disableDials();
                        updateVerticalAdjustButtons(false);
                    }
                    case Commands.NEW_VALUE_LEVEL, Commands.NEW_VALUE_NOTE,
                            Commands.NEW_VALUE_SWING, Commands.NEW_VALUE_PROBABILITY,
                            Commands.NEW_VALUE_VELOCITY_MIN, Commands.NEW_VALUE_VELOCITY_MAX,
                            Commands.NEW_VALUE_RANDOM, Commands.NEW_VALUE_PAN,
                            Commands.NEW_VALUE_SPARSE -> {
                        if (selectedPlayer != null && action.getData() instanceof Map) {
                            updatePlayerValue(action.getCommand(), action.getData());
                        }
                    }
                }
            }
        });
    }

    private void updatePlayerValue(String command, Object data) {
        if (selectedPlayer == null || !(data instanceof Map))
            return;

        Map<String, Object> params = (Map<String, Object>) data;
        String property = (String) params.get("property");
        int value = (Integer) params.get("value");

        switch (property) {
            case "level" -> selectedPlayer.setLevel((long) value);
            case "note" -> selectedPlayer.setNote((long) value);
            case "swing" -> selectedPlayer.setSwing((long) value);
            case "probability" -> selectedPlayer.setProbability((long) value);
            case "minVelocity" -> selectedPlayer.setMinVelocity((long) value);
            case "maxVelocity" -> selectedPlayer.setMaxVelocity((long) value);
            case "random" -> selectedPlayer.setRandomDegree((long) value);
            case "pan" -> selectedPlayer.setPanPosition((long) value);
            case "sparse" -> selectedPlayer.setSparse(value / 100.0);
        }

        // Save changes and notify UI using PlayerManager instead
        PlayerManager.getInstance().savePlayerProperties(selectedPlayer);
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


}