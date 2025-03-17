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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
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

    private Dial noteDial;
    private Dial levelDial;
    private NoteSelectionDial noteSelectionDial;
    private Dial swingDial;
    private Dial probabilityDial;
    private Dial velocityMinDial;
    private Dial velocityMaxDial;
    private Dial randomDial;
    private Dial panDial;
    private Dial sparseDial;

    // Add this field to track the currently selected player
    private Player activePlayer;

    private JPanel controlPanel;

    // Store octave buttons for later access
    private JButton octaveUpButton;
    private JButton octaveDownButton;

    public SessionPanel(StatusConsumer status) {
        super(new BorderLayout(), status);

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel(status);
        this.playerTablePanel = new PlayersPanel(status);

        setupComponents();
        setupCommandBusListener();
        setupSessionPanel();
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
                .add(createVerticalAdjustPanel("Preset", "↑", "↓", Commands.PRESET_UP, Commands.PRESET_DOWN));

        controlPanel
                .add(createVerticalAdjustPanel("Spread", "↑", "↓", Commands.TRANSPOSE_UP, Commands.TRANSPOSE_DOWN));

        controlPanel
                .add(createScaleAdjustPanel());

        // Add horizontal spacer
        // controlPanel.add(Box.createHorizontalStrut(2)); // Adjust the width as needed

        JPanel navPanel = createOctavePanel();
        controlPanel.add(navPanel);

        var dialSize = new Dimension(90, 90);
        noteSelectionDial = new NoteSelectionDial();
        // dial.setValue((int) value);
        noteSelectionDial.setPreferredSize(dialSize);
        noteSelectionDial.setMinimumSize(dialSize);
        noteSelectionDial.setMaximumSize(dialSize);
        // createDial("note", 60, 0, 127, 1);
        noteSelectionDial.setCommand(Commands.NEW_VALUE_NOTE);

        noteDial = createDial("note", 100, 0, 127, 1);
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

        controlPanel.add(createLabeledControl(null, noteSelectionDial));

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

        setupControlChangeListeners();

        // // Add this to your SessionPanel constructor or setupComponents method
        // JButton syncButton = new JButton("Sync Dials");
        // syncButton.addActionListener(e -> {
        // Player player = PlayerManager.getInstance().getActivePlayer();
        // if (player != null) {
        // logger.info("Manually syncing dials for player: " + player.getName());
        // updateDialsFromPlayer(player);
        // } else {
        // logger.warning("No active player to sync dials for");
        // }
        // });
        // Add to your layout
        // controlPanel.add(syncButton);

        return controlPanel;
    }

    private void enableDials() {
        levelDial.setEnabled(true);
        noteDial.setEnabled(true);
        noteSelectionDial.setEnabled(true);
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
        noteSelectionDial.setEnabled(false);
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

        noteDial.setValue(player.getNote().intValue(), false);
        levelDial.setValue(player.getLevel().intValue(), false);
        noteSelectionDial.setValue(player.getNote().intValue(), false);
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
        JButton upButton = new JButton("↑");
        JButton downButton = new JButton("↓");
        upButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        downButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        // Add action listeners
        upButton.addActionListener(e -> {
            if (activePlayer != null) {
                int currentNote = activePlayer.getNote().intValue();
                int newNote = Math.min(127, currentNote + 12); // Don't exceed 127 (max MIDI note)
                
                // Only update if it actually changed
                if (newNote != currentNote) {
                    logger.info("Octave UP: Changing note from " + currentNote + " to " + newNote);
                    
                    // Update player's note
                    activePlayer.setNote((long) newNote);
                    
                    // Update the dials (without triggering listeners)
                    noteDial.setValue(newNote, false);
                    noteSelectionDial.setValue(newNote, false);
                    
                    // Save the change and notify UI
                    PlayerManager.getInstance().updatePlayerNote(activePlayer, newNote);
                    
                    // Request row refresh in players panel
                    CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
                    
                    // Update button states
                    updateOctaveButtons(upButton, downButton, newNote);
                }
            }
        });
        
        downButton.addActionListener(e -> {
            if (activePlayer != null) {
                int currentNote = activePlayer.getNote().intValue();
                int newNote = Math.max(0, currentNote - 12); // Don't go below 0 (min MIDI note)
                
                // Only update if it actually changed
                if (newNote != currentNote) {
                    logger.info("Octave DOWN: Changing note from " + currentNote + " to " + newNote);
                    
                    // Update player's note
                    activePlayer.setNote((long) newNote);
                    
                    // Update the dials (without triggering listeners)
                    noteDial.setValue(newNote, false);
                    noteSelectionDial.setValue(newNote, false);
                    
                    // Save the change and notify UI
                    PlayerManager.getInstance().updatePlayerNote(activePlayer, newNote);
                    
                    // Request row refresh in players panel
                    CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
                    
                    // Update button states
                    updateOctaveButtons(upButton, downButton, newNote);
                }
            }
        });

        // Disable buttons by default (until a player is selected)
        upButton.setEnabled(false);
        downButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Store the buttons for later access
        this.octaveUpButton = upButton;
        this.octaveDownButton = downButton;

        return navPanel;
    }

    /**
     * Update octave button states based on the current note value
     */
    private void updateOctaveButtons(JButton upButton, JButton downButton, int note) {
        // Disable up button if we're at max octave (note >= 116)
        upButton.setEnabled(note < 116);
        
        // Disable down button if we're at min octave (note < 12)
        downButton.setEnabled(note >= 12);
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
        CommandBus.getInstance().register(new IBusListener() {
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

        CommandBus.getInstance().register(new IBusListener() {
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
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                String cmd = action.getCommand();
                logger.info("SessionPanel received command: " + cmd);

                try {
                    if (Commands.PLAYER_SELECTED.equals(cmd)) {
                        if (action.getData() instanceof Player player) {
                            logger.info("SessionPanel updating controls for player: " +
                                    player.getName() + " (ID: " + player.getId() + ")");

                            // Store reference to current player
                            activePlayer = player;

                            // Update dials on EDT to avoid concurrency issues
                            SwingUtilities.invokeLater(() -> {
                                updateDialsFromPlayer(player);
                            });
                        }
                    } else if (Commands.PLAYER_UNSELECTED.equals(cmd)) {
                        logger.info("SessionPanel received PLAYER_UNSELECTED");
                        activePlayer = null;
                        disableDials();
                        updateVerticalAdjustButtons(false);
                    }
                    // Other cases...
                } catch (Exception e) {
                    logger.severe("Error in command handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Helper methods to prevent feedback when programmatically updating controls
    private boolean listenersEnabled = true;

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

    // In setupControls() or similar method where you set up your dials
    private void setupControlChangeListeners() {

        // For note dial
        noteDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = noteDial.getValue();
            logger.info("Updating player note to: " + value);

            // Update player
            activePlayer.setNote((long) value);

            // Save the change and notify UI
            PlayerManager.getInstance().updatePlayerNote(activePlayer, value);

            // Request row refresh in players panel (important!)
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // For level dial
        levelDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = levelDial.getValue();
            logger.info("Updating player level to: " + value);

            // Update player
            activePlayer.setLevel((long) value);

            // Save the change and notify UI
            PlayerManager.getInstance().updatePlayerLevel(activePlayer, value);

            // Request row refresh in players panel (important!)
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // For swing dial
        swingDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = swingDial.getValue();
            logger.info("Updating player swing to: " + value);

            // Update player
            activePlayer.setSwing((long) value);

            // Save the change and notify UI
            PlayerManager.getInstance().updatePlayerSwing(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // For probability dial
        probabilityDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = probabilityDial.getValue();
            logger.info("Updating player probability to: " + value);

            // Update player and save
            PlayerManager.getInstance().updatePlayerProbability(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add velocityMinDial listener
        velocityMinDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = velocityMinDial.getValue();
            logger.info("Updating player min velocity to: " + value);

            // Update player and save
            activePlayer.setMinVelocity((long) value);
            PlayerManager.getInstance().updatePlayerVelocityMin(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add velocityMaxDial listener
        velocityMaxDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = velocityMaxDial.getValue();
            logger.info("Updating player max velocity to: " + value);

            // Update player and save
            activePlayer.setMaxVelocity((long) value);
            PlayerManager.getInstance().updatePlayerVelocityMax(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add panDial listener
        panDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = panDial.getValue();
            logger.info("Updating player pan to: " + value);

            // Update player and save
            activePlayer.setPanPosition((long) value);
            PlayerManager.getInstance().updatePlayerPan(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add randomDial listener
        randomDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = randomDial.getValue();
            logger.info("Updating player random to: " + value);

            // Update player and save
            activePlayer.setRandomDegree((long) value);
            PlayerManager.getInstance().updatePlayerRandom(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add sparseDial listener
        sparseDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = sparseDial.getValue();
            logger.info("Updating player sparse to: " + value);

            // Update player and save
            activePlayer.setSparse(value / 100.0); // Convert to 0-1.0 range
            PlayerManager.getInstance().updatePlayerSparse(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add note dial listener
        noteSelectionDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = noteSelectionDial.getValue();
            logger.info("Updating player note to: " + value);

            // Update player and save
            activePlayer.setNote((long) value);
            PlayerManager.getInstance().updatePlayerNote(activePlayer, value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });
    }

    private void enableControls(boolean enabled) {
        // Enable/disable all dials
        enableDials();
        // Enable/disable all buttons
        updateVerticalAdjustButtons(enabled);
    }

    private void disableControls() {
        // Disable all dials
        disableDials();
        // Disable all buttons
        updateVerticalAdjustButtons(false);
    }

    /**
     * Update all dials based on player values
     */
    private void updateDialsFromPlayer(Player player) {
        if (player == null) {
            logger.warning("Attempted to update dials with null player");
            return;
        }

        try {
            logger.info("Setting dial values for player: " + player.getName());

            // Get player values, handle potential nulls
            int level = player.getLevel() != null ? player.getLevel().intValue() : 64;
            int note = player.getNote() != null ? player.getNote().intValue() : 60;
            int swing = player.getSwing() != null ? player.getSwing().intValue() : 0;
            int minVelocity = player.getMinVelocity() != null ? player.getMinVelocity().intValue() : 64;
            int maxVelocity = player.getMaxVelocity() != null ? player.getMaxVelocity().intValue() : 127;
            int probability = player.getProbability() != null ? player.getProbability().intValue() : 100;
            int randomDegree = player.getRandomDegree() != null ? player.getRandomDegree().intValue() : 0;
            int sparse = 0; // layer.getSparse() != null ? player.getSparse().intValue() : 0;
            int panPosition = player.getPanPosition() != null ? player.getPanPosition().intValue() : 64;

            // Debug output
            logger.info("Player values - level: " + level +
                    ", note: " + note +
                    ", swing: " + swing +
                    ", velocity min/max: " + minVelocity + "/" + maxVelocity);

            // Update dials without triggering notifications (false parameter)
            levelDial.setValue(level, false);
            swingDial.setValue(swing, false);
            velocityMinDial.setValue(minVelocity, false);
            velocityMaxDial.setValue(maxVelocity, false);
            probabilityDial.setValue(probability, false);
            randomDial.setValue(randomDegree, false);
            sparseDial.setValue(sparse, false);
            panDial.setValue(panPosition, false);

            // Note dial might be custom
            if (noteSelectionDial != null) {
                noteSelectionDial.setValue(note, false);
            }
            
            // Update octave button states based on the current note
            if (octaveUpButton != null && octaveDownButton != null) {
                updateOctaveButtons(octaveUpButton, octaveDownButton, note);
            }

            // Enable dials now that they're updated
            enableDials();
            updateVerticalAdjustButtons(true);

            logger.info("Successfully updated all dials for player: " + player.getName());
        } catch (Exception e) {
            logger.severe("Error updating dials: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add this to the SessionPanel constructor or init method
    private void setupSessionPanel() {
        // Existing setup code...

        // Debug that CommandBus is registered
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() != null && action.getCommand().equals("TEST_COMMAND")) {
                    logger.info("SessionPanel received test command - CommandBus is working!");
                }
            }
        });

        // Publish a test command after a short delay
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1000);
                CommandBus.getInstance().publish("TEST_COMMAND", this, "test");
            } catch (Exception ex) {
                // Ignore
            }
        });
    }

}