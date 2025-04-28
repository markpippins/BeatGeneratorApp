package com.angrysurfer.beats.panel.sequencer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;
import com.angrysurfer.beats.widget.MuteButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;

public class MuteSequencerPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MuteSequencerPanel.class);
    private static final int STEP_COUNT = 16;
    private static final long serialVersionUID = 1L;
    
    private final List<MuteButton> muteButtons = new ArrayList<>(STEP_COUNT);
    private int currentStep = 0;
    
    // Track mute patterns for all players
    private final Map<String, boolean[]> playerMutePatterns = new HashMap<>();
    
    // The current player being edited
    private Player currentPlayer;
    
    // Reference to sequencer
    private final Object sequencer; // Either MelodicSequencer or DrumSequencer
    
    // For drum sequencer, track the selected pad
    private int selectedPadIndex = -1;
    
    // Original player levels before muting
    private final Map<String, Integer> originalLevels = new HashMap<>();
    
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
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            "Mute Sequence",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            null
         ));
        
        // Set both preferred and maximum height to 40px
        setPreferredSize(new Dimension(800, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        
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
        
        add(buttonPanel, BorderLayout.CENTER);
        
        // Register with command bus for events
        CommandBus.getInstance().register(this);
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
            case Commands.DRUM_PAD_SELECTED -> {
                if (sequencer instanceof DrumSequencer && 
                    action.getData() instanceof DrumPadSelectionEvent event) {
                    updateSelectedPad(event.getNewSelection());
                }
            }
            case Commands.PLAYER_SELECTED -> {
                if (action.getData() instanceof Player player) {
                    updatePlayer(player);
                }
            }
            case Commands.TRANSPORT_STOP -> {
                resetHighlighting();
            }
        }
    }
    
    private void handleTimingUpdate(TimingUpdate update) {
        // Check if this is a step update by examining the tick value
        // Assuming 24 ticks per step (96 PPQN / 4 = 24 ticks per 16th note)
        if (update.tick() != null && update.tick() % 24 == 0) {
            // Calculate step from tick (0-15)
            int step = (int)((update.tick() / 24) % STEP_COUNT);
            
            // Only process if step changed
            if (step != currentStep) {
                // Update buttons
                highlightStep(step);
                
                // Apply mute for this step
                currentStep = step;
                applyMute();
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
        if (padIndex == selectedPadIndex || !(sequencer instanceof DrumSequencer)) {
            return;
        }
        
        selectedPadIndex = padIndex;
        
        // Get player for the selected pad
        DrumSequencer drumSequencer = (DrumSequencer) sequencer;
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
        storeCurrentPattern();
        
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
    private void storeCurrentPattern() {
        if (currentPlayer == null) {
            return;
        }
        
        boolean[] pattern = new boolean[STEP_COUNT];
        for (int i = 0; i < muteButtons.size(); i++) {
            pattern[i] = muteButtons.get(i).isSelected();
        }
        
        playerMutePatterns.put(currentPlayer.getId().toString(), pattern);
    }
    
    /**
     * Load the pattern for the current player
     */
    private void loadPlayerPattern() {
        if (currentPlayer == null) {
            return;
        }
        
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
        
        // Apply current mute state
        applyMute();
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
        
        boolean[] pattern = getCurrentPattern();
        if (pattern != null && step >= 0 && step < pattern.length) {
            pattern[step] = muted;
        }
    }
    
    /**
     * Reset all mute patterns
     */
    public void resetAllPatterns() {
        playerMutePatterns.clear();
        
        // Reset all buttons
        for (MuteButton button : muteButtons) {
            button.setSelected(false);
            button.repaint();
        }
        
        // Ensure player is unmuted
        if (currentPlayer != null && currentPlayer.getLevel() == 0) {
            int originalLevel = originalLevels.getOrDefault(
                currentPlayer.getId().toString(), 100);
            
            currentPlayer.setLevel(originalLevel);
            
            CommandBus.getInstance().publish(
                Commands.PLAYER_UPDATED,
                this,
                currentPlayer
            );
        }
    }
}