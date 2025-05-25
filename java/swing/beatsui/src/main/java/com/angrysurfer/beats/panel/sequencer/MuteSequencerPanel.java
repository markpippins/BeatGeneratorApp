package com.angrysurfer.beats.panel.sequencer;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.MuteButton;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuteSequencerPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MuteSequencerPanel.class);
    private static final int STEP_COUNT = 16;
    private static final long serialVersionUID = 1L;

    private final List<MuteButton> muteButtons = new ArrayList<>(STEP_COUNT);
    // Track mute patterns for all players
    private final Map<String, boolean[]> playerMutePatterns = new HashMap<>();
    // Reference to sequencer
    private final Object sequencer; // Either MelodicSequencer or DrumSequencer
    // Original player levels before muting
    private final Map<String, Integer> originalLevels = new HashMap<>();
    private int currentStep = 0;
    // The current player being edited
    private Player currentPlayer;
    // For drum sequencer, track the selected pad
    private int selectedPadIndex = -1;

    /**
     * Create a mute sequencer panel for melodic sequencer
     */
    public MuteSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        this.currentPlayer = sequencer.getPlayer();

        if (currentPlayer != null) {
            // Initialize mute pattern for this player
            playerMutePatterns.put(currentPlayer.getId().toString(), new boolean[STEP_COUNT]);
            originalLevels.put(currentPlayer.getId().toString(), currentPlayer.getLevel());
        }

        initialize();
    }

    /**
     * Create a mute sequencer panel for drum sequencer
     */
    public MuteSequencerPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        this.selectedPadIndex = sequencer.getSelectedPadIndex();

        // Initialize for currently selected player if any
        if (selectedPadIndex >= 0 && selectedPadIndex < sequencer.getPlayers().length) {
            Player player = sequencer.getPlayers()[selectedPadIndex];
            if (player != null) {
                currentPlayer = player;
                playerMutePatterns.put(player.getId().toString(), new boolean[STEP_COUNT]);
                originalLevels.put(player.getId().toString(), player.getLevel());
            }
        }

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(2, 2));
        UIHelper.setWidgetPanelBorder(this, "Mutes");

        // Correct the height settings
        setPreferredSize(new Dimension(800, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); // Match preferred height

        JPanel buttonPanel = new JPanel(new GridLayout(1, STEP_COUNT, 2, 0));
        buttonPanel.setBackground(getBackground());

        // Create the 16 mute buttons
        for (int i = 0; i < STEP_COUNT; i++) {
            final int index = i;

            // Create the mute button
            MuteButton button = new MuteButton();
            button.setToolTipText("Mute for step " + (i + 1));
            muteButtons.add(button);

            // Add action listener
            button.addActionListener(e -> {
                setMuteForStep(index, button.isSelected());

                // If this is the current step, apply mute now
                if (index == currentStep) {
                    applyMute();
                }
            });

            // Create container with padding
            JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            container.add(button);
            buttonPanel.add(container);
        }

        add(buttonPanel, BorderLayout.CENTER);        // Register with command bus for events
        CommandBus.getInstance().register(this, new String[]{
                Commands.TIMING_UPDATE,
                Commands.DRUM_PAD_SELECTED,  // Duplicate entry removed
                Commands.PLAYER_ACTIVATED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.TRANSPORT_STOP,
                // Add melodic sequence events
                Commands.MELODIC_SEQUENCE_LOADED,
                Commands.MELODIC_SEQUENCE_CREATED,
                Commands.MELODIC_SEQUENCE_UPDATED
        });
        TimingBus.getInstance().register(this);
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (action.getData() instanceof TimingUpdate update) {
                    handleTimingUpdate(update);
                }
            }
            case Commands.TRANSPORT_STOP -> resetHighlighting();
            case Commands.DRUM_PAD_SELECTED,
                 Commands.PLAYER_SELECTION_EVENT,
                 Commands.PLAYER_ACTIVATED,
                 Commands.MELODIC_SEQUENCE_LOADED,
                 Commands.MELODIC_SEQUENCE_CREATED,
                 Commands.MELODIC_SEQUENCE_UPDATED -> SwingUtilities.invokeLater(this::syncWithSequencer);
        }
    }

    private void handleTimingUpdate(TimingUpdate update) {
        // Check for bar updates instead of step/tick updates
        if (update.bar() != null) {
            int bar = update.bar() - 1; // Convert to 0-based index

            // Only highlight if bar changed
            if (bar != currentStep) {
                // Update highlight
                highlightStep(bar);

                // Update current bar/step reference
                currentStep = bar;
            }
        }
    }

    private void highlightStep(int step) {
        // Update all buttons
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < muteButtons.size(); i++) {
                muteButtons.get(i).setHighlighted(i == step);
                muteButtons.get(i).repaint();
            }
        });
    }

    private void resetHighlighting() {
        SwingUtilities.invokeLater(() -> {
            for (MuteButton button : muteButtons) {
                button.setHighlighted(false);
                button.repaint();
            }
        });
    }

    /**
     * Get the current mute pattern for the active player
     */
    private boolean[] getCurrentPattern() {
        if (currentPlayer == null) {
            return null;
        }

        String playerId = currentPlayer.getId().toString();

        // Create pattern if it doesn't exist
        if (!playerMutePatterns.containsKey(playerId)) {
            playerMutePatterns.put(playerId, new boolean[STEP_COUNT]);
        }

        return playerMutePatterns.get(playerId);
    }

    /**
     * Update which drum pad is selected (for drum sequencers)
     */
    private void updateSelectedPad(int padIndex) {
        if (padIndex == selectedPadIndex || !(sequencer instanceof DrumSequencer drumSequencer)) {
            return;
        }

        selectedPadIndex = padIndex;

        // Get player for the selected pad
        if (padIndex >= 0 && padIndex < drumSequencer.getPlayers().length) {
            Player player = drumSequencer.getPlayers()[padIndex];
            updatePlayer(player);
        }
    }

    /**
     * Update the player being edited
     */
    private void updatePlayer(Player player) {
        if (player == null || (currentPlayer != null && player.getId().equals(currentPlayer.getId()))) {
            return;
        }

        // Store old player's pattern
        // storeCurrentPattern();

        // Update to new player
        currentPlayer = player;

        // Track original level
        if (!originalLevels.containsKey(player.getId().toString())) {
            originalLevels.put(player.getId().toString(), player.getLevel());
        }

        // Load pattern for the new player
        loadPlayerPattern();
    }

    /**
     * Store the current pattern in the map
     */
//    private void storeCurrentPattern() {
//        if (currentPlayer == null) {
//            return;
//        }
//
//        if (sequencer instanceof MelodicSequencer melodicSequencer) {
//            // For melodic sequencer, gather mute values from the UI
//            List<Integer> muteValues = new ArrayList<>();
//
//            for (MuteButton muteButton : muteButtons) {
//                muteValues.add(muteButton.isSelected() ? 1 : 0);
//            }            // Update the sequencer
//            melodicSequencer.setMuteValues(muteValues);
//        } else if (sequencer instanceof DrumSequencer drumSequencer) {
//            int padIndex = drumSequencer.getSelectedPadIndex();
//
//            if (padIndex >= 0) {
//                try {
//                    // Get sequence data
//                    DrumSequenceData sequenceData = drumSequencer.getSequenceData();
//                    if (sequenceData == null) {
//                        logger.error("Sequence data is null in DrumSequencer");
//                        return;
//                    }
//
//                    int bar = 0;
//                    for (MuteButton muteButton : muteButtons) {
//                        sequenceData.setBarMuteValue(padIndex, bar, muteButton.isSelected());
//                        bar++;
//                    }
//
//                    // logger.debug("Storing {} mute values for drum pad: {}", muteValues.size(), padIndex);
//
//                    // Update the sequencer
//                } catch (Exception e) {
//                    logger.error("Error storing mute pattern for drum pad {}: {}", padIndex, e.getMessage(), e);
//                }
//            }
//        } else {
//            // For other sequencers, use existing map approach
//            boolean[] pattern = new boolean[STEP_COUNT];
//            for (int i = 0; i < muteButtons.size(); i++) {
//                pattern[i] = muteButtons.get(i).isSelected();
//            }
//
//            playerMutePatterns.put(currentPlayer.getId().toString(), pattern);
//        }
//    }

    /**
     * Load the pattern for the current player
     */
    private void loadPlayerPattern() {
        if (currentPlayer == null) {
            return;
        }

        if (sequencer instanceof MelodicSequencer melodicSequencer) {
            List<Integer> muteValues = melodicSequencer.getMuteValues();

            // Update UI to reflect loaded pattern
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < muteButtons.size(); i++) {
                    boolean isMuted = muteValues != null && i < muteValues.size() && muteValues.get(i) > 0;
                    muteButtons.get(i).setSelected(isMuted);
                    muteButtons.get(i).repaint();
                }
            });
        } else if (sequencer instanceof DrumSequencer drumSequencer) {
            // Get drum sequencer and selected pad
            int padIndex = drumSequencer.getSelectedPadIndex();

            if (padIndex >= 0) {
                try {
                    // Get mute values for the selected drum
                    DrumSequenceData sequenceData = drumSequencer.getSequenceData();
                    if (sequenceData == null) {
                        logger.error("Sequence data is null in DrumSequencer");
                        return;
                    }

                    // List<Boolean> muteValues = sequenceData.getBarMuteValues(padIndex);
                    // logger.debug("Loading mute pattern for drum pad: {} - found {} mute values",
                    //        padIndex, muteValues != null ? muteValues.size() : 0);

                    // Update UI to reflect loaded pattern
                    SwingUtilities.invokeLater(() -> {
                        for (int i = 0; i < muteButtons.size(); i++) {
                            //  boolean isMuted = i < muteValues.size() && muteValues.get(i);
                            boolean isMuted = sequenceData.getBarMuteValue(padIndex, i);
                            muteButtons.get(i).setSelected(isMuted);
                            muteButtons.get(i).repaint();
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error loading mute pattern for drum pad {}: {}", padIndex, e.getMessage(), e);
                }
            } else {
                // No drum selected, clear buttons
                SwingUtilities.invokeLater(() -> {
                    for (MuteButton button : muteButtons) {
                        button.setSelected(false);
                        button.repaint();
                    }
                });
            }
        } else {
            // For other sequencers, use the existing map-based approach
            String playerId = currentPlayer.getId().toString();

            // Create pattern if it doesn't exist
            if (!playerMutePatterns.containsKey(playerId)) {
                playerMutePatterns.put(playerId, new boolean[STEP_COUNT]);
            }

            boolean[] pattern = playerMutePatterns.get(playerId);

            // Update buttons
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < muteButtons.size(); i++) {
                    muteButtons.get(i).setSelected(pattern[i]);
                    muteButtons.get(i).repaint();
                }
            });
        }
    }

    /**
     * Apply the mute state for the current step
     */
    private void applyMute() {
        if (currentPlayer == null) {
            return;
        }

        boolean[] pattern = getCurrentPattern();
        if (pattern == null) {
            return;
        }

        // Get mute state for current step
        boolean shouldMute = pattern[currentStep];

        // Get original level (default to 100)
        int originalLevel = originalLevels.getOrDefault(
                currentPlayer.getId().toString(), 100);

        // Apply mute
        int newLevel = shouldMute ? 0 : originalLevel;

        // Only change if different
        if (currentPlayer.getLevel() != newLevel) {
            currentPlayer.setLevel(newLevel);

            // Notify system of change
            CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATED,
                    this,
                    currentPlayer
            );

            logger.debug("Player {} {} for step {}",
                    currentPlayer.getName(),
                    shouldMute ? "muted" : "unmuted",
                    currentStep + 1);
        }
    }

    /**
     * Set whether a step should be muted
     */
    public void setMuteForStep(int step, boolean muted) {
        if (currentPlayer == null) {
            return;
        }

        if (sequencer instanceof MelodicSequencer melodicSequencer) {
            List<Integer> muteValues = new ArrayList<>(melodicSequencer.getMuteValues());

            // Ensure list is large enough
            while (muteValues.size() <= step) {
                muteValues.add(0);
            }

            // Update the mute value at this position
            muteValues.set(step, muted ? 1 : 0);            // Save back to sequencer
            melodicSequencer.setMuteValues(muteValues);

            // We don't need to apply mute directly here, as that will happen
            // when the sequencer processes the next bar update        
        } else if (sequencer instanceof DrumSequencer drumSequencer) {
            int padIndex = drumSequencer.getSelectedPadIndex();

            if (padIndex >= 0) {
                try {
                    DrumSequenceData sequenceData = drumSequencer.getSequenceData();
                    if (sequenceData == null) {
                        logger.error("Sequence data is null in DrumSequencer");
                        return;
                    }

                    // Get current mute values
                    //List<Boolean> muteValues = new ArrayList<>();
                    // List<Boolean> existingValues = sequenceData.getBarMuteValues(padIndex);

                    // Add existing values if available
//                    if (existingValues != null) {
//                        muteValues.addAll(existingValues);
//                    }

                    // Make sure list is large enough
//                    while (muteValues.size() <= step) {
//                        muteValues.add(false);
//                    }

                    // Set the new mute value
                    // muteValues.set(step, muted);

                    // Update the sequencer
                    int bar = 0;
                    for (MuteButton button : muteButtons) {
                        sequenceData.setBarMuteValue(padIndex, bar, button.isSelected());
                        bar++;
                    }

                    logger.debug("Set mute for drum {} step {} to {}", padIndex, step, muted);
                } catch (Exception e) {
                    logger.error("Error setting mute for drum {} step {}: {}",
                            padIndex, step, e.getMessage(), e);
                }
            }
        } else {
            // For other sequencers, use the existing pattern approach
            boolean[] pattern = getCurrentPattern();
            if (pattern != null && step >= 0 && step < pattern.length) {
                pattern[step] = muted;
            }
        }
    }

    /**
     * Synchronize this panel with the current sequencer's mute values
     */
    public void syncWithSequencer() {
        if (sequencer instanceof MelodicSequencer melodicSequencer) {
            List<Integer> muteValues = melodicSequencer.getMuteValues();
            logger.debug("Syncing mute panel with melodic sequencer - found {} mute values",
                    muteValues != null ? muteValues.size() : 0);

            // Update UI to reflect loaded pattern
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < muteButtons.size(); i++) {
                    boolean isMuted = muteValues != null && i < muteValues.size() && muteValues.get(i) > 0;
                    muteButtons.get(i).setSelected(isMuted);
                    muteButtons.get(i).repaint();
                }
            });
        } else if (sequencer instanceof DrumSequencer drumSequencer) {
            // Handle drum sequencer syncing
            int padIndex = drumSequencer.getSelectedPadIndex();
            if (padIndex >= 0) {
                try {
                    DrumSequenceData sequenceData = drumSequencer.getSequenceData();
                    if (sequenceData == null) {
                        logger.error("Sequence data is null in DrumSequencer");
                        return;
                    }

                    //List<Boolean> muteValues = sequenceData.getBarMuteValues(padIndex);
                    //logger.debug("Syncing mute panel with drum sequencer - pad: {} - found {} mute values",
                    //        padIndex, muteValues != null ? muteValues.size() : 0);

                    SwingUtilities.invokeLater(() -> {
                        int bar = 0;
                        for (int i = 0; i < muteButtons.size(); i++) {
                            boolean isMuted = sequenceData.getBarMuteValue(padIndex, i);
                            //muteValues != null && i < muteValues.size() && muteValues.get(i);
                            muteButtons.get(i).setSelected(isMuted);
                            muteButtons.get(i).repaint();
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error syncing mute panel with drum sequencer: {}", e.getMessage(), e);
                }
            } else {
                logger.debug("No drum pad selected, cannot sync mute panel");
            }
        }
    }
}