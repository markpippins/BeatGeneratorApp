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
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.Scale;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(SessionPanel.class.getName());

    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;

    private Dial levelDial;
    private NoteSelectionDial noteDial;
    private Dial swingDial;
    private Dial probabilityDial;
    private Dial velocityMinDial;
    private Dial velocityMaxDial;
    private Dial randomDial;
    private Dial panDial;
    private Dial sparseDial;

    // private Player selectedPlayer; // Add this line
    private JPanel controlPanel; // Add this field

    public SessionPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status);

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

        controlPanel.add(new MiniLaunchPanel(null), FlowLayout.LEFT);

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

        var dialSize = new Dimension(90, 90);
        noteDial = new NoteSelectionDial();
        // dial.setValue((int) value);
        noteDial.setPreferredSize(dialSize);
        noteDial.setMinimumSize(dialSize);
        noteDial.setMaximumSize(dialSize);
        // createDial("note", 60, 0, 127, 1);
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

        controlPanel.add(createLabeledControl(null, noteDial));
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

        levelDial.setValue(player.getLevel().intValue(), false);
        noteDial.setValue(player.getNote().intValue(), false);
        swingDial.setValue(player.getSwing().intValue(), false);
        probabilityDial.setValue(player.getProbability().intValue(), false);
        velocityMinDial.setValue(player.getMinVelocity().intValue(), false);
        velocityMaxDial.setValue(player.getMaxVelocity().intValue(), false);
        randomDial.setValue(player.getRandomDegree().intValue(), false);
        panDial.setValue(player.getPanPosition().intValue(), false);
        sparseDial.setValue((int) (player.getSparse() * 100), false); // Convert from 0-1.0 to 0-100
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
        prevButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this,
                PlayerManager.getInstance().getActivePlayer()));
        prevButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        prevButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        JButton nextButton = new JButton(downLabel);
        nextButton.setActionCommand(downCommand);
        nextButton.addActionListener(e -> CommandBus.getInstance().publish(e.getActionCommand(), this,
                PlayerManager.getInstance().getActivePlayer()));
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
                if (action.getSender() == SessionPanel.this) {
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
                    CommandBus.getInstance().publish(sourceDial.getCommand(),
                            PlayerManager.getInstance().getActivePlayer(), sourceDial.getValue());
                }
            }
        });

        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> dial.setEnabled(true);
                    case Commands.PLAYER_UNSELECTED -> dial.setEnabled(false);
                }
            }
        });
        return dial;
    }

    private JPanel createLabeledControl(String label, Dial dial) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        if (label != null) {
            JLabel l = new JLabel(label);
            l.setHorizontalAlignment(JLabel.CENTER);
            panel.add(l, BorderLayout.NORTH);
        }
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
                if (Objects.isNull(action.getCommand()))
                    return;

                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof Player player) {
                            PlayerManager.getInstance().setActivePlayer(player);
                            setDialValues(player);
                            enableDials();
                            updateVerticalAdjustButtons(true);
                        }
                    }
                    case Commands.PLAYER_UNSELECTED -> {
                        PlayerManager.getInstance().setActivePlayer(null);
                        disableDials();
                        updateVerticalAdjustButtons(false);
                    }
                    case Commands.NEW_VALUE_LEVEL, Commands.NEW_VALUE_NOTE,
                            Commands.NEW_VALUE_SWING, Commands.NEW_VALUE_PROBABILITY,
                            Commands.NEW_VALUE_VELOCITY_MIN, Commands.NEW_VALUE_VELOCITY_MAX,
                            Commands.NEW_VALUE_RANDOM, Commands.NEW_VALUE_PAN,
                            Commands.NEW_VALUE_SPARSE ->
                        updatePlayerValue(action.getCommand(), ((Integer) action.getData()).longValue());
                }
            }
        });
    }

    private void updatePlayerValue(String command, Long value) {
        if (Objects.isNull(PlayerManager.getInstance().getActivePlayer()))
            return;

        Player selectedPlayer = PlayerManager.getInstance().getActivePlayer();

        switch (command) {
            case Commands.NEW_VALUE_LEVEL -> selectedPlayer.setLevel((long) value);
            case Commands.NEW_VALUE_NOTE -> selectedPlayer.setNote((long) value);
            case Commands.NEW_VALUE_SWING -> selectedPlayer.setSwing((long) value);
            case Commands.NEW_VALUE_PROBABILITY -> selectedPlayer.setProbability((long) value);
            case Commands.NEW_VALUE_VELOCITY_MIN -> selectedPlayer.setMinVelocity((long) value);
            case Commands.NEW_VALUE_VELOCITY_MAX -> selectedPlayer.setMaxVelocity((long) value);
            case Commands.NEW_VALUE_RANDOM -> selectedPlayer.setRandomDegree((long) value);
            case Commands.NEW_VALUE_PAN -> selectedPlayer.setPanPosition((long) value);
            case Commands.NEW_VALUE_SPARSE -> selectedPlayer.setSparse(value / 100.0);
        }

        // Save changes and notify UI using PlayerManager instead
        // PlayerManager.getInstance().savePlayerProperties(selectedPlayer);
        CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this,
                selectedPlayer);
    }

    private void updateVerticalAdjustButtons(boolean enabled) {
        // Find and update all buttons in vertical adjust panels
        for (Component comp : controlPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component inner : ((JPanel) comp).getComponents()) {
                    if (inner instanceof JButton) {
                        inner.setEnabled(enabled && Objects.nonNull(PlayerManager.getInstance().getActivePlayer()));
                    }
                }
            }
        }
    }

}